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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jsr166e.CompletableFuture;

import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.communication.service.InvocationCallback;
import dk.dma.enav.communication.service.ServiceLocator;
import dk.dma.enav.communication.service.ServiceRegistration;
import dk.dma.enav.communication.service.spi.ServiceInitiationPoint;
import dk.dma.enav.communication.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.client.InternalClient;
import dk.dma.navnet.client.connection.ConnectionMessageBus;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.c2c.service.InvokeService;
import dk.dma.navnet.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.messages.s2c.service.FindService;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.messages.s2c.service.RegisterService;
import dk.dma.navnet.messages.s2c.service.RegisterServiceResult;

/**
 * 
 * @author Kasper Nielsen
 */
public class ClientServiceManager implements Startable {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ClientServiceManager.class);

    /** The network */
    final InternalClient clientInfo;

    final ConnectionMessageBus connection;

    final ConcurrentHashMap<String, DefaultConnectionFuture<?>> invokers = new ConcurrentHashMap<>();

    /** A map of subscribers. ChannelName -> List of listeners. */
    final ConcurrentHashMap<String, Registration> serviceRegistrations = new ConcurrentHashMap<>();

    final ThreadManager threadManager;

    /**
     * Creates a new instance of this class.
     * 
     * @param network
     *            the network
     */
    public ClientServiceManager(ConnectionMessageBus connection, ThreadManager threadManager, InternalClient clientInfo) {
        this.clientInfo = clientInfo;
        this.connection = requireNonNull(connection);
        this.threadManager = threadManager;
    }

    /** {@inheritDoc} */
    public <T, S extends ServiceMessage<T>> DefaultConnectionFuture<T> invokeService(MaritimeId id, S msg) {
        InvokeService is = new InvokeService(1, UUID.randomUUID().toString(), msg.getClass().getName(),
                msg.messageName(), msg);
        is.setDestination(id.toString());
        is.setSource(clientInfo.getLocalId().toString());
        final DefaultConnectionFuture<T> f = threadManager.create();
        DefaultConnectionFuture<InvokeServiceResult> fr = threadManager.create();
        invokers.put(is.getConversationId(), fr);
        fr.thenAcceptAsync(new CompletableFuture.Action<Object>() {
            @SuppressWarnings("unchecked")
            public void accept(Object ack) {
                f.complete((T) ack);
            }
        });
        connection.sendConnectionMessage(is);
        return f;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked" })
    void receiveInvokeService(final InvokeService m) {
        // System.out.println("Invoking service");
        // System.out.println("FFF " + m);
        Registration s = serviceRegistrations.get(m.getServiceType());
        if (s != null) {
            InvocationCallback<Object, Object> sc = (InvocationCallback<Object, Object>) s.c;
            Object o = null;
            try {
                Class<?> mt = Class.forName(m.getServiceType());
                ObjectMapper om = new ObjectMapper();
                o = om.readValue(m.getMessage(), mt);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sc.process(o, new InvocationCallback.Context<Object>() {
                public void complete(Object result) {
                    requireNonNull(result);
                    // System.out.println("Completed");
                    connection.sendConnectionMessage(m.createReply(result));
                }

                public void failWithIllegalAccess(String message) {
                    throw new UnsupportedOperationException();
                }

                public void failWithIllegalInput(String message) {
                    throw new UnsupportedOperationException();
                }

                public void failWithInternalError(String message) {
                    throw new UnsupportedOperationException();
                }

                public MaritimeId getCaller() {
                    return null;
                }

            });
        } else {
            System.err.println("Could not find service " + m.getServiceType() + " from "
                    + serviceRegistrations.keySet());
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void receiveInvokeServiceAck(InvokeServiceResult m) {
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
        return new ServiceLocatorImpl<>(threadManager, sip, this, 0);
    }

    <T, E extends ServiceMessage<T>> DefaultConnectionFuture<FindServiceResult> serviceFindOne(FindService fs) {
        return connection.sendMessage(fs);
    }

    /** {@inheritDoc} */
    public <T, E extends ServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            InvocationCallback<E, T> callback) {
        requireNonNull(sip, "ServiceInitiationPoint is null");
        requireNonNull(callback, "callback is null");
        final Registration reg = new Registration(sip, callback);
        if (serviceRegistrations.putIfAbsent(sip.getName(), reg) != null) {
            throw new IllegalArgumentException(
                    "A service of the specified type has already been registered. Can only register one at a time");
        }
        final DefaultConnectionFuture<RegisterServiceResult> f = connection.sendMessage(new RegisterService(sip
                .getName()));
        f.thenAcceptAsync(new CompletableFuture.Action<RegisterServiceResult>() {
            public void accept(RegisterServiceResult ack) {
                reg.replied.countDown();
            }
        });
        return reg;
    }

    class Registration implements ServiceRegistration {
        final InvocationCallback<?, ?> c;
        final CountDownLatch replied = new CountDownLatch(1);

        final ServiceInitiationPoint<?> sip;

        Registration(ServiceInitiationPoint<?> sip, InvocationCallback<?, ?> c) {
            this.sip = requireNonNull(sip);
            this.c = requireNonNull(c);
        }

        /** {@inheritDoc} */
        @Override
        public boolean awaitRegistered(long timeout, TimeUnit unit) throws InterruptedException {
            return replied.await(timeout, unit);
        }

        /** {@inheritDoc} */
        @Override
        public void cancel() {
            throw new UnsupportedOperationException();
        }

        void completed() {

        }

        /** {@inheritDoc} */
        @Override
        public State getState() {
            throw new UnsupportedOperationException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        connection.subscribe(InvokeService.class, new Consumer<InvokeService>() {
            @Override
            public void accept(InvokeService t) {
                receiveInvokeService(t);
            }
        });
        connection.subscribe(InvokeServiceResult.class, new Consumer<InvokeServiceResult>() {
            @Override
            public void accept(InvokeServiceResult t) {
                receiveInvokeServiceAck(t);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {}

}
