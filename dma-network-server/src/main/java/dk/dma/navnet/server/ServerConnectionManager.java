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
package dk.dma.navnet.server;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.CloseReason;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.transport.ConnectedMessage;
import dk.dma.navnet.core.messages.transport.HelloMessage;
import dk.dma.navnet.protocol.transport.Transport;

/**
 * Keeps track of all connections from the server and out.
 * 
 * @author Kasper Nielsen
 */
class ServerConnectionManager {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ServerConnectionManager.class);

    /** All clients that are currently trying to connect, but where the handshake has not finished. */
    private volatile Set<ServerTransport> connectingTransports = Collections
            .newSetFromMap(new ConcurrentHashMapV8<ServerTransport, Boolean>());

    /** All connections. */
    private final ConcurrentHashMapV8<String, ServerConnection> connections = new ConcurrentHashMapV8<>();

    /** The connection manager lock */
    private final ReentrantLock lock = new ReentrantLock();

    /** The network server */
    final EmbeddableCloudServer server;

    ServerConnectionManager(EmbeddableCloudServer server) {
        this.server = requireNonNull(server);
    }

    void broadcast(ServerConnection sender, final BroadcastMsg broadcast) {
        for (final ServerConnection ch : connections.values()) {
            if (ch != sender) {
                server.daemonPool.execute(new Runnable() {
                    public void run() {
                        ch.sendConnectionMessage(broadcast.cloneIt());
                    }
                });
            }
        }
    }

    void disconnected(ServerConnection c) {
        connections.remove(c.remoteId);
    }

    public Transport createNewTransport() {
        lock.lock();
        try {
            ServerTransport s = new ServerTransport(server);
            connectingTransports.add(s); // add the new created transport to set of connecting transports
            return s;
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getAllConnectionIds() {
        return new HashSet<>(connections.keySet());
    }

    public ServerConnection getConnection(String id) {
        return connections.get(id);
    }

    public int getNumberOfConnections() {
        return connections.size();
    }

    /**
     * Invoked when a hello message is received on a transport
     * 
     * @param transport
     *            the transport the message was received on
     * @param message
     *            the message
     */
    void onMessageHello(ServerTransport transport, HelloMessage message) {
        // check that nobody else has removed the connection, in which case the underlying socket has already been
        // closed and the hello message can safely be ignored
        if (connectingTransports.remove(transport)) {
            String reconnectId = message.getReconnectId();
            String id = message.getClientId().toString();
            PositionTime pt = new PositionTime(message.getLat(), message.getLon(), -1);
            for (;;) {
                // See if we already have information about a client with id
                ServerConnection c = connections.get(id);
                if (c == null) {
                    // no info about the clients id, create a new ServerConnection
                    c = new ServerConnection(server, message.getClientId(), UUID.randomUUID().toString());
                }
                if (connect(c, transport, reconnectId, pt)) {
                    return;
                }
            }
        }
    }

    boolean connect(ServerConnection c, ServerTransport transport, String reconnect, PositionTime pt) {
        c.lock.lock();
        try {
            if (c.connectionId == null) { // new connection
                c.latestPosition = pt;
                c.connectionId = UUID.randomUUID().toString();
                if (connections.putIfAbsent(c.remoteId, c) != null) {
                    return false;
                }
                c.setTransport(transport);
                transport.sendTransportMessage(new ConnectedMessage(c.connectionId));
                server.tracker.update(c, pt);
            } else if (reconnect.equals("")) { // replacing
                System.out.println("XXXX" + connections.size());
                c.latestPosition = pt;
                Transport old = c.getTransport();
                c.setTransport(transport);
                old.close(CloseReason.DUPLICATE_CONNECT);

                c.connectionId = UUID.randomUUID().toString();
                transport.sendTransportMessage(new ConnectedMessage(c.connectionId));
                server.tracker.update(c, pt);
                System.out.println("XXXX" + connections.size());
            } else { // reconnect
                // if (cm.clients.get(sid) == this) {
                // sh.close(CloseReason.DUPLICATE_CONNECT);
                // setTransport(other);
                // sh = other;
                // }
            }
            // make sure this is the current connection
        } finally {
            c.lock.unlock();
        }
        return true;
    }

    void shutdown() {
        lock.lock();
        try {
            Set<ServerTransport> s = connectingTransports;
            connectingTransports = null;
            System.out.println(s);
        } finally {
            lock.unlock();
        }
    }
}

// void handleDeadConnection(ServerTransport pc) {
// // for all pending
// // send news to sender?
// // but not if they are dead
// }
