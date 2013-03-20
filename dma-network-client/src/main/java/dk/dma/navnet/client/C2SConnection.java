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
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;
import dk.dma.navnet.core.spi.AbstractC2SConnection;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
class C2SConnection extends AbstractC2SConnection {

    final ClientNetwork cm;

    final ClientTransport ch;

    C2SConnection(ClientNetwork cn, ClientTransport ch) {
        super(cn.ses);
        this.cm = requireNonNull(cn);
        this.ch = requireNonNull(ch);
    }

    /** {@inheritDoc} */
    @Override
    protected void connected(ConnectedMessage m) {
        ch.connected.countDown();
    }

    /** {@inheritDoc} */
    @Override
    protected void invokeService(InvokeService m) {
        cm.services.receiveInvokeService(m);
    }

    /** {@inheritDoc} */
    @Override
    protected void invokeServiceAck(InvokeServiceResult m) {
        cm.services.receiveInvokeServiceAck(m);
    }

    /** {@inheritDoc} */
    @Override
    protected void receivedBroadcast(BroadcastMsg m) {
        cm.broadcaster.receive(m);

    }

    /** {@inheritDoc} */
    @Override
    protected void serviceFindAck(FindServiceResult a, NetworkFutureImpl<FindServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    @Override
    protected void serviceRegisteredAck(RegisterServiceResult a, NetworkFutureImpl<RegisterServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    @Override
    protected void welcome(WelcomeMessage m) {
        PositionTime pt = cm.positionManager.getPositionTime();
        ch.sendMessage(new HelloMessage(cm.clientId, "enavClient/1.0", "", 2, pt.getLatitude(), pt.getLongitude()));
    }

}
