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

import dk.dma.navnet.core.messages.AbstractMessage;
import dk.dma.navnet.core.messages.c2c.Broadcast;
import dk.dma.navnet.core.messages.c2c.InvokeService;
import dk.dma.navnet.core.messages.c2c.InvokeServiceAck;
import dk.dma.navnet.core.messages.s2c.connection.ConnectedMessage;
import dk.dma.navnet.core.messages.s2c.connection.WelcomeMessage;
import dk.dma.navnet.core.messages.s2c.service.FindServicesAck;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceAck;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ClientHandler extends AbstractHandler {
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public final void handleTextReply(String msg, AbstractMessage m, NetworkFutureImpl<?> f) {
        if (m instanceof RegisterServiceAck) {
            serviceRegisteredAck((RegisterServiceAck) m, (NetworkFutureImpl<RegisterServiceAck>) f);
        } else if (m instanceof FindServicesAck) {
            serviceFindAck((FindServicesAck) m, (NetworkFutureImpl<FindServicesAck>) f);
        } else {
            unknownMessage(msg, m);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void handleText(String msg, AbstractMessage m) {
        if (m instanceof WelcomeMessage) {
            welcome((WelcomeMessage) m);
        } else if (m instanceof ConnectedMessage) {
            connected((ConnectedMessage) m);
        } else if (m instanceof InvokeService) {
            invokeService((InvokeService) m);
        } else if (m instanceof InvokeServiceAck) {
            invokeServiceAck((InvokeServiceAck) m);
        } else if (m instanceof Broadcast) {
            receivedBroadcast((Broadcast) m);
        } else {
            unknownMessage(msg, m);
        }
    }

    /**
     * @param m
     */
    protected void invokeServiceAck(InvokeServiceAck m) {}

    /**
     * @param m
     */
    protected void receivedBroadcast(Broadcast m) {}

    protected void serviceFindAck(FindServicesAck a, NetworkFutureImpl<FindServicesAck> f) {}

    protected void serviceRegisteredAck(RegisterServiceAck a, NetworkFutureImpl<RegisterServiceAck> f) {}

    /**
     * @param m
     */
    protected void invokeService(InvokeService m) {}

    /**
     * @param m
     */
    protected void connected(ConnectedMessage m) {}

    protected void welcome(WelcomeMessage m) {}

    protected void unknownMessage(String msg, AbstractMessage m) {
        System.err.println("Received an unknown message " + m.toJSON());
    };
}
