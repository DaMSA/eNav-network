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
package dk.dma.navnet.core.messages.s2c;

import java.io.IOException;

import dk.dma.navnet.core.messages.AbstractMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.ProtocolWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class AckMessage extends AbstractMessage {

    final String message;

    final long messageAck;

    final int statusCode;

    /**
     * @param messageType
     */
    public AckMessage(long messageAck, int operationID, String message) {
        super(MessageType.ACK);
        this.messageAck = messageAck;
        this.statusCode = operationID;
        this.message = message;
    }

    public AckMessage(ProtocolReader pr) throws IOException {
        this(pr.takeLong(), pr.takeInt(), pr.takeString());
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    public long getMessageAck() {
        return messageAck;
    }

    /**
     * @return the operationID
     */
    public int getStatusCode() {
        return statusCode;
    }

    /** {@inheritDoc} */
    @Override
    protected void write(ProtocolWriter w) {
        w.writeLong(messageAck);
        w.writeInt(statusCode);
        w.writeString(message);
    }
}
