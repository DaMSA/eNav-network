/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.navnet.messages.c2c.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;
import dk.dma.navnet.messages.s2c.ServerRequestMessage;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;

/**
 * 
 * @author Kasper Nielsen
 */
class InvokeServiceHmm extends ServerRequestMessage<FindServiceResult> {

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
    protected void write1(TextMessageWriter w) {
        w.writeString(serviceName);
        w.writeString(maritimeId);
        w.writeString(message);
    }

    public FindServiceResult createReply(String[] array) {
        return new FindServiceResult(getReplyTo(), array);
    }
}
