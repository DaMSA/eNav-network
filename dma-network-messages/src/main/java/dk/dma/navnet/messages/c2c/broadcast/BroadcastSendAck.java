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
package dk.dma.navnet.messages.c2c.broadcast;

import java.io.IOException;

import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.s2c.ServerResponseMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class BroadcastSendAck extends ServerResponseMessage {

    /**
     * @param messageType
     * @throws IOException
     */
    public BroadcastSendAck(TextMessageReader pr) throws IOException {
        super(MessageType.BROADCAST_SEND_ACK, pr);
    }

    public BroadcastSendAck(long messageAck) {
        super(MessageType.BROADCAST_SEND_ACK, messageAck);
    }

}
