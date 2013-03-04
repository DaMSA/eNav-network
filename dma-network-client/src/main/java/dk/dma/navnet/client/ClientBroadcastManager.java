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

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.net.broadcast.BroadcastListener;
import dk.dma.enav.net.broadcast.BroadcastMessage;
import dk.dma.enav.net.broadcast.BroadcastProperties;
import dk.dma.enav.net.broadcast.BroadcastSubscription;
import dk.dma.navnet.core.messages.c2c.Broadcast;

/**
 * Manages sending and receiving of broadcasts.
 * 
 * @author Kasper Nielsen
 */
class ClientBroadcastManager {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ClientBroadcastManager.class);

    /** The network */
    final ClientNetwork c;

    /** A map of subscribers. ChannelName -> List of listeners. */
    final ConcurrentHashMapV8<String, CopyOnWriteArraySet<Subcription>> subscribers = new ConcurrentHashMapV8<>();

    /**
     * Creates a new instance of this class.
     * 
     * @param network
     *            the network
     */
    ClientBroadcastManager(ClientNetwork network) {
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
        Subcription sub = new Subcription(getChannelName(messageType), listener);
        subscribers.computeIfAbsent(messageType.getCanonicalName(),
                new ConcurrentHashMapV8.Fun<String, CopyOnWriteArraySet<Subcription>>() {
                    public CopyOnWriteArraySet<Subcription> apply(String t) {
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
    void receive(Broadcast broadcast) {
        CopyOnWriteArraySet<Subcription> set = subscribers.get(broadcast.getChannel());
        if (set != null && !set.isEmpty()) {
            final BroadcastMessage bm = broadcast.tryRead();
            final BroadcastProperties bp = new BroadcastProperties(broadcast.getId(), broadcast.getPositionTime());

            // Deliver to each listener
            for (final Subcription s : set) {
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
    void send(BroadcastMessage broadcast) {
        requireNonNull(broadcast, "broadcast is null");
        Broadcast b = Broadcast.create(c.clientId, c.positionManager.getPositionTime(), broadcast);
        c.connection.sendMessage(b);
    }

    /** Translates a class to a channel name. */
    private static String getChannelName(Class<?> c) {
        return c.getCanonicalName();
    }

    /** The default implementation of BroadcastSubscription. */
    class Subcription implements BroadcastSubscription {

        /** The number of messages received. */
        private final AtomicLong count = new AtomicLong();

        /** The type of broadcast messages */
        private final String key;

        /** The listener */
        private final BroadcastListener<? extends BroadcastMessage> listener;

        /**
         * @param listener
         */
        Subcription(String key, BroadcastListener<? extends BroadcastMessage> listener) {
            this.key = requireNonNull(key);
            this.listener = requireNonNull(listener);
        }

        /** {@inheritDoc} */
        @Override
        public void cancel() {
            subscribers.get(key).remove(this);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        void deliver(BroadcastProperties properties, BroadcastMessage message) {
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
    }
}
