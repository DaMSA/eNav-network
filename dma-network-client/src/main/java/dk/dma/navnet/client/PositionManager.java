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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.core.messages.s2c.PositionReportMessage;

/**
 * A runnable that will keep sending a keep alive signal.
 * 
 * @author Kasper Nielsen
 */
class PositionManager implements Runnable {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(PositionManager.class);

    /** Send out a signal no more often than. */
    static final long MINIMUM_SIGNAL_DURATION = TimeUnit.SECONDS.convert(4, TimeUnit.NANOSECONDS);

    /** Responsible for creating a current position and time. */
    private final Supplier<PositionTime> positionSupplier;

    /** The connection to the server. */
    private final ClientNetwork c;

    /** When we send the last message */
    private volatile long latestMessage = -MINIMUM_SIGNAL_DURATION; // System.nanoTime>0 so we always send it first time

    /**
     * @param connection
     * @param positionSupplier
     */
    PositionManager(ClientNetwork c, Supplier<PositionTime> positionSupplier) {
        this.c = requireNonNull(c);
        this.positionSupplier = requireNonNull(positionSupplier);
    }

    @Override
    public void run() {
        long now = System.nanoTime();
        // Only send a message if it is more MINIMUM_SIGNAL_DURATION time since the last signal
        if (now - latestMessage < MINIMUM_SIGNAL_DURATION) {
            return;
        }

        PositionTime t = null;
        try {
            t = getPositionTime();
        } catch (Exception e) {
            LOG.error("Could not create a KeepAlive position", e);
        }

        if (t != null) {
            latestMessage = now;
            c.connection.sendMessage(new PositionReportMessage(t));
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