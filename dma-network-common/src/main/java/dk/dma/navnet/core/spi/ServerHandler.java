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
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.c2c.Broadcast;
import dk.dma.navnet.core.messages.s2c.FindServices;
import dk.dma.navnet.core.messages.s2c.PositionReportMessage;
import dk.dma.navnet.core.messages.s2c.RegisterService;
import dk.dma.navnet.core.messages.s2c.connection.HelloMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ServerHandler extends AbstractHandler {

    public void findService(FindServices m) {}

    /** {@inheritDoc} */
    @Override
    public final void handleText(String msg, AbstractMessage m) {
        if (m instanceof HelloMessage) {
            hello((HelloMessage) m);
        } else if (m instanceof RegisterService) {
            registerService((RegisterService) m);
        } else if (m instanceof FindServices) {
            findService((FindServices) m);
        } else if (m instanceof AbstractRelayedMessage) {
            relay((AbstractRelayedMessage) m);
        } else if (m instanceof Broadcast) {
            broadcast(msg, (Broadcast) m);
        } else if (m instanceof PositionReportMessage) {
            positionReport((PositionReportMessage) m);
        } else {
            unknownMessage(msg, m);
        }
    }

    public void positionReport(PositionReportMessage m) {}

    /**
     * @param m
     */
    public void broadcast(String msg, Broadcast m) {}

    public void hello(HelloMessage m) {}

    public void relay(AbstractRelayedMessage m) {}

    public void registerService(RegisterService m) {}

    public void unknownMessage(String msg, AbstractMessage m) {
        System.err.println("Received an unknown message " + m.toJSON());
    };
}
