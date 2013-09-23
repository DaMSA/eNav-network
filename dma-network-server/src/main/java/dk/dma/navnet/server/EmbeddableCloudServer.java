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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
public class EmbeddableCloudServer {

    /** The default port this server is running on. */
    public static final int DEFAULT_PORT = 43234;

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(EmbeddableCloudServer.class);

    /** The thread that is accepting incoming sockets. */
    final ServerConnectionManager connectionManager;

    /** An unbounded pool of daemon threads. */
    final ExecutorService daemonPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("daemonPool").setDaemon(true).build());

    final TransportServerFactory factory;

    /** The id of the server, hard coded for now */
    private final ServerId id = new ServerId(1);

    /** A lock protecting startup and shutdown. */
    private final ReentrantLock lock = new ReentrantLock();

    final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("PositionTrackerUpdate").setDaemon(true).build());

    /** The current state of this server. */
    volatile State state = State.INITIALIZED;

    /** A latch used for waiting on complete shutdown. */
    final CountDownLatch termination = new CountDownLatch(1);

    /** The position tracker. */
    final PositionTracker<Target> tracker = new PositionTracker<>();

    static EmbeddableCloudServer SERVER;

    /** Creates a new ENavNetworkServer */
    public EmbeddableCloudServer() {
        this(DEFAULT_PORT);
    }

    public EmbeddableCloudServer(int port) {
        factory = TransportServerFactory.createServer(port);
        connectionManager = new ServerConnectionManager(this);
        SERVER = this;
    }

    public boolean awaitTerminated(long timeout, TimeUnit unit) throws InterruptedException {
        return termination.await(timeout, unit);
    }

    protected void finalize() {
        shutdown();
    }

    public MaritimeId getLocalId() {
        return id;
    }

    public int getNumberOfConnections() {
        return connectionManager.getNumberOfConnections();
    }

    public void shutdown() {
        lock.lock();
        try {
            if (state == State.INITIALIZED) {
                state = State.TERMINATED;
                termination.countDown();
                LOG.info("Server shutdown, was never started");
            } else if (state == State.RUNNING) {
                LOG.info("Shutting down server");
                state = State.SHUTDOWN;
                new ShutdownThread().start();
            }
        } finally {
            lock.unlock();
        }
    }

    public void start() throws Exception {
        lock.lock();
        try {
            if (state == State.INITIALIZED) {
                LOG.info("Server with id = " + id + " starting");
                // Schedules the tracker to recalculate positions every second.
                tracker.schedule(ses, 1);
                // Starts a new thread that will accept new connections
                try {
                    factory.startAccept(ServerTransport.class);
                } catch (Exception e) {
                    new ShutdownThread().start();
                    state = State.TERMINATED;
                    throw e;
                }
                // Set to running state
                state = State.RUNNING;
            }
        } finally {
            lock.unlock();
        }
    }

    void tryTerminate() {
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
                factory.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ses.shutdown();
            LOG.info("Acceptance of new connections stopped, closing existing");
            int size = connectionManager.getNumberOfConnections();
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
