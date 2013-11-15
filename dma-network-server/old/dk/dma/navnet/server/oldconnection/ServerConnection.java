/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.navnet.server.oldconnection;

import static java.util.Objects.requireNonNull;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.common.util.ResumingQueue;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.PositionReportMessage;
import dk.dma.navnet.messages.c2c.ClientRelayedMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;
import dk.dma.navnet.messages.s2c.service.FindService;
import dk.dma.navnet.messages.s2c.service.RegisterService;
import dk.dma.navnet.server.Server;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConnection {

    /** The unique id of the connection. */
    private final String connectionId;

    /** The latest position of the client. */
    volatile PositionTime latestPosition;

    /** A read lock. */
    private final ReentrantLock receiveLock = new ReentrantLock();

    ResumingQueue rq = new ResumingQueue();

    /** The server we are connected to. */
    final Server server;

    final Target target;

    volatile ServerTransport transport;

    /** A write lock. */
    private final ReentrantLock sendLock = new ReentrantLock();

    /**
     * @param cm
     * @param sh
     */
    public ServerConnection(Target target, Server server, String connectionId) {
        this.connectionId = requireNonNull(connectionId);
        this.server = requireNonNull(server);
        this.target = target;
    }

    public final void closeNormally() {
        transport.doClose(ClosingCode.NORMAL);
    }

    public final void fullyLock() {
        receiveLock.lock();
        sendLock.lock();
    }


    public final void fullyUnlock() {
        sendLock.unlock();
        receiveLock.unlock();
    }


    public final String getConnectionId() {
        return connectionId;
    }

    /** {@inheritDoc} */
    public final void onConnectionMessage(ServerTransport t, ConnectionMessage m) {
        receiveLock.lock();
        try {
            // make sure it is the current transport
            if (t != transport) {
                t.doClose(ClosingCode.WRONG_MESSAGE);
            }
            rq.messageIn(m);


        } finally {
            receiveLock.unlock();
        }
        rq.ackUpToIncluding(message.getLatestReceivedId());
        if (m instanceof RegisterService) {
            registerService((RegisterService) m);
        } else if (m instanceof FindService) {
            // List<String> list = new ArrayList<>();
            // for (Entry<Target, PositionTime> e : l) {
            // list.add(e.getKey().getId());
            // }
            // target.sendConnectionMessage(m.createReply(list.toArray(new String[list.size()])));
            services.findService((FindService) m);
        } else if (m instanceof ClientRelayedMessage) {
            relay((ClientRelayedMessage) m);
        } else if (m instanceof BroadcastSend) {
            server.connectionManager.broadcast(this, (BroadcastSend) m);
        } else if (m instanceof PositionReportMessage) {
            positionReport((PositionReportMessage) m);
        } else {
            // unknownMessage(m);
        }
    }

    /** {@inheritDoc} */
    public void positionReport(PositionReportMessage m) {
        Target target = this.target;
        if (target != null) {
            server.tracker.update(target, m.getPositionTime());
        }
        latestPosition = m.getPositionTime();
    }

    /** {@inheritDoc} */
    public void registerService(RegisterService m) {
        services.registerService(m);
        sendConnectionMessage(m.createReply());
    }

    /** {@inheritDoc} */
    public void relay(ClientRelayedMessage m) {
        String d = m.getDestination();
        ServerConnection c = server.connectionManager.getConnection(d);
        if (c == null) {
            System.err.println("Unknown destination " + d);
            // System.err.println("Available " + server.connectionManager.getAllConnectionIds());
        } else {
            c.sendConnectionMessage(m.cloneIt());
            // c.sendRawTextMessage(m.getReceivedRawMesage());
        }
    }

    public final void sendConnectionMessage(ConnectionMessage m) {
        sendLock.lock();
        try {

        } finally {
            sendLock.unlock();
        }
        rq.write(new Consumer<String>() {

            public void accept(String t) {
                transport.doSendTextAsync(t);
            }
        }, m);
    }

    static ServerConnection connect(Server server, Target target, ServerTransport connectingTransport,
            ServerConnection existing, HelloMessage message) {
        String reconnectId = message.getReconnectId();

        final PositionTime pt = new PositionTime(message.getLat(), message.getLon(), -1);
        if (existing == null || reconnectId.equals("")) {
            ServerConnection c = new ServerConnection(server, message.getClientId(), UUID.randomUUID().toString());
            c.latestPosition = pt;
            c.transport = connectingTransport;
            connectingTransport.doSendTransportMessage(new ConnectedMessage(c.getConnectionId()));
            server.tracker.update(target, pt);
            return c;
        } else {
            // Trying to reconnect
        }
        throw new Error();
    }

    /** The current state of the transport. */
    public enum State {
        CLOSED, CONNECTED, CONNECTING, INITIALIZED;
    }

    /**
     * @param serverTransport
     * @param reason
     */
    public void transportDisconnected(ServerTransport serverTransport, ClosingCode reason) {}
}
