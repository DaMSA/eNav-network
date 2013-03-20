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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import dk.dma.commons.tracker.PositionTracker;
import dk.dma.enav.model.shore.ServerId;
import dk.dma.navnet.core.transport.ServerTransportFactory;
import dk.dma.navnet.core.transport.websocket.WebsocketTransports;

/**
 * 
 * @author Kasper Nielsen
 */
public class ENavNetworkServer {

    /** The default port this server is running on. */
    public static final int DEFAULT_PORT = 43234;

    final ServerId id = new ServerId(1);

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ENavNetworkServer.class);

    final ExecutorService deamonPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("deamonPool").setDaemon(true).build());

    final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("PositionTrackerUpdate").setDaemon(true).build());

    /** The thread that is accepting incoming sockets. */
    final ConnectionManager connections;

    /** The current state of this server. */
    volatile State state = State.INITIALIZED;

    /** A latch used for waiting on complete shutdown. */
    final CountDownLatch termination = new CountDownLatch(1);

    /** The position tracker. */
    final PositionTracker<ServerConnection> tracker = new PositionTracker<>();

    final ServerTransportFactory factory;

    public ENavNetworkServer() {
        this(DEFAULT_PORT);
    }

    public ENavNetworkServer(int port) {
        factory = WebsocketTransports.createServer(port);
        connections = new ConnectionManager(this);
    }

    public boolean awaitTerminated(long timeout, TimeUnit unit) throws InterruptedException {
        return termination.await(timeout, unit);
    }

    protected void finalize() {
        shutdown();
    }

    public int getNumberOfConnections() {
        return connections.getNumberOfConnections();
    }

    public synchronized void shutdown() {
        if (state == State.INITIALIZED) {
            state = State.TERMINATED;
            termination.countDown();
            LOG.info("Server shutdown, was never started");
        } else if (state == State.RUNNING) {
            LOG.info("Shutting down server");
            state = State.SHUTDOWN;
            new ShutdownThread().start();
        }
    }

    public synchronized void start() throws Exception {
        if (state == State.INITIALIZED) {
            LOG.info("Server with id = " + id + " starting");

            // Schedules the tracker to recalcute positions every second.
            ses.scheduleAtFixedRate(tracker, 0, 1, TimeUnit.SECONDS);
            // Starts a new thread that will accept new connections
            try {
                factory.startAccept(connections);
            } catch (Exception e) {
                state = State.TERMINATED;
                factory.close();
                throw e;
            }
            // Set to running state
            state = State.RUNNING;
        }
    }

    synchronized void tryTerminate() {
        termination.countDown();
        LOG.info("Server Terminated");
    }

    class ShutdownThread extends Thread {
        ShutdownThread() {
            setDaemon(true);
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            LOG.info("Shutdown thread started");
            System.out.println("Stopping for acceptance of new connections");
            try {
                factory.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            LOG.info("Acceptance of new connections stopped, closing existing");
            int size = connections.getNumberOfConnections();
            if (size > 0) {
                LOG.info("Shutting down " + size + " connections");
            }
            // TODO close all connections
            // Think we should send them a termination signal
            tryTerminate();
        }
    }

    static enum State {
        INITIALIZED, RUNNING, SHUTDOWN, SHUTDOWN_NOW, TERMINATED;
        boolean isShutdown() {
            return this == SHUTDOWN || this == SHUTDOWN_NOW || this == TERMINATED;
        }
    }
}
