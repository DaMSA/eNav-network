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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.maritimecloud.ConnectionFuture;
import dk.dma.enav.maritimecloud.broadcast.BroadcastFuture;
import dk.dma.enav.maritimecloud.broadcast.BroadcastOptions;
import dk.dma.enav.maritimecloud.broadcast.BroadcastMessage.Ack;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;

/**
 * The default implementation of {@link BroadcastFuture}.
 * 
 * @author Kasper Nielsen
 */
class DefaultBroadcastFuture implements BroadcastFuture {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastManager.class);

    /** A list of all ACKs we have received. */
    private final List<Ack> acks = new ArrayList<>();

    /** All registered consumers. */
    private final CopyOnWriteArrayList<Consumer<? super Ack>> consumers = new CopyOnWriteArrayList<>();

    /** The main lock. */
    private final ReentrantLock lock = new ReentrantLock();

    /** The options for this broadcast. */
    private final BroadcastOptions options;

    /** A connection future used to determined if the broadcast message has been received on the server */
    final DefaultConnectionFuture<Void> receivedOnServer;

    DefaultBroadcastFuture(ThreadManager tm, BroadcastOptions options) {
        this.receivedOnServer = tm.create();
        this.options = requireNonNull(options);
    }

    /** {@inheritDoc} */
    @Override
    public void onAck(Consumer<? super Ack> consumer) {
        requireNonNull(consumer);
        if (!options.isReceiverAckEnabled()) {
            throw new UnsupportedOperationException("Receiver ack is not enabled, must be set in BroadcastOptions");
        }
        lock.lock();
        try {
            consumers.add(consumer);
            // We need to replay acks in case someone have already acked a messages.
            // before this method is called (big GC pause, for example)
            for (Ack ack : acks) {
                try {
                    consumer.accept(ack);
                } catch (Exception e) {
                    LOG.error("Failed to process broadcast ack", e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    void onAckMessage(Ack ack) {
        requireNonNull(ack);
        lock.lock();
        try {
            acks.add(ack);
            for (Consumer<? super Ack> consumer : consumers) {
                try {
                    consumer.accept(ack);
                } catch (Exception e) {
                    LOG.error("Failed to process broadcast ack", e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionFuture<Void> receivedOnServer() {
        return receivedOnServer;
    }
}
