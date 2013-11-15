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

import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.server.services.TargetServiceManager;

/**
 * There exist at most one target per remote client. A target does not necessarily have an active connection. But at
 * some point, some client with the specified id have connected.
 * 
 * @author Kasper Nielsen
 */
public class Target {

    /** The id of the client. */
    final MaritimeId clientId;

    volatile ServerConnection currentConnection;

    /** The remote id. */
    private final String id;

    /** The latest reported time and position. */
    private volatile PositionTime latestPosition;

    private final ReentrantLock lock = new ReentrantLock();

    /** Manages all services registered for a client. */
    final TargetServiceManager services = new TargetServiceManager(this);

    /**
     * @param key
     */
    public Target(String id) {
        this.id = requireNonNull(id);
    }

    /**
     * @return the connection
     */
    public ServerConnection getCurrentConnection() {
        return currentConnection;
    }

    public String getId() {
        return id;
    }

    /**
     * @return the latestPosition
     */
    public PositionTime getLatestPosition() {
        return latestPosition;
    }

    /**
     * @return the services
     */
    public TargetServiceManager getServices() {
        return services;
    }

    boolean helloMessageReceived(final ServerTransport transport, final HelloMessage message) {
        lock();
        try {
            final ServerConnection existing = currentConnection;
            if (existing != null) {
                existing.fullyLock();
                try {
                    ServerTransport oldTransport = existing.transport;
                    existing.target = null;
                    if (oldTransport != null) {
                        oldTransport.fullyLock();
                        try {
                            existing.setTransport(null);
                            oldTransport.connection = null;
                            oldTransport.doClose(ClosingCode.DUPLICATE_CONNECT);
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
            target.setCurrentConnection(newConnection);
            newConnection.target = target;
        } finally {
            unlock();
        }
        return true;
    }

    boolean helloMessageReceived2(final ServerTransport transport, final HelloMessage message) {
        lock();
        try {
            ServerConnection existing = currentConnection;
            if (existing != null) {
                existing.fullyLock();
                try {
                    ServerTransport oldTransport = existing.transport;
                    existing.target = null;
                    if (oldTransport != null) {
                        oldTransport.fullyLock();
                        try {
                            existing.setTransport(null);
                            oldTransport.connection = null;
                            oldTransport.doClose(ClosingCode.DUPLICATE_CONNECT);
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


            ServerConnection newConnection = ServerConnection.connect(server, this, transport, existing, message);
            connections.put(clientId, newConnection);
            target.setCurrentConnection(newConnection);
            newConnection.target = target;
        } finally {
            unlock();
        }
        return true;
    }

    /**
     * 
     * @see java.util.concurrent.locks.ReentrantLock#lock()
     */
    public void lock() {
        lock.lock();
    }

    /**
     * @param createReply
     */
    public void sendConnectionMessage(ConnectionMessage message) throws NoEndpointException {}


    /**
     * @param connection
     *            the connection to set
     */
    public void setCurrentConnection(ServerConnection connection) {
        this.currentConnection = connection;
    }

    /**
     * @param latestPosition
     *            the latestPosition to set
     */
    public void setLatestPosition(PositionTime latestPosition) {
        this.latestPosition = latestPosition;
    }

    /**
     * 
     * @see java.util.concurrent.locks.ReentrantLock#unlock()
     */
    public void unlock() {
        lock.unlock();
    }
}
