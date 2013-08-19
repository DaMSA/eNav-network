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

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.broadcast.BroadcastListener;
import dk.dma.enav.communication.broadcast.BroadcastMessage;
import dk.dma.enav.communication.broadcast.BroadcastMessageHeader;
import dk.dma.enav.communication.broadcast.BroadcastSubscription;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;

/**
 * Manages sending and receiving of broadcasts.
 * 
 * @author Kasper Nielsen
 */
class BroadcastManager {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(BroadcastManager.class);

    /** The network */
    final DefaultPersistentConnection c;

    /** A map of listeners. ChannelName -> List of listeners. */
    final ConcurrentHashMapV8<String, CopyOnWriteArraySet<Listener>> listeners = new ConcurrentHashMapV8<>();

    /**
     * Creates a new instance of this class.
     * 
     * @param network
     *            the network
     */
    BroadcastManager(DefaultPersistentConnection network) {
        this.c = requireNonNull(network);
    }

    /**
     * Sets up listeners for incoming broadcast messages.
     * 
     * @param messageType
     *            the type of message to receive
     * @param listener
     *            the callback listener
     * @return a subscription
     */
    <T extends BroadcastMessage> BroadcastSubscription listenFor(Class<T> messageType, BroadcastListener<T> listener) {
        Listener sub = new Listener(getChannelName(messageType), listener);
        listeners.computeIfAbsent(messageType.getCanonicalName(),
                new ConcurrentHashMapV8.Fun<String, CopyOnWriteArraySet<Listener>>() {
                    public CopyOnWriteArraySet<Listener> apply(String t) {
                        return new CopyOnWriteArraySet<>();
                    }
                }).add(sub);
        return sub;
    }

    /**
     * Invoked whenever a broadcast message was received.
     * 
     * @param broadcast
     *            the broadcast that was received
     */
    void onBroadcastMessage(BroadcastMsg broadcast) {
        CopyOnWriteArraySet<Listener> set = listeners.get(broadcast.getChannel());
        if (set != null && !set.isEmpty()) {
            final BroadcastMessage bm = broadcast.tryRead();
            final BroadcastMessageHeader bp = new BroadcastMessageHeader(broadcast.getId(), broadcast.getPositionTime());

            // Deliver to each listener
            for (final Listener s : set) {
                c.es.execute(new Runnable() {
                    @Override
                    public void run() {
                        s.deliver(bp, bm);
                    }
                });
            }
        }
    }

    /**
     * Sends a broadcast.
     * 
     * @param broadcast
     *            the broadcast to send
     */
    void sendBroadcastMessage(BroadcastMessage broadcast) {
        requireNonNull(broadcast, "broadcast is null");
        BroadcastMsg b = BroadcastMsg.create(c.getLocalId(), c.positionManager.getPositionTime(), broadcast);
        c.connection().sendConnectionMessage(b);
    }

    /** Translates a class to a channel name. */
    private static String getChannelName(Class<?> c) {
        return c.getCanonicalName();
    }

    /** The default implementation of BroadcastSubscription. */
    class Listener implements BroadcastSubscription {

        /** The number of messages received. */
        private final AtomicLong count = new AtomicLong();

        /** The type of broadcast messages. */
        private final String channel;

        /** The listener. */
        private final BroadcastListener<? extends BroadcastMessage> listener;

        /**
         * @param listener
         */
        Listener(String channel, BroadcastListener<? extends BroadcastMessage> listener) {
            this.channel = requireNonNull(channel);
            this.listener = requireNonNull(listener);
        }

        /** {@inheritDoc} */
        @Override
        public void cancel() {
            listeners.get(channel).remove(this);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        void deliver(BroadcastMessageHeader properties, BroadcastMessage message) {
            try {
                ((BroadcastListener) listener).onMessage(properties, message);
                count.incrementAndGet();
            } catch (Exception e) {
                LOG.error("Exception while handling an incoming broadcast message of type " + message.getClass(), e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public long getNumberOfReceivedMessages() {
            return count.get();
        }

        /** {@inheritDoc} */
        @Override
        public String getChannel() {
            return channel;
        }
    }
}
