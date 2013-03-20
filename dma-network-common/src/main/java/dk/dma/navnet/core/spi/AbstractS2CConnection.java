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

import java.util.concurrent.ScheduledExecutorService;

import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
import dk.dma.navnet.core.messages.auxiliary.PositionReportMessage;
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public class AbstractS2CConnection extends AbstractConnection {

    /**
     * @param ses
     */
    protected AbstractS2CConnection(ScheduledExecutorService ses) {
        super(ses);
    }

    public void findService(FindService m) {}

    /** {@inheritDoc} */
    @Override
    public final void handleTextReply(AbstractTextMessage m, NetworkFutureImpl<?> f) {
        unknownMessage(m);
    }

    /** {@inheritDoc} */
    @Override
    public final void handleText(AbstractTextMessage m) {
        if (m instanceof HelloMessage) {
            hello((HelloMessage) m);
        } else if (m instanceof RegisterService) {
            registerService((RegisterService) m);
        } else if (m instanceof FindService) {
            findService((FindService) m);
        } else if (m instanceof AbstractRelayedMessage) {
            relay((AbstractRelayedMessage) m);
        } else if (m instanceof BroadcastMsg) {
            broadcast((BroadcastMsg) m);
        } else if (m instanceof PositionReportMessage) {
            positionReport((PositionReportMessage) m);
        } else if (m instanceof FindService) {
            findServices((FindService) m);
        } else {
            unknownMessage(m);
        }
    }

    public void positionReport(PositionReportMessage m) {}

    /**
     * @param m
     */
    public void broadcast(BroadcastMsg m) {}

    public void hello(HelloMessage m) {}

    public void relay(AbstractRelayedMessage m) {}

    public void registerService(RegisterService m) {}

    public void findServices(FindService s) {}

    public void unknownMessage(AbstractTextMessage m) {
        System.err.println("Received an unknown message " + m.getReceivedRawMesage());
    };
}
