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
package dk.dma.navnet.core.messages;

import java.io.IOException;

import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * A text only message.
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractTextMessage extends AbstractMessage {

    private String receivedMessage;

    public AbstractTextMessage(MessageType messageType) {
        super(messageType);
    }

    public String getReceivedRawMesage() {
        if (receivedMessage == null) {
            throw new IllegalStateException();
        }
        return receivedMessage;
    }

    public void setReceivedRawMesage(String message) {
        if (receivedMessage != null) {
            throw new IllegalStateException("already set");
        }
        this.receivedMessage = message;
    }

    public String toJSON() {
        TextMessageWriter w = new TextMessageWriter();
        w.writeInt(getMessageType().type);
        write(w);
        String s = w.sb.append("]").toString();
        return s;
    }

    protected abstract void write(TextMessageWriter w);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static AbstractTextMessage read(String msg) throws IOException {
        TextMessageReader pr = new TextMessageReader(msg);
        int type = pr.takeInt();
        try {
            Class<? extends AbstractTextMessage> cl = (Class) AbstractMessage.getType(type);
            return cl.getConstructor(TextMessageReader.class).newInstance(pr);
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }
}
