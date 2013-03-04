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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.net.NetworkFuture;
import dk.dma.enav.net.ServiceCallback;
import dk.dma.enav.net.ServiceRegistration;
import dk.dma.enav.net.broadcast.BroadcastListener;
import dk.dma.enav.net.broadcast.BroadcastMessage;
import dk.dma.enav.net.broadcast.BroadcastSubscription;
import dk.dma.enav.service.spi.InitiatingMessage;
import dk.dma.enav.service.spi.MaritimeService;
import dk.dma.enav.service.spi.MaritimeServiceMessage;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * An implementation of {@link MaritimeNetworkConnection} using websockets and JSON.
 * 
 * @author Kasper Nielsen
 */
public class ClientNetwork implements MaritimeNetworkConnection {

    /** Responsible for listening and sending broadcasts. */
    final ClientBroadcastManager broadcaster;

    /** The id of this client */
    final MaritimeId clientId;

    /** The single connection to a server. */
    final ClientConnection connection;

    /** An {@link ExecutorService} for running various tasks. */
    final ExecutorService es = Executors.newCachedThreadPool();

    /** A lock used internally. */
    private final ReentrantLock lock = new ReentrantLock();

    /** Manages the position of the client. */
    final PositionManager positionManager;

    /** Manages registration of services. */
    final ClientServiceManager services;

    /** A {@link ScheduledExecutorService} for scheduling various tasks. */
    final ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);

    /** The current state of the client. Only set while holding lock, can be read at any time. */
    private volatile State state = State.CREATED;

    /** Used to await for termination. */
    private final CountDownLatch terminated = new CountDownLatch(1);

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration
     */
    ClientNetwork(MaritimeNetworkConnectionBuilder builder) {
        this.clientId = requireNonNull(builder.getId());
        this.positionManager = new PositionManager(this, builder.getPositionSupplier());
        this.broadcaster = new ClientBroadcastManager(this);
        this.services = new ClientServiceManager(this);
        this.connection = new ClientConnection("ws://" + builder.getHost(), this);
    }

    /* DELEGATING METHODS */

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
    public NetworkFutureImpl<Map<MaritimeId, PositionTime>> findAll(Area shape) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFuture<Map<MaritimeId, String>> findServices(String serviceType) {
        return services.findServices(serviceType);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends MaritimeServiceMessage<?>, S extends MaritimeService, E extends MaritimeServiceMessage<T> & InitiatingMessage> ServiceRegistration registerService(
            S service, ServiceCallback<E, T> b) {
        return services.registerService(service, b);
    }

    /** {@inheritDoc} */
    @Override
    public <T, S extends MaritimeServiceMessage<T> & InitiatingMessage> NetworkFutureImpl<T> invokeService(
            MaritimeId id, S msg) {
        return services.invokeService(id, msg);
    }

    /* LIFECYCLE METHODS */

    /** {@inheritDoc} */
    @Override
    public boolean isClosed() {
        return state == State.TERMINATED || state == State.CLOSED;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTerminated() {
        return state == State.TERMINATED;
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitTerminated(long timeout, TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        lock.lock();
        try {
            if (isClosed()) {
                return;
            }
            state = State.CLOSED;
            es.shutdown();
            ses.shutdown();

            try {
                ses.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // skal lige have fundet ud af med det shutdown
            terminated.countDown();
        } finally {
            lock.unlock();
        }
    }

    public static MaritimeNetworkConnection create(MaritimeNetworkConnectionBuilder builder) throws IOException {
        ClientNetwork n = new ClientNetwork(builder);
        try {
            // Okay we might be offline when we start up the client
            // perhaps we should just treat it as a reconnect.
            // The downside is that people could wait a lot of time
            // when entering the wrong url.
            n.connection.connect(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IOException(e);// could not connect within 10 seconds
        }
        // Schedules regular position updates to the server
        n.ses.scheduleAtFixedRate(n.positionManager, 0, 1, TimeUnit.SECONDS);
        return n;
    }

    enum State {
        CLOSED, CONNECTED, CREATED, TERMINATED;
    }
}
