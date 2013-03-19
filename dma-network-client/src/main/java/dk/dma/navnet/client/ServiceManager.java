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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jsr166e.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.communication.service.InvocationCallback;
import dk.dma.enav.communication.service.ServiceLocator;
import dk.dma.enav.communication.service.ServiceRegistration;
import dk.dma.enav.communication.service.spi.ServiceInitiationPoint;
import dk.dma.enav.communication.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;
import dk.dma.navnet.core.util.JSonUtil;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
class ServiceManager {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);

    /** The network */
    final ClientNetwork c;

    /** A map of subscribers. ChannelName -> List of listeners. */
    final ConcurrentHashMap<String, Registration> listeners = new ConcurrentHashMap<>();

    final ConcurrentHashMap<String, NetworkFutureImpl<?>> invokers = new ConcurrentHashMap<>();

    /**
     * Creates a new instance of this class.
     * 
     * @param network
     *            the network
     */
    ServiceManager(ClientNetwork clientNetwork) {
        this.c = requireNonNull(clientNetwork);
    }

    public <T, E extends ServiceMessage<T>> ServiceLocator<T, E> serviceFind(ServiceInitiationPoint<E> sip) {
        return new ServiceLocatorImpl<>(sip, this, 0);
    }

    <T, E extends ServiceMessage<T>> NetworkFutureImpl<FindServiceResult> serviceFindOne(FindService fs) {
        return c.connection.sendMessage(fs);
    }

    /** {@inheritDoc} */
    <T, S extends ServiceMessage<T>> NetworkFutureImpl<T> invokeService(MaritimeId id, S msg) {
        InvokeService is = new InvokeService(1, UUID.randomUUID().toString(), msg.getClass().getName(),
                msg.messageName(), JSonUtil.persistAndEscape(msg));
        is.setDestination(id.toString());
        is.setSource(c.clientId.toString());
        final NetworkFutureImpl<T> f = new NetworkFutureImpl<>(c.ses);
        NetworkFutureImpl<InvokeServiceResult> fr = new NetworkFutureImpl<>(c.ses);
        invokers.put(is.getConversationId(), fr);
        fr.thenAcceptAsync(new CompletableFuture.Action<Object>() {
            @SuppressWarnings("unchecked")
            public void accept(Object ack) {
                f.complete((T) ack);
            }
        });
        c.connection.sendMessage(is);
        return f;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked" })
    void receiveInvokeService(final InvokeService m) {
        // System.out.println("Invoking service");
        // System.out.println("FFF " + m);
        Registration s = listeners.get(m.getServiceType());
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
                    c.connection.sendMessage(m.createReply(result));
                }

                @Override
                public MaritimeId getCaller() {
                    return null;
                }

                @Override
                public void failWithIllegalAccess(String message) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void failWithIllegalInput(String message) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void failWithInternalError(String message) {
                    throw new UnsupportedOperationException();
                }

            });
        } else {
            System.err.println("Could not find service " + m.getServiceType() + " from " + listeners.keySet());
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void receiveInvokeServiceAck(InvokeServiceResult m) {
        NetworkFutureImpl f = invokers.get(m.getUuid());
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

    /** {@inheritDoc} */
    public <T, E extends ServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            InvocationCallback<E, T> callback) {
        final Registration reg = new Registration(sip, callback);
        if (listeners.putIfAbsent(sip.getName(), reg) != null) {
            throw new IllegalArgumentException(
                    "A service of the specified type has already been registered. Can only register one at a time");
        }
        final NetworkFutureImpl<RegisterServiceResult> f = c.connection.sendMessage(new RegisterService(sip.getName()));
        f.thenAcceptAsync(new CompletableFuture.Action<RegisterServiceResult>() {
            @Override
            public void accept(RegisterServiceResult ack) {
                reg.replied.countDown();
            }
        });
        return reg;
    }

    class Registration implements ServiceRegistration {
        final CountDownLatch replied = new CountDownLatch(1);
        final InvocationCallback<?, ?> c;

        final ServiceInitiationPoint<?> sip;

        Registration(ServiceInitiationPoint<?> sip, InvocationCallback<?, ?> c) {
            this.sip = requireNonNull(sip);
            this.c = requireNonNull(c);
        }

        void completed() {

        }

        /** {@inheritDoc} */
        @Override
        public State getState() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public void cancel() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public boolean awaitRegistered(long timeout, TimeUnit unit) throws InterruptedException {
            return replied.await(timeout, unit);
        }
    }

}
