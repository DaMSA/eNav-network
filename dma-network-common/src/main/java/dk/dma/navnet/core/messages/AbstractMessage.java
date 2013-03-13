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
import java.util.Map.Entry;
import java.util.TreeMap;

import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractMessage {
    static final MessageType M = MessageType.CONNECTED;
    static final MessageType[] types;
    private final MessageType messageType;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
    }

    static {
        TreeMap<Integer, MessageType> m = new TreeMap<>();
        for (MessageType mt : MessageType.values()) {
            m.put(mt.type, mt);
        }
        types = new MessageType[m.lastKey() + 1];
        for (Entry<Integer, MessageType> e : m.entrySet()) {
            types[e.getKey()] = e.getValue();
        }
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String toJSON() {
        TextMessageWriter w = new TextMessageWriter();
        w.writeInt(messageType.type);
        write(w);
        String s = w.sb.append("]").toString();
        return s;
    }

    protected abstract void write(TextMessageWriter w);

    public static AbstractMessage read(String msg) throws IOException {
        TextMessageReader pr = new TextMessageReader(msg);
        int type = pr.takeInt();
        // TODO guard indexes
        Class<? extends AbstractMessage> cl = types[type].cl;
        try {
            return cl.getConstructor(TextMessageReader.class).newInstance(pr);
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }
}
