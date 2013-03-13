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

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.s2c.AckMessage;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class FindServiceResult extends AckMessage {

    final String[] maritimeIds;

    public FindServiceResult(TextMessageReader pr) throws IOException {
        super(MessageType.FIND_SERVICE_ACK, pr);
        this.maritimeIds = requireNonNull(pr.takeStringArray());
    }

    /**
     * @param messageType
     */
    public FindServiceResult(long id, String[] ids) {
        super(MessageType.FIND_SERVICE_ACK, id);
        this.maritimeIds = requireNonNull(ids);

    }

    public String[] getMax() {
        return maritimeIds;
    }

    public final Class<String[]> getType() {
        return String[].class;
    }

    /** {@inheritDoc} */
    @Override
    protected void write0(TextMessageWriter w) {
        w.writeStringArray(maritimeIds);
    }
}
