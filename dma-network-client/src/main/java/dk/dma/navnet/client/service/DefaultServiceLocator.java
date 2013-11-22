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

import java.util.ArrayList;
import java.util.List;

import jsr166e.CompletableFuture;
import dk.dma.enav.maritimecloud.ConnectionFuture;
import dk.dma.enav.maritimecloud.service.ServiceEndpoint;
import dk.dma.enav.maritimecloud.service.ServiceLocator;
import dk.dma.enav.maritimecloud.service.spi.ServiceInitiationPoint;
import dk.dma.enav.maritimecloud.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.s2c.service.FindService;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;

/**
 * The default implementation of ServiceLocator.
 * 
 * @author Kasper Nielsen
 */
class DefaultServiceLocator<T, E extends ServiceMessage<T>> implements ServiceLocator<T, E> {

    /** The distance in meters for where to look for the service. */
    final int distance;

    final ClientServiceManager csm;

    final ServiceInitiationPoint<E> sip;

    final ThreadManager threadManager;

    DefaultServiceLocator(ThreadManager threadManager, ServiceInitiationPoint<E> sip, ClientServiceManager csm,
            int distance) {
        this.csm = requireNonNull(csm);
        this.distance = distance;
        this.sip = requireNonNull(sip);
        this.threadManager = threadManager;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceLocator<T, E> withinDistanceOf(int meters) {
        if (meters <= 0 || meters >= 100_000_000) {
            throw new IllegalArgumentException("Meters must be greater >0 and <100.000.000");
        }
        return new DefaultServiceLocator<>(threadManager, sip, csm, meters);
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionFuture<ServiceEndpoint<E, T>> nearest() {
        DefaultConnectionFuture<FindServiceResult> f = csm.serviceFindOne(new FindService(sip.getName(), distance, 1));
        final DefaultConnectionFuture<ServiceEndpoint<E, T>> result = threadManager.create();
        f.thenAcceptAsync(new CompletableFuture.Action<FindServiceResult>() {
            @Override
            public void accept(FindServiceResult ack) {
                String[] st = ack.getMax();
                if (st.length > 0) {
                    result.complete(new DefaultRemoteServiceEndpoint<>(csm, MaritimeId.create(st[0]), sip));
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
    public ConnectionFuture<List<ServiceEndpoint<E, T>>> nearest(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("The specified limit must be positive (>=1), was " + limit);
        }
        DefaultConnectionFuture<FindServiceResult> f = csm.serviceFindOne(new FindService(sip.getName(), distance,
                limit));
        final DefaultConnectionFuture<List<ServiceEndpoint<E, T>>> result = threadManager.create();
        f.thenAcceptAsync(new CompletableFuture.Action<FindServiceResult>() {
            @Override
            public void accept(FindServiceResult ack) {
                String[] st = ack.getMax();
                List<ServiceEndpoint<E, T>> l = new ArrayList<>();
                if (st.length > 0) {
                    for (String s : st) {
                        l.add(new DefaultRemoteServiceEndpoint<>(csm, MaritimeId.create(s), sip));
                    }
                }
                result.complete(l);
            }
        });
        return result;
    }
}
