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
package dk.dma.navnet.client;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.Caching;

import dk.dma.enav.communication.MaritimeNetworkClientConfiguration;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.client.broadcast.BroadcastManager;
import dk.dma.navnet.client.connection.ConnectionManager;
import dk.dma.navnet.client.connection.ConnectionMessageBus;
import dk.dma.navnet.client.service.ClientServiceManager;
import dk.dma.navnet.client.service.PositionManager;
import dk.dma.navnet.client.util.ThreadManager;

/**
 * The internal client.
 * 
 * @author Kasper Nielsen
 */
@SuppressWarnings("serial")
public class InternalClient extends ReentrantLock {

    /** The container is is normal running mode. (certain pre-start hooks may still be running. */
    static final int S_RUNNING = 0;

    /** The container has been started either by a preStart() or by invoking a lazy-starting method. */
    static final int S_STARTING = 1;

    /** The container has been shutdown, for example, by calling shutdown(). */
    static final int S_SHUTDOWN = 2;

    /** The container has been fully terminated. */
    static final int S_TERMINATED = 3;

    /** The id of this client */
    private final MaritimeId clientId;

    /** PicoContainer instance. Got really tired of Guice, so replaced it with PicoContainer. */
    private final DefaultPicoContainer picoContainer = new DefaultPicoContainer(new Caching());

    /** Supplies the current position. */
    private final Supplier<PositionTime> positionSupplier;

    /** The current state of the client. Only set while holding lock, can be read at any time. */
    private volatile int state = 0;

    /** A latch that is released when the client has been terminated. */
    private final CountDownLatch terminated = new CountDownLatch(1);

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration
     */
    private InternalClient(MaritimeNetworkClientConfiguration builder) {
        picoContainer.addComponent(builder);
        picoContainer.addComponent(this);
        picoContainer.addComponent(PositionManager.class);
        picoContainer.addComponent(BroadcastManager.class);
        picoContainer.addComponent(ClientServiceManager.class);
        picoContainer.addComponent(ConnectionMessageBus.class);
        picoContainer.addComponent(ThreadManager.class);
        picoContainer.addComponent(ConnectionManager.class);

        clientId = requireNonNull(builder.getId());
        positionSupplier = requireNonNull(builder.getPositionSupplier());
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    public void close() {
        lock();
        try {
            if (state < S_SHUTDOWN) {
                state = S_SHUTDOWN;
                Runnable r = new Runnable() {

                    public void run() {
                        close0();
                    }
                };
                new Thread(r).start();
            }
        } finally {
            unlock();
        }
    }

    void close0() {
        lock();
        try {
            if (state == S_SHUTDOWN) {
                try {
                    picoContainer.stop();
                } finally {
                    state = S_TERMINATED;
                    terminated.countDown();
                }
            }
        } finally {
            unlock();
        }
    }

    protected void finalize() {
        close();
    }

    /**
     * Returns the maritime id of the client.
     * 
     * @return the maritime id of the client
     */
    public MaritimeId getLocalId() {
        return clientId;
    }

    public boolean isClosed() {
        return state >= S_SHUTDOWN;
    }

    public boolean isTerminated() {
        return state == S_TERMINATED;
    }

    /**
     * Reads and returns the current position.
     * 
     * @return the current position
     */
    public PositionTime readCurrentPosition() {
        return positionSupplier.get();
    }

    static PicoContainer create(MaritimeNetworkClientConfiguration builder) {
        InternalClient client = new InternalClient(builder);
        client.picoContainer.start();
        return client.picoContainer;
    }
}
