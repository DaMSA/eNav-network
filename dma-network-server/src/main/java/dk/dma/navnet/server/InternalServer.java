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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.behaviors.Caching;

import dk.dma.enav.model.shore.ServerId;
import dk.dma.navnet.server.broadcast.BroadcastManager;
import dk.dma.navnet.server.connection.ConnectionManager;
import dk.dma.navnet.server.connection.ServerMessageBus;
import dk.dma.navnet.server.connection.WebSocketServer;
import dk.dma.navnet.server.services.ServiceManager;
import dk.dma.navnet.server.target.TargetManager;
import dk.dma.navnet.server.util.ThreadManager;

/**
 * 
 * @author Kasper Nielsen
 */
public class InternalServer {

    /** The container is is normal running mode. (certain pre-start hooks may still be running. */
    static final int S_INITIALIZED = 0;

    /** The container has been started either by a preStart() or by invoking a lazy-starting method. */
    static final int S_RUNNING = 1;

    /** The container has been shutdown, for example, by calling shutdown(). */
    static final int S_SHUTDOWN = 2;

    /** The container has been fully terminated. */
    static final int S_TERMINATED = 3;

    private final ReentrantLock lock = new ReentrantLock();

    /** PicoContainer instance. Got really tired of Guice, so replaced it with PicoContainer. */
    private final DefaultPicoContainer picoContainer = new DefaultPicoContainer(new Caching());

    private final ServerId serverId;

    /** The current state of the client. Only set while holding lock, can be read at any time. */
    private volatile int state = 0;


    private final ServerInfo info;

    /** A latch that is released when the client has been terminated. */
    private final CountDownLatch terminated = new CountDownLatch(1);

    public InternalServer(int port) {
        this(new ServerConfiguration().setServerPort(port));
    }

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration
     */
    public InternalServer(ServerConfiguration configuration) {
        serverId = requireNonNull(configuration.getId());

        picoContainer.addComponent(configuration);
        picoContainer.addComponent(this);

        picoContainer.addComponent(ServerInfo.class);
        picoContainer.addComponent(ThreadManager.class);
        picoContainer.addComponent(TargetManager.class);
        picoContainer.addComponent(ConnectionManager.class);
        picoContainer.addComponent(WebSocketServer.class);
        picoContainer.addComponent(ServerMessageBus.class);
        picoContainer.addComponent(BroadcastManager.class);
        picoContainer.addComponent(ServiceManager.class);

        info = picoContainer.getComponent(ServerInfo.class);
    }

    public ServerInfo info() {
        return info;
    }

    public <T> T getService(Class<T> service) {
        return picoContainer.getComponent(service);
    }

    public boolean awaitTerminated(long timeout, TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    /**
     * @return the serverId
     */
    public ServerId getServerId() {
        return serverId;
    }

    public void shutdown() {
        lock.lock();
        try {
            if (state == S_RUNNING) {
                state = S_SHUTDOWN;
                picoContainer.stop();
            }
            state = S_TERMINATED;
            terminated.countDown();
        } finally {
            lock.unlock();
        }
    }

    public void start() {
        lock.lock();
        try {
            if (state == S_INITIALIZED) {
                picoContainer.start();
                picoContainer.getComponent(ConnectionManager.class);
                state = S_RUNNING;
            }
        } finally {
            lock.unlock();
        }
    }
}
