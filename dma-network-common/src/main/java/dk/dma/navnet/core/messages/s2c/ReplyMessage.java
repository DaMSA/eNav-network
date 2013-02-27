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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.navnet.core.messages.AbstractMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.ProtocolWriter;
import dk.dma.navnet.core.spi.AbstractHandler;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ReplyMessage<T> extends AbstractMessage {

    long replyTo;
    AbstractHandler handler;

    public ReplyMessage(MessageType messageType, ProtocolReader pr) throws IOException {
        super(messageType);
        this.replyTo = pr.takeLong();
    }

    /**
     * @param messageType
     */
    public ReplyMessage(MessageType messageType) {
        super(messageType);
    }

    public void setReplyTo(long replyTo) {
        this.replyTo = replyTo;
    }

    public void setCallback(AbstractHandler handler) {
        this.handler = handler;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) Void.class;
    }

    // replyTo skal saettes saaledes
    // at de bliver skrevet i samme raekkefoelge som de bliver nummereret

    public void reply(T value) {
        try {
            reply0(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void write(ProtocolWriter w) {
        w.writeLong(replyTo);
        write0(w);
    }

    protected abstract void write0(ProtocolWriter w);

    public void reply0(T value) throws JsonProcessingException {
        if (value != null) {
            String val = new ObjectMapper().writeValueAsString(value);
            val = val.replace("\"", "\\\"");
            System.out.println("VALL " + val);
            handler.sendMessage(new AckMessage(replyTo, 0, val));
        } else {
            handler.sendMessage(new AckMessage(replyTo, 0, ""));
        }
    }

    public void replyWithFailure(int code, String message) {

    }
}
