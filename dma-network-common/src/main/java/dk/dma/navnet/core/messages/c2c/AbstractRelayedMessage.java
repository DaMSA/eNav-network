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

import java.io.IOException;

import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractRelayedMessage extends AbstractTextMessage {

    String destination;

    String source;

    /**
     * @param messageType
     */
    public AbstractRelayedMessage(MessageType messageType) {
        super(messageType);
    }

    public AbstractRelayedMessage(MessageType messageType, TextMessageReader pr) throws IOException {
        super(messageType);
        this.source = pr.takeString();
        this.destination = pr.takeString();
    }

    /**
     * @return the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @return the src
     */
    public String getSource() {
        return source;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    protected final void write(TextMessageWriter w) {
        w.writeString(source);
        w.writeString(destination);
        write0(w);
    }

    protected abstract void write0(TextMessageWriter w);
}
