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

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.Caching;

import dk.dma.commons.util.LongUtil;
import dk.dma.enav.communication.ConnectionClosedException;
import dk.dma.enav.communication.ConnectionListener;
import dk.dma.enav.communication.MaritimeNetworkConnection;
import dk.dma.enav.communication.MaritimeNetworkConnection.State;
import dk.dma.enav.communication.MaritimeNetworkConnectionBuilder;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.client.broadcast.BroadcastManager;
import dk.dma.navnet.client.connection.ClientConnection;
import dk.dma.navnet.client.connection.ConnectionManager;
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
    static final int S_RUNNING = 3;

    /** The container has been shutdown, for example, by calling shutdown(). */
    static final int S_SHUTDOWN = 4;

    /** The container has been started either by a preStart() or by invoking a lazy-starting method. */
    static final int S_STARTING = 2;

    /** The container has been fully terminated. */
    static final int S_TERMINATED = 5;

    /** A latch used for waiting on state changes from the {@link #awaitState(Container.State, long, TimeUnit)} method. */
    volatile CountDownLatch awaitStateLatch = new CountDownLatch(1);

    /** A list of connection listeners. */
    final CopyOnWriteArrayList<ConnectionListener> listeners = new CopyOnWriteArrayList<>();

    /** PicoContainer */
    final DefaultPicoContainer picoContainer;

    /** The current state of the client. Only set while holding lock, can be read at any time. */
    private volatile State state = State.DISCONNECTED;

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration
     */
    public InternalClient(MaritimeNetworkConnectionBuilder builder) {
        // Got really tired of Guice, so replaced it with PicoContainer
        picoContainer = new DefaultPicoContainer(new Caching());
        picoContainer.addComponent(builder);
        picoContainer.addComponent(this);
        picoContainer.addComponent(ClientConnection.class);
        picoContainer.addComponent(ClientInfo.class);
        picoContainer.addComponent(PositionManager.class);
        picoContainer.addComponent(BroadcastManager.class);
        picoContainer.addComponent(ClientServiceManager.class);
        picoContainer.addComponent(ThreadManager.class);
        picoContainer.addComponent(ConnectionManager.class);

        listeners.addAll(builder.getListeners());
    }

    /** {@inheritDoc} */
    public boolean awaitState(MaritimeNetworkConnection.State state, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (state == State.DISCONNECTED) {
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

    void close() {
        lock();
        try {
            if (state == State.CLOSED || state == State.TERMINATED) {
                return;
            }
            setStateWhileHolderLock(state, State.CLOSED);

            picoContainer.getComponent(ConnectionManager.class).disconnect();
            // Do it in another thread
            // Close if trying to connect
            try {
                picoContainer.stop();
                picoContainer.getComponent(ClientConnection.class).closeNormally();
            } finally {
                setStateWhileHolderLock(state, State.TERMINATED);
            }
        } finally {
            unlock();
        }
    }

    void connect() {
        lock();
        try {
            State state = this.state;
            if (state == State.CONNECTED || state == State.CONNECTING) {
                return; // we are either already connected, or already trying to
            } else if (state == State.CLOSED || state == State.TERMINATED) {
                throw new ConnectionClosedException("The connection has already been closed via connection.close()");
            }

            setStateWhileHolderLock(state, state = State.CONNECTING);
            try {
                picoContainer.getComponent(ClientConnection.class).connect(10, TimeUnit.SECONDS);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (state == State.CONNECTING) {
                setStateWhileHolderLock(state, State.CONNECTED);
                forEachListener(new Consumer<ConnectionListener>() {
                    public void accept(ConnectionListener t) {
                        t.connected();
                    }
                });

                picoContainer.start();
            } else {
                throw new RuntimeException("Could not connect " + state);
            }
        } finally {
            unlock();
        }
    }

    public void finalize() {
        close();
    }

    void forEachListener(Consumer<ConnectionListener> c) {
        for (ConnectionListener l : listeners) {
            c.accept(l);
        }
    }

    MaritimeNetworkConnection.State getState() {
        return state;
    }

    void setStateWhileHolderLock(State currentState, State newState) {
        this.state = newState;
        if (currentState != newState) {
            CountDownLatch prev = awaitStateLatch;
            awaitStateLatch = state == State.TERMINATED ? null : new CountDownLatch(1);
            prev.countDown();
        }
        // TODO update of listeners should be synchronous in some way
    }

    static PicoContainer create(MaritimeNetworkConnectionBuilder builder) {
        return new InternalClient(builder).picoContainer;
    }
}
