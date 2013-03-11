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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.ServiceNotFoundException;

import jsr166e.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.communication.NetworkFuture;
import dk.dma.enav.communication.service.InvocationCallback;
import dk.dma.enav.communication.service.InvocationCallback.FailureCode;
import dk.dma.enav.communication.service.ServiceEndpoint;
import dk.dma.enav.communication.service.ServiceInitiationPoint;
import dk.dma.enav.communication.service.ServiceRegistration;
import dk.dma.enav.communication.service.spi.MaritimeServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.core.messages.c2c.InvokeService;
import dk.dma.navnet.core.messages.c2c.InvokeServiceAck;
import dk.dma.navnet.core.messages.s2c.service.FindServices;
import dk.dma.navnet.core.messages.s2c.service.FindServicesAck;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceAck;
import dk.dma.navnet.core.util.JSonUtil;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
class ClientServiceManager {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ClientServiceManager.class);

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
    ClientServiceManager(ClientNetwork clientNetwork) {
        this.c = requireNonNull(clientNetwork);
    }

    /** {@inheritDoc} */
    NetworkFuture<Map<MaritimeId, String>> findServices(final String serviceType) {
        // return NetworkFutureImpl.wrap(c.connection.sendMessage(new FindServices(serviceType)).thenApply(
        // new CompletableFuture.Fun<String[], Map<MaritimeId, String>>() {
        // @Override
        // public Map<MaritimeId, String> apply(String[] s) {
        // HashMap<MaritimeId, String> m = new HashMap<>();
        // for (String str : s) {
        // m.put(MaritimeId.create(str), serviceType);
        // }
        // return m;
        // }
        // }));
        return null;
    }

    <T, E extends MaritimeServiceMessage<T>> NetworkFuture<ServiceEndpoint<E, T>> serviceFindOne(
            final ServiceInitiationPoint<E> sip) {
        final NetworkFutureImpl<FindServicesAck> f = c.connection.sendMessage(new FindServices(sip.getName(), 1));

        final NetworkFutureImpl<ServiceEndpoint<E, T>> result = new NetworkFutureImpl<>();

        f.thenAcceptAsync(new CompletableFuture.Action<FindServicesAck>() {
            @Override
            public void accept(FindServicesAck ack) {
                String[] st = ack.getMax();
                if (st.length > 0) {
                    result.complete(new SI<E, T>(MaritimeId.create(st[0]), sip));
                } else {
                    result.completeExceptionally(new ServiceNotFoundException(""));
                }
            }
        });

        return result;
    }

    /** {@inheritDoc} */
    <T, S extends MaritimeServiceMessage<T>> NetworkFutureImpl<T> invokeService(MaritimeId id, S msg) {
        InvokeService is = new InvokeService(1, UUID.randomUUID().toString(), msg.getClass().getName(),
                msg.messageName(), JSonUtil.persistAndEscape(msg));
        is.setDestination(id.toString());
        is.setSource(c.clientId.toString());
        final NetworkFutureImpl<T> f = new NetworkFutureImpl<>();
        NetworkFutureImpl<InvokeServiceAck> fr = new NetworkFutureImpl<>();
        invokers.put(is.getConversationId(), fr);
        fr.thenAcceptAsync(new CompletableFuture.Action<Object>() {
            @Override
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
                public void fail(FailureCode fc, String message) {}
            });
        } else {
            System.err.println("Could not find service " + m.getServiceType() + " from " + listeners.keySet());
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void receiveInvokeServiceAck(InvokeServiceAck m) {
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
    public <T, E extends MaritimeServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            InvocationCallback<E, T> callback) {
        final Registration reg = new Registration(sip, callback);
        if (listeners.putIfAbsent(sip.getName(), reg) != null) {
            throw new IllegalArgumentException(
                    "A service of the specified type has already been registered. Can only register one at a time");
        }
        final NetworkFutureImpl<RegisterServiceAck> f = c.connection.sendMessage(new RegisterService(sip.getName()));
        f.thenAcceptAsync(new CompletableFuture.Action<RegisterServiceAck>() {
            @Override
            public void accept(RegisterServiceAck ack) {
                reg.replied.countDown();
            }
        });
        return reg;
    }

    class SI<E, T> implements ServiceEndpoint<E, T> {
        final MaritimeId id;
        final ServiceInitiationPoint<E> sip;

        SI(MaritimeId id, ServiceInitiationPoint<E> sip) {
            this.id = requireNonNull(id);
            this.sip = requireNonNull(sip);
        }

        /** {@inheritDoc} */
        @Override
        public MaritimeId getId() {
            return id;
        }

        /** {@inheritDoc} */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public NetworkFuture<T> invoke(E message) {
            return invokeService(id, (MaritimeServiceMessage) message);
        }
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
