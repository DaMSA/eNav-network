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
package dk.dma.enav.network.server;

import static java.util.Objects.requireNonNull;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;

import dk.dma.enav.network.PersistentConnection;

/**
 * Keeps track of all connections from the server and out.
 * 
 * @author Kasper Nielsen
 */
class ConnectionManager extends Thread {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    /** The pool of threads used for each connection. */
    final ExecutorService es = Executors.newCachedThreadPool();

    /** The pool of threads used for each connection. */
    final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    /** The actual server */
    final ENavNetworkServer server;

    /** The socket address we listen to. */
    private final SocketAddress socketAddress;

    /** The bounded server socket. Or null if the socket has not yet been bounded */
    private volatile ServerSocket ss;

    /** All the current connections. */
    private final ConcurrentHashMapV8<String, PersistentConnection> connections = new ConcurrentHashMapV8<>();

    /** Connections for a particular UUID (used for reconnects). */
    private final ConcurrentHashMapV8<UUID, PersistentConnection> connectionForUUID = new ConcurrentHashMapV8<>();

    ConnectionManager(ENavNetworkServer server, SocketAddress socketAddress) {
        super("Accept-Connection-Thread-" + socketAddress);
        this.socketAddress = requireNonNull(socketAddress);
        this.server = requireNonNull(server);
    }

    synchronized void connected(PersistentConnection c) {
        connections.put(c.getRemoteID().toString(), c);

        // Hmm man kan jo ikke bruge persistent connection til noget
        // Hvis den doer er det jo klients ansvar

        // Jo for serveren skal soerge for at matce det op
    }

    void disconnected(PersistentConnection connection) {

    }

    synchronized void dropConnection(PersistentConnection pc) {
        connectionForUUID.remove(pc.getConnectionId());
        connections.remove(pc.getRemoteID().toString());
    }

    public Set<String> getAllConnectionIds() {
        return new HashSet<>(connections.keySet());
    }

    public int getNumberOfConnections() {
        return connections.size();
    }

    public PersistentConnection getConnection(String id) {
        return connections.get(id);
    }

    public void run() {
        ses.scheduleAtFixedRate(new ConnectionChecker(), 0, 1, TimeUnit.SECONDS);
        try (final ServerSocket socket = ss = new ServerSocket()) {
            socket.bind(socketAddress);
            LOG.info("Listening to  " + socketAddress);
            for (;;) {
                final Socket s = socket.accept();
                LOG.debug("Accepted socket from " + s.getRemoteSocketAddress());
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        Server2ClientConnection.connect(ConnectionManager.this, s);
                    }
                });
            }
        } catch (Throwable t) {
            // Check if we have been forcefully been shutdown by the user
            if (server.state.isShutdown()) {
                LOG.info("Stopped accepting new sockets.");
                return;
            }
            // Looks like the accept thread failed for some reason.
            // Lets shutdown
            LOG.error("Shutting down, because acceptor thread failed", t);
            server.shutdown();
        }
    }

    /** Stops accepting any more sockets. */
    void stopAccepting() {
        ses.shutdown();
        ServerSocket ss = this.ss;
        if (ss != null) {
            Closeables.closeQuietly(ss);
        }
    }

    class ConnectionChecker implements Runnable {
        /** {@inheritDoc} */
        @SuppressWarnings("synthetic-access")
        @Override
        public void run() {
            connections.forEachValueInParallel(new Action<PersistentConnection>() {
                @Override
                public void apply(PersistentConnection pc) {
                    // remove stale connections
                }
            });
        }
    }
}
