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
package dk.dma.navnet.core.messages.s2c.connection;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.core.messages.AbstractMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.ProtocolWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectedMessage extends AbstractMessage {

    private final String connectionId;

    public ConnectedMessage(ProtocolReader pr) throws IOException {
        this(pr.takeString());
    }

    /**
     * @param messageType
     */
    public ConnectedMessage(String connectionId) {
        super(MessageType.CONNECTED);
        this.connectionId = requireNonNull(connectionId);
    }

    /** {@inheritDoc} */
    @Override
    public void write(ProtocolWriter w) {
        w.writeString(connectionId);
    }
}
