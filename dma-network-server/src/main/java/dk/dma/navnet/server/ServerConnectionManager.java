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
import java.util.concurrent.locks.ReentrantLock;

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.Fun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastMsg;
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

    /** All targets. */
    private final ConcurrentHashMapV8<String, Target> targets = new ConcurrentHashMapV8<>();

    /** All current active connections. */
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

    void disconnected(ServerConnection c) {
        connections.remove(c.clientId.toString(), c);
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
     * {@link ServerTransport#onTransportMessage(dk.dma.navnet.messages.TransportMessage)} has already made sure that
     * the message is the first message received on the transport.
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

            final Target target = targets.computeIfAbsent(clientId, new Fun<String, Target>() {
                public Target apply(String key) {
                    return new Target(key);
                }
            });

            target.lock();
            try {
                final ServerConnection existing = (ServerConnection) target.getConnection();
                if (existing != null) {
                    existing.fullyLock();
                    try {
                        Transport oldTransport = existing.getTransport();
                        existing.setApplication(null);
                        if (oldTransport != null) {
                            oldTransport.fullyLock();
                            try {
                                existing.setTransport(null);
                                oldTransport.setConnection(null);
                                oldTransport.close(ClosingCode.DUPLICATE_CONNECT);
                            } finally {
                                oldTransport.fullyUnlock();
                            }
                        } else {
                            existing.setTransport(null);
                        }
                    } finally {
                        if (existing != null) {
                            existing.fullyUnlock();
                        }
                    }
                }
                ServerConnection newConnection = ServerConnection.connect(server, target, transport, existing, message);
                connections.put(clientId, newConnection);
                target.setConnection(newConnection);
                newConnection.setApplication(target);
            } finally {
                target.unlock();
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

