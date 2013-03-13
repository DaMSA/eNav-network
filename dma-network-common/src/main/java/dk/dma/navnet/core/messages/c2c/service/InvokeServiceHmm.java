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
package dk.dma.navnet.core.messages.c2c.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.s2c.ReplyMessage;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
class InvokeServiceHmm extends ReplyMessage<FindServiceResult> {

    final String serviceName;

    final String maritimeId;

    final String message;

    public InvokeServiceHmm(TextMessageReader pr) throws IOException {
        super(MessageType.SERVICE_INVOKE, pr);
        this.serviceName = requireNonNull(pr.takeString());
        this.maritimeId = requireNonNull(pr.takeString());
        this.message = requireNonNull(pr.takeString());
    }

    /**
     * @param messageType
     */
    public InvokeServiceHmm(String serviceName, String maritimeId, String message) {
        super(MessageType.SERVICE_INVOKE);
        this.serviceName = requireNonNull(serviceName);
        this.maritimeId = maritimeId;
        this.message = requireNonNull(message);
    }

    /** {@inheritDoc} */
    @Override
    protected void write0(TextMessageWriter w) {
        w.writeString(serviceName);
        w.writeString(maritimeId);
        w.writeString(message);
    }

    public FindServiceResult createReply(String[] array) {
        return new FindServiceResult(getReplyTo(), array);
    }
}
