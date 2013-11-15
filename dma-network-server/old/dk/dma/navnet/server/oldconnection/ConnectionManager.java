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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.Fun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;
import dk.dma.navnet.server.InternalServer;
import dk.dma.navnet.server.util.ThreadManager;

/**
 * Keeps track of all connections from the server and out.
 * 
 * @author Kasper Nielsen
 */
class ConnectionManager {

    static ConnectionManager CONNECTION_MANAGER;

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    /** All clients that are currently trying to connect, but where the handshake has not finished. */
    volatile Set<ConnectionFuture> connectingTransports = Collections
            .newSetFromMap(new ConcurrentHashMapV8<ConnectionFuture, Boolean>());

    /** All current active connections. */
    private final ConcurrentHashMapV8<String, ServerConnection> actuve = connections = new ConcurrentHashMapV8<>();

    /** The connection manager lock */
    private final ReentrantLock lock = new ReentrantLock();

    final InternalServer server;


    /** All targets. */
    private final ConcurrentHashMapV8<String, Target> targets = new ConcurrentHashMapV8<>();

    final ThreadManager tm;

    public ConnectionManager(InternalServer server, ThreadManager tm) {
        CONNECTION_MANAGER = this;
        this.server = requireNonNull(server);
        this.tm = requireNonNull(tm);
    }

    void broadcast(ServerConnection sender, final BroadcastSend broadcast) {
        for (final ServerConnection ch : connections.values()) {
            if (ch != sender) {
                tm.daemonPool.execute(new Runnable() {

                    public void run() {
                        ch.sendConnectionMessage(broadcast.cloneIt());
                    }
                });
            }
        }
    }

    void disconnected(ServerConnection c) {
        connections.remove(c.clientId.toString(), c);
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
     * @return
     */
    void onConnectFinish(final ServerTransport transport, final HelloMessage message) {
        // check that nobody else has removed the connection, in which case the underlying socket has already been
        // closed and the hello message can safely be ignored
        if (connectingTransports.remove(transport)) {
            String clientId = message.getClientId().toString();
            // There is a very rare case, where we remove a target because it is stale.
            // but at the same time a connection comes in trying to use the stale target.
            // so we just try again if a target has been removed

            Target target = targets.computeIfAbsent(clientId, new Fun<String, Target>() {

                public Target apply(String key) {
                    return new Target(key);
                }
            });
            if (target.helloMessageReceived(transport, message)) {
                return;
            }
        }
    }

    void shutdown() {
        lock.lock();
        try {
            Set<ConnectionFuture> s = connectingTransports;
            connectingTransports = null;
            System.out.println(s);
        } finally {
            lock.unlock();
        }
    }
}
