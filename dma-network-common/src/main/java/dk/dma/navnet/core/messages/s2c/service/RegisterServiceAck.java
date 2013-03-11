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
package dk.dma.navnet.core.messages.s2c.service;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.s2c.AckMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class RegisterServiceAck extends AckMessage {

    // Area
    public RegisterServiceAck(ProtocolReader pr) throws IOException {
        super(MessageType.REGISTER_SERVICE_ACK, pr);
    }

    /**
     * @param messageType
     */
    public RegisterServiceAck(long id) {
        super(MessageType.REGISTER_SERVICE_ACK, id);
    }
}
