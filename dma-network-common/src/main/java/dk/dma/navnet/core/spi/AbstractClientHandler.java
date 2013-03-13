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
package dk.dma.navnet.core.spi;

import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractClientHandler extends AbstractHandler {

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public final void handleTextReply(AbstractTextMessage m, NetworkFutureImpl<?> f) {
        if (m instanceof RegisterServiceResult) {
            serviceRegisteredAck((RegisterServiceResult) m, (NetworkFutureImpl<RegisterServiceResult>) f);
        } else if (m instanceof FindServiceResult) {
            serviceFindAck((FindServiceResult) m, (NetworkFutureImpl<FindServiceResult>) f);
        } else {
            unknownMessage(m);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void handleText(AbstractTextMessage m) {
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

    /**
     * @param m
     */
    protected void invokeServiceAck(InvokeServiceResult m) {}

    /**
     * @param m
     */
    protected void receivedBroadcast(BroadcastMsg m) {}

    protected void serviceFindAck(FindServiceResult a, NetworkFutureImpl<FindServiceResult> f) {}

    protected void serviceRegisteredAck(RegisterServiceResult a, NetworkFutureImpl<RegisterServiceResult> f) {}

    /**
     * @param m
     */
    protected void invokeService(InvokeService m) {}

    /**
     * @param m
     */
    protected void connected(ConnectedMessage m) {}

    protected void welcome(WelcomeMessage m) {}

    protected void unknownMessage(AbstractTextMessage m) {
        System.err.println("Received an unknown message " + m.getReceivedRawMesage());
    };
}
