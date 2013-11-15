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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.shore.ServerId;

/**
 * 
 * @author Kasper Nielsen
 */
class Server {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(Server.class);

    /** The thread that is accepting incoming sockets. */
    final ConnectionManager connectionManager;

    /** The id of the server, hard coded for now */
    private final ServerId id = new ServerId(1);

    /** A lock protecting startup and shutdown. */
    private final ReentrantLock lock = new ReentrantLock();

    /** The current state of this server. */
    volatile State state = State.INITIALIZED;

    /** A latch used for waiting on complete shutdown. */
    final CountDownLatch termination = new CountDownLatch(1);

    static Server SERVER;

    public Server(int port) {
        this(ServerConfiguration.from(port));
    }

    /** Creates a new ENavNetworkServer */
    public Server(ServerConfiguration configuration) {
        connectionManager = new ConnectionManager(this);
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
                // Starts a new thread that will accept new connections
                try {
                    factory.startAccept();
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
