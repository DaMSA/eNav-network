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

import dk.dma.enav.maritimecloud.ConnectionFuture;
import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.enav.maritimecloud.MaritimeCloudClientConfiguration;
import dk.dma.enav.maritimecloud.MaritimeCloudConnection;
import dk.dma.enav.maritimecloud.broadcast.BroadcastFuture;
import dk.dma.enav.maritimecloud.broadcast.BroadcastListener;
import dk.dma.enav.maritimecloud.broadcast.BroadcastMessage;
import dk.dma.enav.maritimecloud.broadcast.BroadcastOptions;
import dk.dma.enav.maritimecloud.broadcast.BroadcastSubscription;
import dk.dma.enav.maritimecloud.service.ServiceLocator;
import dk.dma.enav.maritimecloud.service.invocation.InvocationCallback;
import dk.dma.enav.maritimecloud.service.registration.ServiceRegistration;
import dk.dma.enav.maritimecloud.service.spi.ServiceInitiationPoint;
import dk.dma.enav.maritimecloud.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.client.broadcast.BroadcastManager;
import dk.dma.navnet.client.service.ClientServiceManager;

/**
 * An implementation of {@link MaritimeCloudClient} using WebSockets and JSON. This class delegates all work to other
 * services.
 * 
 * @author Kasper Nielsen
 */
public class DefaultMaritimeCloudClient implements MaritimeCloudClient {

    /** Responsible for listening and sending broadcasts. */
    private final BroadcastManager broadcaster;

    /** Manages registration of services. */
    private final MaritimeCloudConnection connection;

    /** The internal client. */
    private final ClientContainer internalClient;

    /** Manages registration of services. */
    private final ClientServiceManager services;

    private final BroadcastOptions broadcastDefaultOptions;

    /**
     * Creates a new instance of this class.
     * 
     * @param builder
     *            the configuration of the connection
     */
    public DefaultMaritimeCloudClient(MaritimeCloudClientConfiguration configuration) {
        PicoContainer pc = ClientContainer.create(configuration);
        broadcaster = pc.getComponent(BroadcastManager.class);
        connection = pc.getComponent(MaritimeCloudConnection.class);
        internalClient = pc.getComponent(ClientContainer.class);
        services = pc.getComponent(ClientServiceManager.class);
        broadcastDefaultOptions = configuration.getDefaultBroadcastOptions().immutable();
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return internalClient.awaitTermination(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public BroadcastFuture broadcast(BroadcastMessage message) {
        return broadcast(message, broadcastDefaultOptions);
    }

    /** {@inheritDoc} */
    @Override
    public BroadcastFuture broadcast(BroadcastMessage message, BroadcastOptions options) {
        return broadcaster.sendBroadcastMessage(message, options);
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
    public MaritimeCloudConnection connection() {
        return connection;
    }

    /** {@inheritDoc} */
    @Override
    public final MaritimeId getClientId() {
        return internalClient.getLocalId();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClosed() {
        return internalClient.isClosed();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTerminated() {
        return internalClient.isTerminated();
    }

    /** {@inheritDoc} */
    @Override
    public <T, E extends ServiceMessage<T>> ServiceLocator<T, E> serviceLocate(ServiceInitiationPoint<E> sip) {
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
