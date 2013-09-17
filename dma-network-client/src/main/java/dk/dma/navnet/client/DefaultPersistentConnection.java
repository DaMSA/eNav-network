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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.util.component.Container;

import dk.dma.commons.util.LongUtil;
import dk.dma.commons.util.concurrent.CustomConcurrentHashMap;
import dk.dma.enav.communication.ConnectionClosedException;
import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.communication.ConnectionListener;
import dk.dma.enav.communication.PersistentConnection;
import dk.dma.enav.communication.broadcast.BroadcastListener;
import dk.dma.enav.communication.broadcast.BroadcastMessage;
import dk.dma.enav.communication.broadcast.BroadcastSubscription;
import dk.dma.enav.communication.service.InvocationCallback;
import dk.dma.enav.communication.service.ServiceLocator;
import dk.dma.enav.communication.service.ServiceRegistration;
import dk.dma.enav.communication.service.spi.ServiceInitiationPoint;
import dk.dma.enav.communication.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.protocol.transport.TransportClientFactory;

/**
 * An implementation of {@link PersistentConnection} using websockets and JSON.
 * 
 * @author Kasper Nielsen
 */
class DefaultPersistentConnection extends ClientState implements PersistentConnection {

    /** A latch used for waiting on state changes from the {@link #awaitState(Container.State, long, TimeUnit)} method. */
    volatile CountDownLatch awaitStateLatch = new CountDownLatch(1);

    /** Responsible for listening and sending broadcasts. */
    final BroadcastManager broadcaster;

    final NetworkFutureSupplier cfs = new NetworkFutureSupplier();

    /** An {@link ExecutorService} for running various tasks. */
    final ExecutorService es = Executors.newCachedThreadPool();

    /** A list of connection listeners. */
    final CopyOnWriteArrayList<ConnectionListener> listeners = new CopyOnWriteArrayList<>();

    /** A lock used internally. */
    private final ReentrantLock lock = new ReentrantLock();

    /** Manages the position of the client. */
    final PositionManager positionManager;

    /** Manages registration of services. */
    final ClientServiceManager services;

    /** A {@link ScheduledExecutorService} for scheduling various tasks. */
    final ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(2);

    /** The current state of the client. Only set while holding lock, can be read at any time. */
    private volatile State state = State.INITIALIZED;

    /** Factory for creating new transports. */
    final TransportClientFactory transportFactory;

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration
     */
    DefaultPersistentConnection(MaritimeNetworkConnectionBuilder builder) {
        super(builder);
        this.positionManager = new PositionManager(this, builder.getPositionSupplier());
        this.broadcaster = new BroadcastManager(this);
        this.services = new ClientServiceManager(this);
        this.transportFactory = TransportClientFactory.createClient(builder.getHost());
        listeners.addAll(builder.listeners);
        this.connection = new ClientConnection("fff", this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitState(PersistentConnection.State state, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (state == State.INITIALIZED) {
            return true; // always at least initialized
        }
        long deadline = LongUtil.saturatedAdd(System.nanoTime(), unit.toNanos(timeout));
        CountDownLatch latch;
        while ((latch = awaitStateLatch) != null && state != getState() && getState() != State.TERMINATED) {
            if (state.isEnded() && (state == State.CONNECTED || state == State.CONNECTING)) {
                break;
            }
            // makes sure we do not have lost updates by checking awaitStateLatch==latch
            if (awaitStateLatch == latch && !latch.await(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void broadcast(BroadcastMessage message) {
        broadcaster.sendBroadcastMessage(message);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends BroadcastMessage> BroadcastSubscription broadcastListen(Class<T> messageType,
            BroadcastListener<T> consumer) {
        return broadcaster.listenFor(messageType, consumer);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        lock.lock();
        try {
            if (state.isEnded()) {
                return;
            }
            setState(State.CLOSED);
            // Close if trying to connect
            try {
                es.shutdown();
                ses.shutdown();

                for (DefaultConnectionFuture<?> f : cfs.futures) {
                    if (!f.isDone()) {
                        f.completeExceptionally(new ConnectionClosedException());
                    }
                }
                for (Runnable r : ses.getQueue()) {
                    ScheduledFuture<?> sf = (ScheduledFuture<?>) r;
                    sf.cancel(false);
                }
                ses.purge(); // remove all the tasks we just cancelled
                try {
                    ses.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                connection.closeNormally();
            } finally {
                setState(State.TERMINATED);
            }
        } finally {
            lock.unlock();
        }
    }

    ClientConnection connection() {
        return connection;
    }

    void forEachListener(Consumer<ConnectionListener> c) {
        for (ConnectionListener l : listeners) {
            c.accept(l);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getConnectionId() {
        return connection.connectionId;
    }

    /** {@inheritDoc} */
    @Override
    PositionTime getCurrentPosition() {
        return positionManager.getPositionTime();
    }

    /** {@inheritDoc} */
    @Override
    public PersistentConnection.State getState() {
        return state;
    }

    /** {@inheritDoc} */
    @Override
    public <T, E extends ServiceMessage<T>> ServiceLocator<T, E> serviceFind(ServiceInitiationPoint<E> sip) {
        return services.serviceFind(sip);
    }

    /** {@inheritDoc} */
    @Override
    public <T, S extends ServiceMessage<T>> ConnectionFuture<T> serviceInvoke(MaritimeId id, S initiatingServiceMessage) {
        return services.invokeService(id, initiatingServiceMessage);
    }

    /** {@inheritDoc} */
    @Override
    public <T, E extends ServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            InvocationCallback<E, T> callback) {
        requireNonNull(sip, "ServiceInitiationPoint is null");
        requireNonNull(callback, "callback is null");
        return services.serviceRegister(sip, callback);
    }

    void setState(State state) {
        lock.lock();
        try {
            this.state = requireNonNull(state);
            CountDownLatch prev = awaitStateLatch;
            awaitStateLatch = state == State.TERMINATED ? null : new CountDownLatch(1);
            prev.countDown();
        } finally {
            lock.unlock();
        }

        // TODO update of listeners should be synchronous in some way
    }

    void start() throws IOException {
        setState(State.CONNECTING);
        connection.connect(10, TimeUnit.SECONDS);
        lock.lock();
        try {
            if (state == State.CONNECTING) {
                setState(State.CONNECTED);
                forEachListener(new Consumer<ConnectionListener>() {
                    public void accept(ConnectionListener t) {
                        t.connected();
                    }
                });

                ses.scheduleAtFixedRate(positionManager, 0, 1, TimeUnit.SECONDS);
            } else {
                throw new IOException("Could not connect");
            }
        } finally {
            lock.unlock();
        }

        // Schedules regular position updates to the server
    }

    class NetworkFutureSupplier {
        final Set<DefaultConnectionFuture<?>> futures = Collections
                .newSetFromMap(new CustomConcurrentHashMap<DefaultConnectionFuture<?>, Boolean>(
                        CustomConcurrentHashMap.WEAK, CustomConcurrentHashMap.EQUALS, CustomConcurrentHashMap.STRONG,
                        CustomConcurrentHashMap.EQUALS, 0));

        /** {@inheritDoc} */
        public <T> DefaultConnectionFuture<T> create() {
            DefaultConnectionFuture<T> t = new DefaultConnectionFuture<>(ses);
            futures.add(t);
            return t;
        }
    }
}

// Lav get state
// Og awaitState

