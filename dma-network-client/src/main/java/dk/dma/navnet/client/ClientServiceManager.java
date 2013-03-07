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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jsr166e.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.communication.NetworkFuture;
import dk.dma.enav.communication.service.ServiceInitiationPoint;
import dk.dma.enav.communication.service.ServiceInvocationCallback;
import dk.dma.enav.communication.service.ServiceInvocationCallback.FailureCode;
import dk.dma.enav.communication.service.ServiceRegistration;
import dk.dma.enav.communication.service.spi.InitiatingMessage;
import dk.dma.enav.communication.service.spi.MaritimeService;
import dk.dma.enav.communication.service.spi.MaritimeServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.core.messages.c2c.InvokeService;
import dk.dma.navnet.core.messages.s2c.FindServices;
import dk.dma.navnet.core.messages.s2c.RegisterService;
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
        return NetworkFutureImpl.wrap(c.connection.sendMessage(new FindServices(serviceType)).thenApply(
                new CompletableFuture.Fun<String[], Map<MaritimeId, String>>() {
                    @Override
                    public Map<MaritimeId, String> apply(String[] s) {
                        HashMap<MaritimeId, String> m = new HashMap<>();
                        for (String str : s) {
                            m.put(MaritimeId.create(str), serviceType);
                        }
                        return m;
                    }
                }));
    }

    /** {@inheritDoc} */
    <T, S extends MaritimeServiceMessage<T> & InitiatingMessage> NetworkFutureImpl<T> invokeService(MaritimeId id, S msg) {
        InvokeService is = new InvokeService(1, UUID.randomUUID().toString(), msg.serviceName(), msg.messageName(),
                JSonUtil.persistAndEscape(msg));
        is.setDestination(id.toString());
        is.setSource(c.clientId.toString());
        return c.connection.sendMessage(is);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("null")
    void receiveInvokeService(InvokeService m) {
        Registration s = listeners.get(m.getServiceType());
        if (s != null) {
            ServiceInvocationCallback<Object, Object> sc = null;// s.c;
            Object o = null;
            try {
                Class<?> mt = null;// Class.forName(s.type.getName() + "$" + m.getServiceMessageType());
                ObjectMapper om = new ObjectMapper();
                o = om.readValue(m.getMessage(), mt);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sc.process(o, new ServiceInvocationCallback.Context<Object>() {
                public void complete(Object result) {
                    requireNonNull(result);
                    System.out.println("Completed");
                    // con.packetWrite(p.replyWith(result));
                }

                @Override
                public void fail(FailureCode fc, String message) {}
            });
        }
    }

    /** {@inheritDoc} */
    <T extends MaritimeServiceMessage<?>, S extends MaritimeService, E extends MaritimeServiceMessage<T> & InitiatingMessage> ServiceRegistration serviceRegister(
            S service, ServiceInvocationCallback<E, T> b) {
        // if (listeners.putIfAbsent(service.getName(), new Registration(service.getClass(), b)) != null) {
        // throw new IllegalArgumentException(
        // "A service of the specified type has already been registered. Can only register one at a time");
        // }
        final NetworkFutureImpl<Void> stp = c.connection.sendMessage(new RegisterService(service.getName()));
        // final NetworkFutureImpl<Void> stp = null;// connection.withReply(null, Packet.REGISTER_SERVICE, service);
        return new ServiceRegistration() {

            @Override
            public boolean awaitRegistered(long timeout, TimeUnit unit) {
                try {
                    stp.get(timeout, unit);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }

            @Override
            public void cancel() {
                throw new UnsupportedOperationException();
            }

            @Override
            public State getState() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** {@inheritDoc} */
    public <T, E extends MaritimeServiceMessage<T>> ServiceRegistration serviceRegister(ServiceInitiationPoint<E> sip,
            ServiceInvocationCallback<E, T> callback) {
        if (listeners.putIfAbsent(sip.getName(), new Registration(sip, callback)) != null) {
            throw new IllegalArgumentException(
                    "A service of the specified type has already been registered. Can only register one at a time");
        }
        // final NetworkFutureImpl<Void> stp = c.connection.sendMessage(new RegisterService(service.getName()));
        return null;
    }

    class Registration {
        final ServiceInvocationCallback<?, ?> c;

        final ServiceInitiationPoint<?> sip;

        Registration(ServiceInitiationPoint<?> sip, ServiceInvocationCallback<?, ?> c) {
            this.sip = requireNonNull(sip);
            this.c = requireNonNull(c);
        }
    }

}
