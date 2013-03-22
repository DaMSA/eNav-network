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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.transport.Transport;

/**
 * Keeps track of all connections from the server and out.
 * 
 * @author Kasper Nielsen
 */
class ConnectionManager extends Supplier<Transport> {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    /** The actual server */
    final ENavNetworkServer server;

    /** All clients that currently connecting. */
    volatile Set<ServerTransport> connectingTransports = Collections
            .newSetFromMap(new ConcurrentHashMapV8<ServerTransport, Boolean>());

    /** All clients */
    final ConcurrentHashMapV8<String, ServerConnection> clients = new ConcurrentHashMapV8<>();

    final ReentrantLock lock = new ReentrantLock();

    ConnectionManager(ENavNetworkServer server) {
        this.server = requireNonNull(server);
    }

    void disconnected(ServerTransport connection) {
        clients.remove(connection.c().sid);
    }

    void broadcast(ServerTransport sender, final BroadcastMsg broadcast) {
        for (ServerConnection ch : clients.values()) {
            final ServerTransport sc = ch.t();
            if (sc != sender) {
                server.deamonPool.execute(new Runnable() {
                    public void run() {
                        sc.sendRawTextMessage(broadcast.getReceivedRawMesage());
                    }
                });
            }
        }
    }

    public Set<String> getAllConnectionIds() {
        return new HashSet<>(clients.keySet());
    }

    public int getNumberOfConnections() {
        return clients.size();
    }

    public ServerTransport getConnection(String id) {
        return clients.get(id).t();
    }

    void handleDeadConnection(ServerTransport pc) {
        // for all pending
        // send news to sender?
        // but not if they are dead
    }

    /** {@inheritDoc} */
    @Override
    public Transport get() {
        lock.lock();
        try {
            ServerTransport s = new ServerTransport(this);
            connectingTransports.add(s);
            return s;
        } finally {
            lock.unlock();
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
// class ConnectionChecker implements Runnable {
// /** {@inheritDoc} */
// @Override
// public void run() {
// clients.forEachKeyInParallel(new Action<String>() {
// @Override
// public void apply(String s) {
// clients.computeIfPresent(s, new BiFun<String, Client, Client>() {
// public Client apply(String s, Client pc) {
// // if (pc.isDead()) {
// // handleDeadConnection(pc);
// // return null;
// // }
// return pc;
// }
// });
// }
// });
// }
// }
