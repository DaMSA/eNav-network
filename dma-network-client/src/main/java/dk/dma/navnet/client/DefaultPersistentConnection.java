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
package dk.dma.navnet.client;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
import dk.dma.navnet.core.transport.ClientTransportFactory;
import dk.dma.navnet.core.transport.websocket.WebsocketTransports;

/**
 * An implementation of {@link PersistentConnection} using websockets and JSON.
 * 
 * @author Kasper Nielsen
 */
class DefaultPersistentConnection implements PersistentConnection {

    /** Responsible for listening and sending broadcasts. */
    final BroadcastManager broadcaster;

    /** The id of this client */
    private final MaritimeId clientId;

    /** Used to await for termination. */
    private final CountDownLatch closed = new CountDownLatch(1);

    /** The single connection to a server. */
    private final ClientConnection connection;

    /** An {@link ExecutorService} for running various tasks. */
    final ExecutorService es = Executors.newCachedThreadPool();

    /** A list of connection listeners. */
    final CopyOnWriteArrayList<ConnectionListener> listeners = new CopyOnWriteArrayList<>();

    /** A lock used internally. */
    private final ReentrantLock lock = new ReentrantLock();

    /** Manages the position of the client. */
    final PositionManager positionManager;

    /** Manages registration of services. */
    final ServiceManager services;

    /** A {@link ScheduledExecutorService} for scheduling various tasks. */
    final ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);

    /** The current state of the client. Only set while holding lock, can be read at any time. */
    private volatile State state = State.INITIALIZED;

    /** Used to await for termination. */
    private final CountDownLatch terminated = new CountDownLatch(1);

    /** Factory for creating new transports. */
    final ClientTransportFactory transportFactory;

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration
     */
    DefaultPersistentConnection(MaritimeNetworkConnectionBuilder builder) {
        this.clientId = requireNonNull(builder.getId());
        this.positionManager = new PositionManager(this, builder.getPositionSupplier());
        this.broadcaster = new BroadcastManager(this);
        this.services = new ServiceManager(this);
        this.transportFactory = WebsocketTransports.createClient(builder.getHost());
        this.connection = new ClientConnection(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitState(PersistentConnection.State state, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (state == State.INITIALIZED) {
            return true; // always at least initialized
        } else if (state == State.CLOSED) {
            return closed.await(timeout, unit);
        } else if (state == State.TERMINATED) {
            return terminated.await(timeout, unit);
        } else {
            // only want to await connected/connecting if not shutdown
            while (closed.getCount() > 0) {
                throw new UnsupportedOperationException();
            }
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void broadcast(BroadcastMessage message) {
        broadcaster.send(message);
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
            if (closed.getCount() == 0) {
                return;
            }
            closed.countDown();
            state = State.CLOSED;
            es.shutdown();
            ses.shutdown();
            try {
                ses.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connection.closeNormally();
        } finally {
            terminated.countDown();
            lock.unlock();
        }
    }

    ClientConnection connection() {
        return connection;
    }

    /** {@inheritDoc} */
    @Override
    public MaritimeId getLocalId() {
        return clientId;
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
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public <T, E extends ServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            InvocationCallback<E, T> callback) {
        return services.serviceRegister(sip, callback);
    }

    public void start() throws IOException {
        connection.connect(10, TimeUnit.SECONDS);
        // Schedules regular position updates to the server
        ses.scheduleAtFixedRate(positionManager, 0, 1, TimeUnit.SECONDS);
    }
}
