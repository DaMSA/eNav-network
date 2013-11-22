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
package dk.dma.navnet.client.service;

import static java.util.Objects.requireNonNull;
import dk.dma.enav.maritimecloud.ConnectionFuture;
import dk.dma.enav.maritimecloud.service.ServiceEndpoint;
import dk.dma.enav.maritimecloud.service.spi.ServiceInitiationPoint;
import dk.dma.enav.maritimecloud.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;

/**
 * 
 * @author Kasper Nielsen
 */
public class DefaultRemoteServiceEndpoint<T, E extends ServiceMessage<T>> implements ServiceEndpoint<E, T> {
    final MaritimeId id;

    final ServiceInitiationPoint<E> sip;

    final ClientServiceManager csm;

    DefaultRemoteServiceEndpoint(ClientServiceManager csm, MaritimeId id, ServiceInitiationPoint<E> sip) {
        this.csm = requireNonNull(csm);
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
    public ConnectionFuture<T> invoke(E message) {
        return csm.invokeService(id, (ServiceMessage) message);
    }

}
