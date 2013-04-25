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
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;
import dk.dma.navnet.core.spi.AbstractConnection;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
class ClientConnection extends AbstractConnection {

    final DefaultPersistentConnection cm;

    private volatile ClientTransport ch;

    volatile String connectionId;

    ClientConnection(DefaultPersistentConnection cn) {
        super(cn.cfs);
        this.cm = requireNonNull(cn);
        this.ch = new ClientTransport();
        super.setTransport(ch);
    }

    /** {@inheritDoc} */
    protected final void handleMessage(AbstractTextMessage m) {
        if (m instanceof WelcomeMessage) {
            welcome((WelcomeMessage) m);
        } else if (m instanceof ConnectedMessage) {
            connected((ConnectedMessage) m);
        } else if (m instanceof InvokeService) {
            invokeService((InvokeService) m);
        } else if (m instanceof InvokeServiceResult) {
            invokeServiceAck((InvokeServiceResult) m);
        } else if (m instanceof BroadcastMsg) {
            receivedBroadcast((BroadcastMsg) m);
        } else {
            unknownMessage(m);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    protected final void handleMessageReply(AbstractTextMessage m, NetworkFutureImpl<?> f) {
        if (m instanceof RegisterServiceResult) {
            serviceRegisteredAck((RegisterServiceResult) m, (NetworkFutureImpl<RegisterServiceResult>) f);
        } else if (m instanceof FindServiceResult) {
            serviceFindAck((FindServiceResult) m, (NetworkFutureImpl<FindServiceResult>) f);
        } else {
            unknownMessage(m);
        }
    }

    /** {@inheritDoc} */
    protected void connected(ConnectedMessage m) {
        connectionId = m.getConnectionId();
        ch.connected.countDown();
    }

    /** {@inheritDoc} */
    protected void invokeService(InvokeService m) {
        cm.services.receiveInvokeService(m);
    }

    /** {@inheritDoc} */
    protected void invokeServiceAck(InvokeServiceResult m) {
        cm.services.receiveInvokeServiceAck(m);
    }

    /** {@inheritDoc} */
    protected void receivedBroadcast(BroadcastMsg m) {
        cm.broadcaster.receive(m);
    }

    /** {@inheritDoc} */
    protected void serviceFindAck(FindServiceResult a, NetworkFutureImpl<FindServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    protected void serviceRegisteredAck(RegisterServiceResult a, NetworkFutureImpl<RegisterServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    protected void welcome(WelcomeMessage m) {
        PositionTime pt = cm.positionManager.getPositionTime();
        ch.sendMessage(new HelloMessage(cm.getLocalId(), "enavClient/1.0", "", 2, pt.getLatitude(), pt.getLongitude()));
    }

    public void connect(long timeout, TimeUnit unit) throws IOException {
        try {
            cm.transportFactory.connect(ch, timeout, unit);
            ch.connected.await(timeout, unit);
            if (ch.connected.getCount() > 0) {
                throw new ConnectException("Timedout while connecting to ");
            }

        } catch (IOException e) {
            cm.es.shutdown();
            cm.ses.shutdown();
            cm.transportFactory.shutdown();
            throw e;
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }
}
