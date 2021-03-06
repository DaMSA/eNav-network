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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dk.dma.enav.maritimecloud.service.invocation.InvocationCallback;
import dk.dma.enav.maritimecloud.service.registration.ServiceRegistration;
import dk.dma.enav.maritimecloud.service.spi.ServiceInitiationPoint;
import dk.dma.navnet.client.connection.ConnectionMessageBus;
import dk.dma.navnet.messages.c2c.service.InvokeService;

/**
 * 
 * @author Kasper Nielsen
 */
public class DefaultLocalServiceRegistration implements ServiceRegistration {

    final InvocationCallback<Object, Object> c;

    final CountDownLatch replied = new CountDownLatch(1);

    final ServiceInitiationPoint<?> sip;

    final ConnectionMessageBus bus;

    @SuppressWarnings("unchecked")
    DefaultLocalServiceRegistration(ConnectionMessageBus bus, ServiceInitiationPoint<?> sip, InvocationCallback<?, ?> c) {
        this.sip = requireNonNull(sip);
        this.c = requireNonNull((InvocationCallback<Object, Object>) c);
        this.bus = requireNonNull(bus);
    }

    void invoke(InvokeService message) {
        Object o = null;
        try {
            o = message.parseMessage();
        } catch (Exception e) {
            // LOG error
            // Send invalid message
            e.printStackTrace();
        }


        DefaultLocalServiceInvocationContext<Object> context = new DefaultLocalServiceInvocationContext<>(
                message.getSourceId());
        c.process(o, context);

        if (context.done) {
            if (context.errorCode == 0) {
                bus.sendConnectionMessage(message.createReply(context.message));
            } else {
                // send fail message
            }
        } else {
            // Log error
            // send failed internal error message
        }
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
