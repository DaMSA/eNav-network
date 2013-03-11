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
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.ProtocolWriter;
import dk.dma.navnet.core.messages.s2c.ReplyMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class FindServices extends ReplyMessage<FindServicesAck> {

    final String serviceName;

    final int max;

    public FindServices(ProtocolReader pr) throws IOException {
        super(MessageType.FIND_SERVICE, pr);
        this.serviceName = requireNonNull(pr.takeString());
        this.max = pr.takeInt();
    }

    /**
     * @param messageType
     */
    public FindServices(String serviceName, int max) {
        super(MessageType.FIND_SERVICE);
        this.serviceName = requireNonNull(serviceName);
        this.max = max;

    }

    public int getMax() {
        return max;
    }

    public String getServiceName() {
        return serviceName;
    }

    /** {@inheritDoc} */
    @Override
    protected void write0(ProtocolWriter w) {
        w.writeString(serviceName);
        w.writeInt(max);
    }

    public FindServicesAck createReply(String[] array) {
        return new FindServicesAck(getReplyTo(), array);
    }
}
