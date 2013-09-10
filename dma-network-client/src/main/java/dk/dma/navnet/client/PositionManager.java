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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.messages.auxiliary.PositionReportMessage;

/**
 * A runnable that will keep sending a keep alive signal.
 * 
 * @author Kasper Nielsen
 */
class PositionManager implements Runnable {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(PositionManager.class);

    /** Send out a signal no more often than. */
    static final long MINIMUM_SIGNAL_DURATION = TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS);

    /** Responsible for creating a current position and time. */
    private final Supplier<PositionTime> positionSupplier;

    /** The connection to the server. */
    private final DefaultPersistentConnection c;

    /** When we send the last message */
    private volatile long latestTime = -MINIMUM_SIGNAL_DURATION; // System.nanoTime>0 so we always send it first time

    /**
     * @param transport
     * @param positionSupplier
     */
    PositionManager(DefaultPersistentConnection c, Supplier<PositionTime> positionSupplier) {
        this.c = requireNonNull(c);
        this.positionSupplier = requireNonNull(positionSupplier);
    }

    @Override
    public void run() {
        long now = System.nanoTime();
        // Only send a message if it is more MINIMUM_SIGNAL_DURATION time since the last signal
        if (now - latestTime < MINIMUM_SIGNAL_DURATION) {
            return;
        }

        PositionTime t = null;
        try {
            t = getPositionTime();
        } catch (Exception e) {
            LOG.error("Could not create a KeepAlive position", e);
        }

        if (t != null) {
            latestTime = now;
            c.connection().sendConnectionMessage(new PositionReportMessage(t));
        }
    }

    PositionTime getPositionTime() {
        PositionTime pt = positionSupplier == null ? null : positionSupplier.get();
        if (pt == null) {
            // We just send a dummy position
            // We should probably just send ", ," instead of "0,0," as the position
            pt = new PositionTime(0, 0, System.currentTimeMillis());
        }
        return pt;
    }
}
