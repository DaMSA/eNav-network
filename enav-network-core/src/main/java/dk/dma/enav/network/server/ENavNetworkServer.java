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
package dk.dma.enav.network.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import dk.dma.commons.tracker.PositionTracker;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.network.Packets;
import dk.dma.enav.network.PersistentConnection;
import dk.dma.enav.service.spi.MaritimeService;

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

    public ENavNetworkServer(int port) {
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

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     */
    void packetRead(PersistentConnection con, Packets p) throws Exception {
        if (!id.equals(p.destination)) {
            PersistentConnection c = at.getConnection(p.destination.toString());
            if (c == null) {
                System.err.println("Unknown destination " + p.destination);
                System.err.println("Available " + at.getAllConnectionIds());
            } else {
                LOG.debug("Relaying packet from " + p.src + " to " + p.destination);
                c.packetWrite(p);
            }
            return;
        }

        switch (p.b) {
        case Packets.POSITION_REPORT:
            PositionTime pt = (PositionTime) p.payloadToObject();
            tracker.update(p.src, pt);
            break;
        case Packets.FIND_FOR_SHAPE:
            Area s = (Area) p.payloadToObject();
            con.packetWrite(p.replyWith(tracker.getTargetsWithin(s)));
            break;
        case Packets.REGISTER_SERVICE:
            MaritimeService ms = p.payloadToObject();
            registeredServices.registerService(p.src, ms);
            con.packetWrite(p.replyWith(1));
            break;
        case Packets.FIND_SERVICE:
            Class<? extends MaritimeService> serviceType = p.payloadToObject();
            con.packetWrite(p.replyWith(registeredServices.findServicesOfType(serviceType)));
            break;
        case Packets.CONNECTION_DISCONNECT:
            at.disconnected(con);
            registeredServices.remove(con.getRemoteID());
            con.packetWrite(Packets.logoutResponse());
            break;
        }
    }

    public synchronized void shutdown() {
        if (state == State.INITIALIZED) {
            state = State.TERMINATED;
        } else if (state == State.RUNNING) {
            LOG.info("Shutting down server");
            state = State.SHUTDOWN;
            at.stopAccepting();
            new ShutdownThread().start();
        }
    }

    public synchronized void start() {
        if (state == State.INITIALIZED) {
            LOG.info("Server with id = " + id + " starting");

            // Schedules the tracker to recalcute positions every second.
            ses.scheduleAtFixedRate(tracker, 0, 1, TimeUnit.SECONDS);
            // Starts a new thread that will accept new connections
            at.start();
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
