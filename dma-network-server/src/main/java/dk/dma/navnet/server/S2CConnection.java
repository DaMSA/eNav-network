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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import jsr166e.ConcurrentHashMapV8;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.Circle;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
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
public class S2CConnection extends AbstractConnection {

    final ConnectionManager cm;

    final ServerTransport sh;

    /**
     * @param cm
     * @param sh
     */
    public S2CConnection(ConnectionManager cm, ServerTransport sh) {
        super(cm.ses);
        super.setTransport(sh);
        this.cm = cm;
        this.sh = sh;
    }

    /** {@inheritDoc} */
    @Override
    public final void handleMessageReply(AbstractTextMessage m, NetworkFutureImpl<?> f) {
        unknownMessage(m);
    }

    /** {@inheritDoc} */
    @Override
    public final void handleMessage(AbstractTextMessage m) {
        if (m instanceof HelloMessage) {
            hello((HelloMessage) m);
        } else if (m instanceof RegisterService) {
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
        cm.broadcast(sh, m);
    }

    /** {@inheritDoc} */
    public void findService(final FindService m) {
        final PositionTime pos = sh.holder.latestPosition;
        double meters = m.getMeters() <= 0 ? Integer.MAX_VALUE : m.getMeters();
        Area a = new Circle(pos, meters, CoordinateSystem.GEODETIC);
        // Find all services with the area
        final ConcurrentHashMapV8<Client, PositionTime> map = new ConcurrentHashMapV8<>();
        cm.server.tracker.forEachWithinArea(a, new BiConsumer<Client, PositionTime>() {
            public void accept(Client l, PositionTime r) {
                if (l.services.hasService(m.getServiceName())) {
                    map.put(l, r);
                }
            }
        });
        Client cli = sh.holder;
        if (cli != null) {
            map.remove(cli);
        }
        // Sort by distance
        List<Entry<Client, PositionTime>> l = new ArrayList<>(map.entrySet());
        Collections.sort(l, new Comparator<Entry<Client, PositionTime>>() {
            public int compare(Entry<Client, PositionTime> o1, Entry<Client, PositionTime> o2) {
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
        for (Entry<Client, PositionTime> e : l) {
            list.add(e.getKey().id.toString());
        }
        sh.sendMessage(m.createReply(list.toArray(new String[list.size()])));
    }

    /** {@inheritDoc} */
    public void hello(HelloMessage m) {
        UUID uuid = UUID.randomUUID();
        Client c = cm.addConnection(m.getClientId(), m.getClientId().toString(), sh);
        PositionTime pt = new PositionTime(m.getLat(), m.getLon(), -1);
        c.latestPosition = pt;
        sh.sendMessage(new ConnectedMessage(uuid.toString()));
        cm.server.tracker.update(sh.holder, pt);
    }

    /** {@inheritDoc} */
    public void positionReport(PositionReportMessage m) {
        cm.server.tracker.update(sh.holder, m.getPositionTime());
        sh.holder.latestPosition = m.getPositionTime();
    }

    /** {@inheritDoc} */
    public void registerService(RegisterService m) {
        sh.holder.services.registerService(m);
        sh.sendMessage(m.createReply());
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
