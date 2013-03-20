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

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dk.dma.navnet.core.spi.AbstractC2SConnection;
import dk.dma.navnet.core.spi.AbstractMessageTransport;

/**
 * 
 * @author Kasper Nielsen
 */
class ClientTransport extends AbstractMessageTransport {

    /** The actual websocket client. Changes when reconnecting. */
    final ClientNetwork cm;

    final CountDownLatch connected = new CountDownLatch(1);

    ClientTransport(ClientNetwork cm) {
        super(cm.ses);
        this.cm = requireNonNull(cm);
    }

    public void connect(long timeout, TimeUnit unit) throws Exception {
        try {
            cm.transportFactory.connect(this, timeout, unit);
            connected.await(timeout, unit);
            if (connected.getCount() > 0) {
                throw new ConnectException("Timedout while connecting to ");
            }
        } catch (IOException e) {
            cm.es.shutdown();
            cm.ses.shutdown();
            cm.transportFactory.shutdown();
            throw (Exception) e.getCause();// todo fix throw
        }
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractC2SConnection client() {
        return cm.connection;
    }
}
