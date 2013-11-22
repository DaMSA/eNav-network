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
package dk.dma.navnet.client.service;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.maritimecloud.MaritimeCloudClientConfiguration;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.client.connection.ConnectionMessageBus;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.auxiliary.PositionReportMessage;

/**
 * A runnable that will keep sending a keep alive signal.
 * 
 * @author Kasper Nielsen
 */
public class PositionManager implements Startable {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(PositionManager.class);

    /** Send out a signal no more often than. */
    static long minimumSignalDuration;

    /** Responsible for creating a current position and time. */
    private final Supplier<PositionTime> positionSupplier;

    /** The connection to the server. */
    private final ConnectionMessageBus connection;

    /** When we send the last message */
    private volatile long latestTime; // <0 so we always send it first time

    private final ThreadManager threadManager;

    /**
     * @param transport
     * @param positionSupplier
     */
    public PositionManager(ConnectionMessageBus connection, MaritimeCloudClientConfiguration builder,
            ThreadManager threadManager) {
        this.connection = requireNonNull(connection);
        this.positionSupplier = requireNonNull(builder.getPositionSupplier());
        this.threadManager = threadManager;
        minimumSignalDuration = builder.getKeepAlive(TimeUnit.NANOSECONDS);
        latestTime = System.nanoTime();// -minimumSignalDuration;
    }

    void sendSignal() {
        long now = System.nanoTime();
        // Only send a message if it is more MINIMUM_SIGNAL_DURATION time since the last signal
        if (now - latestTime < minimumSignalDuration) {
            return;
        }

        PositionTime t = null;
        try {
            t = getPositionTime();
        } catch (Exception e) {
            LOG.error("Could not create a KeepAlive position", e);
            return;
        }

        latestTime = now;
        connection.sendConnectionMessage(new PositionReportMessage(t));
    }

    public PositionTime getPositionTime() {
        PositionTime pt = positionSupplier == null ? null : positionSupplier.get();
        if (pt == null) {
            // We just send a dummy position
            // We should probably just send ", ," instead of "0,0," as the position
            pt = new PositionTime(0, 0, System.currentTimeMillis());
        }
        return pt;
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        threadManager.scheduleAtFixedRate(new Runnable() {
            public void run() {
                sendSignal();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {}
}
