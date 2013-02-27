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
import dk.dma.navnet.core.messages.s2c.connection.ConnectedMessage;
import dk.dma.navnet.core.messages.s2c.connection.WelcomeMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ClientHandler extends AbstractHandler {

    /** {@inheritDoc} */
    @Override
    public final void handleText(String msg, AbstractMessage m) {
        if (m instanceof WelcomeMessage) {
            welcome((WelcomeMessage) m);
        } else if (m instanceof ConnectedMessage) {
            connected((ConnectedMessage) m);
        } else if (m instanceof InvokeService) {
            invokeService((InvokeService) m);
        } else if (m instanceof Broadcast) {
            broadcast((Broadcast) m);
        } else {
            System.err.println("Received an unknown message " + m.toJSON());
        }
    }

    /**
     * @param m
     */
    public void broadcast(Broadcast m) {}

    /**
     * @param m
     */
    public void invokeService(InvokeService m) {}

    /**
     * @param m
     */
    public void connected(ConnectedMessage m) {}

    public void welcome(WelcomeMessage m) {}

}
