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
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AckMessage extends AbstractMessage {

    final long messageAck;

    /**
     * @param messageType
     */
    public AckMessage(MessageType type, long messageAck) {
        super(type);
        this.messageAck = messageAck;
    }

    public AckMessage(MessageType type, TextMessageReader pr) throws IOException {
        this(type, pr.takeLong());
    }

    public long getMessageAck() {
        return messageAck;
    }

    /** {@inheritDoc} */
    @Override
    protected final void write(TextMessageWriter w) {
        w.writeLong(messageAck);
        write0(w);
    }

    protected void write0(TextMessageWriter w) {};
}
