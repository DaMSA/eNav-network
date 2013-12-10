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
package dk.dma.navnet.client.service;

import static java.util.Objects.requireNonNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.maritimecloud.service.ServiceLocator;
import dk.dma.enav.maritimecloud.service.invocation.InvocationCallback;
import dk.dma.enav.maritimecloud.service.registration.ServiceRegistration;
import dk.dma.enav.maritimecloud.service.spi.ServiceInitiationPoint;
import dk.dma.enav.maritimecloud.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.client.ClientContainer;
import dk.dma.navnet.client.connection.ConnectionMessageBus;
import dk.dma.navnet.client.connection.OnMessage;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.c2c.service.InvokeService;
import dk.dma.navnet.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.messages.s2c.service.FindService;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.messages.s2c.service.RegisterService;
import dk.dma.navnet.messages.s2c.service.RegisterServiceResult;

/**
 * Manages local and remote services.
 * 
 * @author Kasper Nielsen
 */
public class ClientServiceManager {

    final ConnectionMessageBus connection;

    /** The client container. */
    private final ClientContainer container;

    private final ConcurrentHashMap<String, DefaultConnectionFuture<?>> invokers = new ConcurrentHashMap<>();

    /** A map of subscribers. ChannelName -> List of listeners. */
    final ConcurrentHashMap<String, DefaultLocalServiceRegistration> localServices = new ConcurrentHashMap<>();

    private final ThreadManager threadManager;

    /**
     * Creates a new instance of this class.
     * 
     * @param network
     *            the network
     */
    public ClientServiceManager(ClientContainer container, ConnectionMessageBus connection, ThreadManager threadManager) {
        this.container = requireNonNull(container);
        this.connection = requireNonNull(connection);
        this.threadManager = requireNonNull(threadManager);
    }

    /** {@inheritDoc} */
    public <T, S extends ServiceMessage<T>> DefaultConnectionFuture<T> invokeService(MaritimeId id, S msg) {
        InvokeService is = new InvokeService(1, UUID.randomUUID().toString(), msg.getClass().getName(),
                msg.messageName(), msg);
        is.setDestination(id.toString());
        is.setSource(container.getLocalId().toString());
        final DefaultConnectionFuture<T> f = threadManager.create();
        DefaultConnectionFuture<InvokeServiceResult> fr = threadManager.create();
        invokers.put(is.getConversationId(), fr);
        fr.thenAcceptAsync(new DefaultConnectionFuture.Action<Object>() {
            @SuppressWarnings("unchecked")
            public void accept(Object ack) {
                f.complete((T) ack);
            }
        });
        connection.sendConnectionMessage(is);
        return f;
    }

    @OnMessage
    public void onInvokeService(InvokeService message) {
        String type = message.getServiceType();
        DefaultLocalServiceRegistration s = localServices.get(type);
        if (s != null) {
            s.invoke(message);
        } else {
            System.err.println("Could not find service " + type + " from " + localServices.keySet());
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @OnMessage
    public void receiveInvokeServiceAck(InvokeServiceResult m) {
        DefaultConnectionFuture f = invokers.get(m.getUuid());
        if (f != null) {
            Object o = null;
            try {
                Class<?> mt = Class.forName(m.getReplyType());
                ObjectMapper om = new ObjectMapper();
                o = om.readValue(m.getMessage(), mt);
                f.complete(o);
            } catch (Exception e) {
                e.printStackTrace();
                f.completeExceptionally(e);
            }
        } else {
            System.err.println("Could not find invoked service " + m.getUuid() + " from " + invokers.keySet());
        }
    }

    public <T, E extends ServiceMessage<T>> ServiceLocator<T, E> serviceFind(ServiceInitiationPoint<E> sip) {
        return new DefaultServiceLocator<>(threadManager, sip, this, 0);
    }

    <T, E extends ServiceMessage<T>> DefaultConnectionFuture<FindServiceResult> serviceFindOne(FindService fs) {
        return connection.sendMessage(fs);
    }

    /** {@inheritDoc} */
    public <T, E extends ServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            InvocationCallback<E, T> callback) {
        requireNonNull(sip, "ServiceInitiationPoint is null");
        requireNonNull(callback, "callback is null");
        final DefaultLocalServiceRegistration reg = new DefaultLocalServiceRegistration(connection, sip, callback);
        if (localServices.putIfAbsent(sip.getName(), reg) != null) {
            throw new IllegalArgumentException(
                    "A service of the specified type has already been registered. Can only register one at a time");
        }
        final DefaultConnectionFuture<RegisterServiceResult> f = connection.sendMessage(new RegisterService(sip
                .getName()));
        f.thenAcceptAsync(new DefaultConnectionFuture.Action<RegisterServiceResult>() {
            public void accept(RegisterServiceResult ack) {
                reg.replied.countDown();
            }
        });
        return reg;
    }
}
