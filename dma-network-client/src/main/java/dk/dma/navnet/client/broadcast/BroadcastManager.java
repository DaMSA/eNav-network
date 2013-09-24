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

import java.util.concurrent.CopyOnWriteArraySet;

import jsr166e.ConcurrentHashMapV8;

import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.broadcast.BroadcastListener;
import dk.dma.enav.communication.broadcast.BroadcastMessage;
import dk.dma.enav.communication.broadcast.BroadcastMessageHeader;
import dk.dma.enav.communication.broadcast.BroadcastSubscription;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.client.ClientInfo;
import dk.dma.navnet.client.connection.ClientConnection;
import dk.dma.navnet.client.service.PositionManager;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastMsg;

/**
 * Manages sending and receiving of broadcasts.
 * 
 * @author Kasper Nielsen
 */
public class BroadcastManager implements Startable {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastManager.class);

    /** The network */
    private final ClientConnection connection;

    /** A map of listeners. ChannelName -> List of listeners. */
    final ConcurrentHashMapV8<String, CopyOnWriteArraySet<Subscription>> listeners = new ConcurrentHashMapV8<>();

    private final PositionManager positionManager;

    /** Thread manager takes care of asynchronous processing. */
    private final ThreadManager threadManager;

    private final ClientInfo clientInfo;

    /**
     * Creates a new instance of this class.
     * 
     * @param network
     *            the network
     */
    public BroadcastManager(PositionManager positionManager, ThreadManager threadManager, ClientInfo clientInfo,
            ClientConnection connection) {
        this.connection = requireNonNull(connection);
        this.positionManager = requireNonNull(positionManager);
        this.threadManager = requireNonNull(threadManager);
        this.clientInfo = requireNonNull(clientInfo);
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
    public <T extends BroadcastMessage> BroadcastSubscription listenFor(Class<T> messageType,
            BroadcastListener<T> listener) {
        Subscription sub = new Subscription(this, getChannelName(messageType), listener);
        listeners.computeIfAbsent(messageType.getCanonicalName(),
                new ConcurrentHashMapV8.Fun<String, CopyOnWriteArraySet<Subscription>>() {
                    public CopyOnWriteArraySet<Subscription> apply(String t) {
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
        CopyOnWriteArraySet<Subscription> set = listeners.get(broadcast.getChannel());
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
            for (final Subscription s : set) {
                threadManager.execute(new Runnable() {
                    public void run() {
                        s.deliver(bp, bmm);
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
    public void sendBroadcastMessage(BroadcastMessage broadcast) {
        requireNonNull(broadcast, "broadcast is null");
        BroadcastMsg b = BroadcastMsg.create(clientInfo.getLocalId(), positionManager.getPositionTime(), broadcast);
        connection.sendConnectionMessage(b);
    }

    /** Translates a class to a channel name. */
    private static String getChannelName(Class<?> c) {
        return c.getCanonicalName();
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        connection.subscribe(BroadcastMsg.class, new Consumer<BroadcastMsg>() {
            public void accept(BroadcastMsg t) {
                onBroadcastMessage(t);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {}
}
