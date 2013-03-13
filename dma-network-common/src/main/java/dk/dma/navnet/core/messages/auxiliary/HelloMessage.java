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
package dk.dma.navnet.core.messages.auxiliary;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class HelloMessage extends AbstractTextMessage {

    // include connection type
    private final MaritimeId clientId;

    private final String clientInfo;

    private final String reconnectId;

    public HelloMessage(TextMessageReader pr) throws IOException {
        this(MaritimeId.create(pr.takeString()), pr.takeString(), pr.takeString(), 123);
    }

    /**
     * @param messageType
     */
    public HelloMessage(MaritimeId clientId, String clientInfo, String reconnectId, long lastReceivedMessageId) {
        super(MessageType.HELLO);
        this.clientId = requireNonNull(clientId);
        this.clientInfo = requireNonNull(clientInfo);
        this.reconnectId = reconnectId;
    }

    /**
     * @return the clientId
     */
    public MaritimeId getClientId() {
        return clientId;
    }

    /**
     * @return the clientInfo
     */
    public String getClientInfo() {
        return clientInfo;
    }

    /**
     * @return the reconnectId
     */
    public String getReconnectId() {
        return reconnectId;
    }

    /** {@inheritDoc} */
    @Override
    protected void write(TextMessageWriter w) {
        w.writeString(clientId.toString());
        w.writeString(clientInfo);
        w.writeString(reconnectId);
    }
}
