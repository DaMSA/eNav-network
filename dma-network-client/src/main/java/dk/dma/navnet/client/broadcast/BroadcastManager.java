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

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;

import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.enav.maritimecloud.broadcast.BroadcastFuture;
import dk.dma.enav.maritimecloud.broadcast.BroadcastListener;
import dk.dma.enav.maritimecloud.broadcast.BroadcastMessage;
import dk.dma.enav.maritimecloud.broadcast.BroadcastMessageHeader;
import dk.dma.enav.maritimecloud.broadcast.BroadcastOptions;
import dk.dma.enav.maritimecloud.broadcast.BroadcastSubscription;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.navnet.client.ClientContainer;
import dk.dma.navnet.client.connection.ConnectionMessageBus;
import dk.dma.navnet.client.connection.OnMessage;
import dk.dma.navnet.client.service.PositionManager;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastAck;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastDeliver;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSendAck;

/**
 * Manages sending and receiving of broadcasts.
 * 
 * @author Kasper Nielsen
 */
public class BroadcastManager {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastManager.class);

    private final ConcurrentMap<Long, DefaultOutstandingBroadcast> outstandingBroadcasts = new MapMaker().weakValues()
            .makeMap();

    /** The client */
    private final MaritimeId clientId;

    /** The network */
    private final ConnectionMessageBus connection;

    /** A map of local broadcast listeners. ChannelName -> List of listeners. */
    final ConcurrentHashMapV8<String, CopyOnWriteArraySet<BroadcastMessageSubscription>> listeners = new ConcurrentHashMapV8<>();

    /** Maintains latest position for the client. */
    private final PositionManager positionManager;

    /** Thread manager takes care of asynchronous processing. */
    private final ThreadManager threadManager;

    /**
     * Creates a new instance of this class.
     * 
     * @param network
     *            the network
     */
    public BroadcastManager(PositionManager positionManager, ThreadManager threadManager, ClientContainer client,
            ConnectionMessageBus connection) {
        this.connection = requireNonNull(connection);
        this.positionManager = requireNonNull(positionManager);
        this.threadManager = requireNonNull(threadManager);
        this.clientId = requireNonNull(client.getLocalId());
    }

    /**
     * Sets up listeners for incoming broadcast messages.
     * 
     * @param messageType
     *            the type of message to receive
     * @param listener
     *            the callback listener
     * @return a subscription
     * @see MaritimeCloudClient#broadcastListen(Class, BroadcastListener)
     */
    public <T extends BroadcastMessage> BroadcastSubscription listenFor(Class<T> messageType,
            BroadcastListener<T> listener) {
        requireNonNull(messageType, "messageType is null");
        requireNonNull(listener, "listener is null");

        String channelName = messageType.getCanonicalName();

        BroadcastMessageSubscription sub = new BroadcastMessageSubscription(this, channelName, listener);

        listeners.computeIfAbsent(messageType.getCanonicalName(),
                new ConcurrentHashMapV8.Fun<String, CopyOnWriteArraySet<BroadcastMessageSubscription>>() {
                    public CopyOnWriteArraySet<BroadcastMessageSubscription> apply(String t) {
                        return new CopyOnWriteArraySet<>();
                    }
                }).add(sub);
        return sub;
    }


    @OnMessage
    public void onBroadcastAck(BroadcastAck ack) {
        DefaultOutstandingBroadcast f = outstandingBroadcasts.get(ack.getBroadcastId());
        // if we do not have a valid outstanding broadcast just ignore the ack
        if (f != null) {
            f.onAckMessage(ack);
        }
    }

    /**
     * Invoked whenever a broadcast message is received from a remote actor.
     * 
     * @param broadcast
     *            the broadcast that was received
     */
    @OnMessage
    public void onBroadcastMessage(BroadcastDeliver broadcast) {
        // Find out if we actually listens for it
        CopyOnWriteArraySet<BroadcastMessageSubscription> set = listeners.get(broadcast.getChannel());
        if (set != null && !set.isEmpty()) {
            BroadcastMessage bm = null;
            try {
                bm = broadcast.tryRead();
            } catch (Exception e) {
                LOG.error("Exception while trying to deserialize an incoming broadcast message ", e);
                LOG.error(broadcast.toJSON());
            }

            final BroadcastMessage bmm = bm;
            final BroadcastMessageHeader bp = new BroadcastMessageHeader(broadcast.getId(), broadcast.getPositionTime());

            // Deliver to each listener
            for (final BroadcastMessageSubscription s : set) {
                threadManager.execute(new Runnable() {
                    public void run() {
                        s.deliver(bp, bmm); // deliver() handles any exception
                    }
                });
            }
        }
    }

    public BroadcastFuture sendBroadcastMessage(BroadcastMessage broadcast, BroadcastOptions options) {
        requireNonNull(broadcast, "broadcast is null");
        requireNonNull(options, "options is null");
        options = options.immutable(); // Make the options immutable just in case

        // create the message we will send to the server
        BroadcastSend b = BroadcastSend.create(clientId, positionManager.getPositionTime(), broadcast, options);

        final DefaultOutstandingBroadcast dob = new DefaultOutstandingBroadcast(threadManager, options);
        outstandingBroadcasts.put(b.getReplyTo(), dob);

        DefaultConnectionFuture<BroadcastSendAck> response = connection.sendMessage(b);
        response.handle(new BiConsumer<BroadcastSendAck, Throwable>() {
            public void accept(BroadcastSendAck ack, Throwable cause) {
                if (ack != null) {
                    dob.receivedOnServer.complete(null);
                } else {
                    dob.receivedOnServer.completeExceptionally(cause);
                    // remove from broadcasts??
                }
            }
        });
        return dob;
    }
}
