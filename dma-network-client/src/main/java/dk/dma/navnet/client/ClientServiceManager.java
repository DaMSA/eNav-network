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

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.net.NetworkFuture;
import dk.dma.enav.net.ServiceCallback;
import dk.dma.enav.net.ServiceRegistration;
import dk.dma.enav.service.spi.InitiatingMessage;
import dk.dma.enav.service.spi.MaritimeService;
import dk.dma.enav.service.spi.MaritimeServiceMessage;
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
    final ConcurrentHashMap<String, InternalServiceCallbackRegistration> registeredServices = new ConcurrentHashMap<>();
    final ClientNetwork c;

    /**
     * @param clientNetwork
     */
    public ClientServiceManager(ClientNetwork clientNetwork) {
        this.c = requireNonNull(clientNetwork);
    }

    /** {@inheritDoc} */
    public void receiveInvokeService(InvokeService m) {
        InternalServiceCallbackRegistration s = registeredServices.get(m.getServiceType());
        if (s != null) {
            ServiceCallback<Object, Object> sc = s.c;
            Object o = null;
            try {
                Class<?> mt = Class.forName(s.type.getName() + "$" + m.getServiceMessageType());
                ObjectMapper om = new ObjectMapper();
                o = om.readValue(m.getMessage(), mt);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sc.process(o, new ServiceCallback.Context<Object>() {
                public void complete(Object result) {
                    requireNonNull(result);
                    System.out.println("Completed");
                    // con.packetWrite(p.replyWith(result));
                }

                public void fail(Throwable cause) {
                    requireNonNull(cause);
                    System.out.println(cause);
                    // con.packetWrite(p.replyWithFailure(cause));
                }
            });
        }
    }

    /** {@inheritDoc} */
    public <T, S extends MaritimeServiceMessage<T> & InitiatingMessage> NetworkFutureImpl<T> invokeService(
            MaritimeId id, S msg) {
        InvokeService is = new InvokeService(1, UUID.randomUUID().toString(), msg.serviceName(), msg.messageName(),
                JSonUtil.persistAndEscape(msg));
        is.setDestination(id.toString());
        is.setSource(c.clientId.toString());
        return c.connection.sendMessage(is);
    }

    /** {@inheritDoc} */
    public NetworkFuture<Map<MaritimeId, String>> findServices(final String serviceType) {
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
    public <T extends MaritimeServiceMessage<?>, S extends MaritimeService, E extends MaritimeServiceMessage<T> & InitiatingMessage> ServiceRegistration registerService(
            S service, ServiceCallback<E, T> b) {
        if (registeredServices.putIfAbsent(service.getName(),
                new InternalServiceCallbackRegistration(service.getClass(), b)) != null) {
            throw new IllegalArgumentException("A service of the specified type has already been registered");
        }
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
        };
    }

    class InternalServiceCallbackRegistration {
        final Class<? extends MaritimeService> type;

        final ServiceCallback<Object, Object> c;

        @SuppressWarnings("unchecked")
        InternalServiceCallbackRegistration(Class<? extends MaritimeService> type, ServiceCallback<?, ?> c) {
            this.type = requireNonNull(type);
            this.c = (ServiceCallback<Object, Object>) requireNonNull(c);
        }

    }

}
