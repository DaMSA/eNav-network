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
package dk.dma.navnet.core.messages.c2c;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.ProtocolWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class InvokeServiceAck extends AbstractRelayedMessage {

    final String message;

    final String replyType;

    final String uuid;

    public InvokeServiceAck(ProtocolReader pr) throws IOException {
        super(MessageType.SERVICE_INVOKE_ACK, pr);
        this.uuid = requireNonNull(pr.takeString());
        this.message = requireNonNull(pr.takeString());
        this.replyType = requireNonNull(pr.takeString());
    }

    /**
     * @param messageType
     */
    public InvokeServiceAck(String uuid, String message, String replyType) {
        super(MessageType.SERVICE_INVOKE_ACK);
        this.uuid = uuid;
        this.message = message;
        this.replyType = replyType;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the replyType
     */
    public String getReplyType() {
        return replyType;
    }

    /**
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /** {@inheritDoc} */
    @Override
    protected void write0(ProtocolWriter w) {
        w.writeString(uuid);
        w.writeString(message);
        w.writeString(replyType);
    }
}
