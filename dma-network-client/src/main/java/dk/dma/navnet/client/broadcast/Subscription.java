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
package dk.dma.navnet.client.broadcast;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.broadcast.BroadcastListener;
import dk.dma.enav.communication.broadcast.BroadcastMessage;
import dk.dma.enav.communication.broadcast.BroadcastMessageHeader;
import dk.dma.enav.communication.broadcast.BroadcastSubscription;

/**
 * The default implementation of BroadcastSubscription.
 * 
 * @author Kasper Nielsen
 */
class Subscription implements BroadcastSubscription {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Subscription.class);

    /** The broadcast manager. */
    private final BroadcastManager broadcastManager;

    /** The type of broadcast messages. */
    private final String channel;

    /** The number of messages received. */
    private final AtomicLong count = new AtomicLong();

    /** The listener. */
    private final BroadcastListener<? extends BroadcastMessage> listener;

    Subscription(BroadcastManager broadcastManager, String channel,
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
    public String getChannel() {
        return channel;
    }

    /** {@inheritDoc} */
    @Override
    public long getNumberOfReceivedMessages() {
        return count.get();
    }
}
