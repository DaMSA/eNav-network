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

import java.util.concurrent.TimeUnit;

import org.picocontainer.PicoContainer;

import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.communication.MaritimeNetworkConnectionBuilder;
import dk.dma.enav.communication.MaritimeNetworkConnection;
import dk.dma.enav.communication.broadcast.BroadcastListener;
import dk.dma.enav.communication.broadcast.BroadcastMessage;
import dk.dma.enav.communication.broadcast.BroadcastSubscription;
import dk.dma.enav.communication.service.InvocationCallback;
import dk.dma.enav.communication.service.ServiceLocator;
import dk.dma.enav.communication.service.ServiceRegistration;
import dk.dma.enav.communication.service.spi.ServiceInitiationPoint;
import dk.dma.enav.communication.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.client.broadcast.BroadcastManager;
import dk.dma.navnet.client.connection.ClientConnection;
import dk.dma.navnet.client.service.ClientServiceManager;

/**
 * An implementation of {@link MaritimeNetworkConnection} using WebSockets and JSON. This class delegates all work to other
 * services.
 * 
 * @author Kasper Nielsen
 */
public class DefaultMaritimeNetworkConnection implements MaritimeNetworkConnection {

    /** Responsible for listening and sending broadcasts. */
    private final BroadcastManager broadcaster;

    /** The id of this client */
    private final ClientInfo clientInfo;

    /** The single connection to a server. */
    private final ClientConnection connection;

    /** Manages registration of services. */
    private final ClientServiceManager services;

    /** The internal client. */
    private final InternalClient internalClient;

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration of the connection
     */
    public DefaultMaritimeNetworkConnection(MaritimeNetworkConnectionBuilder builder) {
        PicoContainer pc = InternalClient.create(builder);
        broadcaster = pc.getComponent(BroadcastManager.class);
        clientInfo = pc.getComponent(ClientInfo.class);
        internalClient = pc.getComponent(InternalClient.class);
        connection = pc.getComponent(ClientConnection.class);
        services = pc.getComponent(ClientServiceManager.class);
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitState(MaritimeNetworkConnection.State state, long timeout, TimeUnit unit)
            throws InterruptedException {
        return internalClient.awaitState(state, timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public void broadcast(BroadcastMessage message) {
        broadcaster.sendBroadcastMessage(message);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends BroadcastMessage> BroadcastSubscription broadcastListen(Class<T> messageType,
            BroadcastListener<T> consumer) {
        return broadcaster.listenFor(messageType, consumer);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        internalClient.close();
    }

    /** {@inheritDoc} */
    @Override
    public void connect() {
        internalClient.connect();
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect() {}

    /** {@inheritDoc} */
    @Override
    public String getConnectionId() {
        return connection.getConnectionId2();
    }

    /** {@inheritDoc} */
    @Override
    public final MaritimeId getLocalId() {
        return clientInfo.getLocalId();
    }

    /** {@inheritDoc} */
    @Override
    public MaritimeNetworkConnection.State getState() {
        return internalClient.getState();
    }

    /** {@inheritDoc} */
    @Override
    public <T, E extends ServiceMessage<T>> ServiceLocator<T, E> serviceFind(ServiceInitiationPoint<E> sip) {
        return services.serviceFind(sip);
    }

    /** {@inheritDoc} */
    @Override
    public <T, S extends ServiceMessage<T>> ConnectionFuture<T> serviceInvoke(MaritimeId id, S initiatingServiceMessage) {
        return services.invokeService(id, initiatingServiceMessage);
    }

    /** {@inheritDoc} */
    @Override
    public <T, E extends ServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            InvocationCallback<E, T> callback) {
        return services.serviceRegister(sip, callback);
    }
}
