/*
 * Copyright (c) 2008 Kasper Nielsen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.navnet.server;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import jsr166e.ConcurrentHashMapV8;
import dk.dma.enav.communication.CloseReason;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.Circle;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.PositionReportMessage;
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.spi.AbstractConnection;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConnection extends AbstractConnection {

    final ConnectionManager cm;

    /** The latest position of the client. */
    volatile PositionTime latestPosition;

    final MaritimeId id;
    final String sid;
    final ClientServices services;

    volatile String uuid;

    ServerTransport t() {
        return (ServerTransport) super.transport;
    }

    /**
     * @param cm
     * @param sh
     */
    public ServerConnection(ConnectionManager cm, MaritimeId id) {
        this.cm = cm;
        this.sid = id.toString();
        this.id = requireNonNull(id);
        services = new ClientServices(this);
    }

    boolean connect(ServerTransport other, String reconnect, PositionTime pt) {
        lock.lock();
        try {
            if (uuid == null) { // new connection
                latestPosition = pt;
                uuid = UUID.randomUUID().toString();
                if (cm.clients.putIfAbsent(sid, this) != null) {
                    return false;
                }
                setTransport(other);
                sendMessage(new ConnectedMessage(uuid));
                cm.server.tracker.update(this, pt);
            } else if (reconnect.equals("")) { // replacing
                System.out.println("XXXX" + cm.clients.size());
                latestPosition = pt;
                ServerTransport old = t();
                setTransport(other);
                old.close(CloseReason.DUPLICATE_CONNECT);

                uuid = UUID.randomUUID().toString();
                sendMessage(new ConnectedMessage(uuid));
                cm.server.tracker.update(this, pt);
                System.out.println("XXXX" + cm.clients.size());
            } else { // reconnect
                // if (cm.clients.get(sid) == this) {
                // sh.close(CloseReason.DUPLICATE_CONNECT);
                // setTransport(other);
                // sh = other;
                // }
            }
            // make sure this is the current connection
        } finally {
            lock.unlock();
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void handleMessageReply(AbstractTextMessage m, NetworkFutureImpl<?> f) {
        unknownMessage(m);
    }

    /** {@inheritDoc} */
    @Override
    public final void handleMessage(AbstractTextMessage m) {
        if (m instanceof RegisterService) {
            registerService((RegisterService) m);
        } else if (m instanceof FindService) {
            findService((FindService) m);
        } else if (m instanceof AbstractRelayedMessage) {
            relay((AbstractRelayedMessage) m);
        } else if (m instanceof BroadcastMsg) {
            broadcast((BroadcastMsg) m);
        } else if (m instanceof PositionReportMessage) {
            positionReport((PositionReportMessage) m);
        } else if (m instanceof FindService) {
            findService((FindService) m);
        } else {
            unknownMessage(m);
        }
    }

    /** {@inheritDoc} */
    public void broadcast(BroadcastMsg m) {
        cm.broadcast(t(), m);
    }

    /** {@inheritDoc} */
    public void findService(final FindService m) {
        final PositionTime pos = latestPosition;
        double meters = m.getMeters() <= 0 ? Integer.MAX_VALUE : m.getMeters();
        Area a = new Circle(pos, meters, CoordinateSystem.GEODETIC);
        // Find all services with the area
        final ConcurrentHashMapV8<ServerConnection, PositionTime> map = new ConcurrentHashMapV8<>();
        cm.server.tracker.forEachWithinArea(a, new BiConsumer<ServerConnection, PositionTime>() {
            public void accept(ServerConnection l, PositionTime r) {
                if (l.services.hasService(m.getServiceName())) {
                    map.put(l, r);
                }
            }
        });
        map.remove(this);
        // Sort by distance
        List<Entry<ServerConnection, PositionTime>> l = new ArrayList<>(map.entrySet());
        Collections.sort(l, new Comparator<Entry<ServerConnection, PositionTime>>() {
            public int compare(Entry<ServerConnection, PositionTime> o1, Entry<ServerConnection, PositionTime> o2) {
                return Double.compare(o1.getValue().distanceTo(pos, CoordinateSystem.GEODETIC), o2.getValue()
                        .distanceTo(pos, CoordinateSystem.GEODETIC));
            }
        });

        // If we have a maximum number of results, filter the list
        if (l.size() > m.getMax()) {
            l = l.subList(0, m.getMax());
        }

        // Extract the maritime id
        List<String> list = new ArrayList<>();
        for (Entry<ServerConnection, PositionTime> e : l) {
            list.add(e.getKey().id.toString());
        }
        t().sendMessage(m.createReply(list.toArray(new String[list.size()])));
    }

    /** {@inheritDoc} */
    public void positionReport(PositionReportMessage m) {
        cm.server.tracker.update(this, m.getPositionTime());
        latestPosition = m.getPositionTime();
    }

    /** {@inheritDoc} */
    public void registerService(RegisterService m) {
        services.registerService(m);
        t().sendMessage(m.createReply());
    }

    /** {@inheritDoc} */
    public void relay(AbstractRelayedMessage m) {
        String d = m.getDestination();
        ServerTransport c = cm.getConnection(d);
        if (c == null) {
            System.err.println("Unknown destination " + d);
            System.err.println("Available " + cm.getAllConnectionIds());
        } else {
            c.sendRawTextMessage(m.getReceivedRawMesage());
        }
    }
}
