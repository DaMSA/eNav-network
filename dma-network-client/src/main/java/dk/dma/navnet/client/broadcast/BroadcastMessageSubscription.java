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
package dk.dma.navnet.client.broadcast;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.maritimecloud.broadcast.BroadcastListener;
import dk.dma.enav.maritimecloud.broadcast.BroadcastMessage;
import dk.dma.enav.maritimecloud.broadcast.BroadcastMessageHeader;
import dk.dma.enav.maritimecloud.broadcast.BroadcastSubscription;

/**
 * The default implementation of {@link BroadcastSubscription}.
 * 
 * @author Kasper Nielsen
 */
class BroadcastMessageSubscription implements BroadcastSubscription {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastMessageSubscription.class);

    /** The broadcast manager. */
    private final BroadcastManager broadcastManager;

    /** The type of broadcast messages. */
    private final String channel;

    /** The number of messages received. */
    private final AtomicLong count = new AtomicLong();

    /** The listener. */
    private final BroadcastListener<? extends BroadcastMessage> listener;

    BroadcastMessageSubscription(BroadcastManager broadcastManager, String channel,
            BroadcastListener<? extends BroadcastMessage> listener) {
        this.broadcastManager = requireNonNull(broadcastManager);
        this.channel = requireNonNull(channel);
        this.listener = requireNonNull(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        broadcastManager.listeners.get(channel).remove(this);
    }

    // invoked in another thread so never throw anything
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void deliver(BroadcastMessageHeader broadcastHeader, BroadcastMessage message) {
        try {
            ((BroadcastListener) listener).onMessage(broadcastHeader, message);
            count.incrementAndGet();
        } catch (Exception e) {
            LOG.error("Exception while handling an incoming broadcast message of type " + message.getClass(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getChannel() {
        return channel;
    }

    /** {@inheritDoc} */
    @Override
    public long getNumberOfReceivedMessages() {
        return count.get();
    }
}
