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

import java.util.ArrayList;
import java.util.List;

import jsr166e.CompletableFuture;
import dk.dma.enav.communication.NetworkFuture;
import dk.dma.enav.communication.service.ServiceEndpoint;
import dk.dma.enav.communication.service.ServiceLocator;
import dk.dma.enav.communication.service.spi.ServiceInitiationPoint;
import dk.dma.enav.communication.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
class ServiceLocatorImpl<T, E extends ServiceMessage<T>> implements ServiceLocator<T, E> {

    final int distance;

    final ServiceManager csm;
    final ServiceInitiationPoint<E> sip;

    ServiceLocatorImpl(ServiceInitiationPoint<E> sip, ServiceManager csm, int distance) {
        this.csm = requireNonNull(csm);
        this.distance = distance;
        this.sip = requireNonNull(sip);
    }

    /** {@inheritDoc} */
    @Override
    public ServiceLocator<T, E> withinDistanceOf(int meters) {
        if (meters <= 0 || meters >= 99999000) {
            throw new IllegalArgumentException("Meters must be greater >0 and <100000000");
        }
        return new ServiceLocatorImpl<>(sip, csm, meters);
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFuture<ServiceEndpoint<E, T>> nearest() {
        NetworkFutureImpl<FindServiceResult> f = csm.serviceFindOne(new FindService(sip.getName(), distance, 1));
        final NetworkFutureImpl<ServiceEndpoint<E, T>> result = new NetworkFutureImpl<>();
        f.thenAcceptAsync(new CompletableFuture.Action<FindServiceResult>() {
            @Override
            public void accept(FindServiceResult ack) {
                String[] st = ack.getMax();
                if (st.length > 0) {
                    result.complete(new SI(MaritimeId.create(st[0]), sip));
                } else {
                    result.complete(null);
                    // result.completeExceptionally(new ServiceNotFoundException(""));
                }
            }
        });
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFuture<List<ServiceEndpoint<E, T>>> nearest(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("The specified limit must be positive (>=1), was " + limit);
        }
        NetworkFutureImpl<FindServiceResult> f = csm.serviceFindOne(new FindService(sip.getName(), distance, limit));
        final NetworkFutureImpl<List<ServiceEndpoint<E, T>>> result = new NetworkFutureImpl<>();
        f.thenAcceptAsync(new CompletableFuture.Action<FindServiceResult>() {
            @Override
            public void accept(FindServiceResult ack) {
                String[] st = ack.getMax();
                List<ServiceEndpoint<E, T>> l = new ArrayList<>();
                if (st.length > 0) {
                    for (String s : st) {
                        l.add(new SI(MaritimeId.create(s), sip));
                    }
                }
                result.complete(l);
            }
        });
        return result;
    }

    class SI implements ServiceEndpoint<E, T> {
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
            return csm.invokeService(id, (ServiceMessage) message);
        }
    }
}
