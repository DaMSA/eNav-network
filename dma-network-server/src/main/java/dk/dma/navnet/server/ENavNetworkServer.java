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

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import dk.dma.commons.tracker.PositionTracker;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.shore.ServerId;

/**
 * 
 * @author Kasper Nielsen
 */
public class ENavNetworkServer {

    final ServerId id = new ServerId(1);

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ENavNetworkServer.class);

    final ServiceManager registeredServices = new ServiceManager();

    final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("PositionTrackerUpdate").setDaemon(true).build());

    /** The thread that is accepting incoming sockets. */
    final ConnectionManager at;

    /** The current state of this server. */
    volatile State state = State.INITIALIZED;

    /** A latch used for waiting on complete shutdown. */
    final CountDownLatch termination = new CountDownLatch(1);

    /** The position tracker. */
    final PositionTracker<MaritimeId> tracker = new PositionTracker<>();

    final Server server;

    public ENavNetworkServer(int port) {
        server = new Server(new InetSocketAddress(port));
        at = new ConnectionManager(this, new InetSocketAddress(port));
    }

    public boolean awaitTerminated(long timeout, TimeUnit unit) throws InterruptedException {
        return termination.await(timeout, unit);
    }

    protected void finalize() {
        shutdown();
    }

    public int getNumberOfConnections() {
        return at.getNumberOfConnections();
    }

    public synchronized void shutdown() {
        if (state == State.INITIALIZED) {
            state = State.TERMINATED;
            termination.countDown();
            LOG.info("Server shutdown, was never started");
        } else if (state == State.RUNNING) {
            LOG.info("Shutting down server");
            state = State.SHUTDOWN;

            // at.stopAccepting();
            new ShutdownThread().start();
        }
    }

    public synchronized void start() {
        if (state == State.INITIALIZED) {
            LOG.info("Server with id = " + id + " starting");

            // Schedules the tracker to recalcute positions every second.
            ses.scheduleAtFixedRate(tracker, 0, 1, TimeUnit.SECONDS);
            // Starts a new thread that will accept new connections
            try {
                server.start();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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

        /** {@inheritDoc} */
        @Override
        public void run() {
            LOG.info("Shutdown thread started");
            try {
                server.stop();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            LOG.info("JettyServer stopped");
            int size = at.getNumberOfConnections();
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
