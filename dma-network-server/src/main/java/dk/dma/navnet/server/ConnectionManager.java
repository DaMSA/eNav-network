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

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.Action;
import jsr166e.ConcurrentHashMapV8.BiFun;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;

/**
 * Keeps track of all connections from the server and out.
 * 
 * @author Kasper Nielsen
 */
class ConnectionManager {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    /** The pool of threads used for each connection. */
    final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    /** The actual server */
    final ENavNetworkServer server;

    final ConcurrentHashMapV8<String, Client> connections = new ConcurrentHashMapV8<>();

    ConnectionManager(ENavNetworkServer server, SocketAddress socketAddress) {
        this.server = requireNonNull(server);

        // Creates the web socket handler that accept incoming requests
        WebSocketHandler wsHandler = new WebSocketHandler() {
            public void configure(WebSocketServletFactory factory) {
                factory.setCreator(new WebSocketCreator() {
                    public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
                        return new ServerHandler(ConnectionManager.this).getListener();
                    }
                });
            }
        };

        server.server.setHandler(wsHandler);
    }

    synchronized ServerHandler addConnection(MaritimeId mid, String id, ServerHandler c) {
        Client newCH = new Client(mid, server, c);
        c.holder = newCH;
        Client ch = connections.put(id, newCH);
        return ch == null ? null : ch.currentConnection;
    }

    void disconnected(ServerHandler connection) {
        connections.remove(connection.holder.id.toString());
    }

    void broadcast(ServerHandler sender, final BroadcastMsg broadcast) {
        for (Client ch : connections.values()) {
            final ServerHandler sc = ch.currentConnection;
            if (sc != sender) {
                server.deamonPool.execute(new Runnable() {
                    public void run() {
                        sc.sendRawTextMessage(broadcast.getReceivedRawMesage());
                    }
                });
            }
        }
    }

    synchronized void dropConnection() {
        // connections.remove(pc.getRemoteID().toString());
    }

    public Set<String> getAllConnectionIds() {
        return new HashSet<>(connections.keySet());
    }

    public int getNumberOfConnections() {
        return connections.size();
    }

    public ServerHandler getConnection(String id) {
        return connections.get(id).currentConnection;
    }

    /** Stops accepting any more sockets. */
    void stopAccepting() {
        ses.shutdown();
    }

    void handleDeadConnection(ServerHandler pc) {
        // for all pending
        // send news to sender?
        // but not if they are dead
    }

    class ConnectionChecker implements Runnable {
        /** {@inheritDoc} */
        @Override
        public void run() {
            connections.forEachKeyInParallel(new Action<String>() {
                @Override
                public void apply(String s) {
                    connections.computeIfPresent(s, new BiFun<String, Client, Client>() {
                        public Client apply(String s, Client pc) {
                            // if (pc.isDead()) {
                            // handleDeadConnection(pc);
                            // return null;
                            // }
                            return pc;
                        }
                    });
                }
            });
        }
    }
}
