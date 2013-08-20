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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.BiFun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.CloseReason;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
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

    public static void main(String[] args) {
        ConcurrentHashMapV8<String, String> cm = new ConcurrentHashMapV8<>();
        cm.compute("A", new BiFun<String, String, String>() {

            @Override
            public String apply(String paramA, String paramB) {
                return "fffd";
            }
        });
        cm.compute("A", new BiFun<String, String, String>() {

            @Override
            public String apply(String paramA, String paramB) {
                return "fff";
            }
        });

        System.out.println(cm.size());
    }

    public Transport createNewTransport() {
        // // The only reason we need to lock here is to avoid competing with shutdown.
        // Set<ServerTransport> connectingTransports = this.connectingTransports;
        // if (connectingTransports != null) {
        // ServerTransport s = new ServerTransport(server);
        // connectingTransports.add(s);
        // if (this.connectingTransports != null) {
        // return s;
        // }
        // }
        // return null;
        //
        // ; // add the new created transport to set of connecting transports
        // return s;

        lock.lock();
        try {
            ServerTransport s = new ServerTransport(server);
            connectingTransports.add(s); // add the new created transport to set of connecting transports
            return s;
        } finally {
            lock.unlock();
        }
    }

    void disconnected(ServerConnection c) {
        connections.remove(c.remoteId, c);
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
     * Invoked when a hello message is received on a newly created transport.
     * {@link ServerTransport#onTransportMessage(dk.dma.navnet.core.messages.TransportMessage)} has already made sure
     * that the message is the first message received on the transport.
     * 
     * @param transport
     *            the transport the message was received on
     * @param message
     *            the hello message
     */
    void onMessageHello(final ServerTransport transport, final HelloMessage message) {
        // check that nobody else has removed the connection, in which case the underlying socket has already been
        // closed and the hello message can safely be ignored
        if (connectingTransports.remove(transport)) {
            String clientId = message.getClientId().toString();

            final AtomicReference<ServerConnection> existing = new AtomicReference<>();
            connections.compute(clientId, new BiFun<String, ServerConnection, ServerConnection>() {
                public ServerConnection apply(String id, ServerConnection connection) {
                    existing.set(connection);
                    return ServerConnection.connect(server, transport, connection, message);
                }
            });
            if (existing.get() != null) {
                Transport old = existing.get().getTransport();
                old.close(CloseReason.DUPLICATE_CONNECT);
                server.tracker.remove(existing.get());
            }
        }
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

