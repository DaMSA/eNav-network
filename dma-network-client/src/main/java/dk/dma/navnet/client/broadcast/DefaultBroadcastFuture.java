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

import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.communication.broadcast.BroadcastFuture;
import dk.dma.enav.communication.broadcast.BroadcastMessage.Ack;
import dk.dma.enav.communication.broadcast.BroadcastOptions;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;

/**
 * The default implementation of {@link BroadcastFuture}.
 * 
 * @author Kasper Nielsen
 */
class DefaultBroadcastFuture implements BroadcastFuture {

    private final List<Ack> acks = new ArrayList<>();

    /** All registered consumers. */
    private final CopyOnWriteArrayList<Consumer<? super Ack>> consumers = new CopyOnWriteArrayList<>();

    /** The main lock. */
    private final ReentrantLock lock = new ReentrantLock();

    final DefaultConnectionFuture<Void> receivedOnServer;

    final BroadcastOptions options;

    DefaultBroadcastFuture(ThreadManager tm, BroadcastOptions options) {
        receivedOnServer = new DefaultConnectionFuture<>(tm.getScheduler());
        this.options = options;
    }

    /** {@inheritDoc} */
    @Override
    public void onAck(Consumer<? super Ack> consumer) {
        requireNonNull(consumer);
        if (!options.isReceiverAckEnabled()) {
            throw new UnsupportedOperationException("Receiver ack is not enabled");
        }
        lock.lock();
        try {
            consumers.add(consumer);
            // We need to replay acks in case someone have already acked a messages.
            // before this method is called (big GC pause, for example)
            for (Ack a : acks) {
                consumer.accept(a);
            }
        } finally {
            lock.unlock();
        }
    }

    void onMessage(Ack ack) {
        requireNonNull(ack);
        lock.lock();
        try {
            acks.add(ack);
            for (Consumer<? super Ack> c : consumers) {
                c.accept(ack);
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
