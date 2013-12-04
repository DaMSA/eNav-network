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
package dk.dma.navnet.server.targets;

import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.server.connection.ServerConnection;
import dk.dma.navnet.server.services.TargetServiceManager;

/**
 * 
 * @author Kasper Nielsen
 */
public class Target {

    private volatile ServerConnection connection;

    final MaritimeId id;

    /** The latest reported time and position. */
    private volatile PositionTime latestPosition;

    final ReentrantLock retrieveLock = new ReentrantLock();

    final ReentrantLock sendLock = new ReentrantLock();

    final TargetServiceManager serviceManager;

    final TargetManager tm;

    public Target(TargetManager tm, MaritimeId id) {
        this.id = id;
        this.tm = tm;
        serviceManager = new TargetServiceManager(this);
    }

    public void fullyLock() {
        retrieveLock.lock();
        sendLock.lock();
    }

    public void fullyUnlock() {
        sendLock.unlock();
        retrieveLock.unlock();
    }

    /**
     * @return the connegction
     */
    public ServerConnection getConnection() {
        return connection;
    }

    public void sendIfConnected(ConnectionMessage cm) {
        ServerConnection c = getConnection();
        if (c != null) {
            c.messageSend(cm);
        }
    }

    /**
     * @return the id
     */
    public MaritimeId getId() {
        return id;
    }

    /**
     * @return the latestPosition
     */
    public PositionTime getLatestPosition() {
        return latestPosition;
    }

    /**
     * @param connegction
     *            the connegction to set
     */
    public void setConnection(ServerConnection connegction) {
        this.connection = connegction;
    }

    /**
     * @param latestPosition
     *            the latestPosition to set
     */
    public void setLatestPosition(PositionTime latestPosition) {
        this.latestPosition = latestPosition;
        tm.reportPosition(this, latestPosition);
    }

    /**
     * @return
     */
    public TargetServiceManager getServices() {
        return serviceManager;
    }

    /**
     * @return
     */
    public boolean isConnected() {
        return connection != null;
    }
}
