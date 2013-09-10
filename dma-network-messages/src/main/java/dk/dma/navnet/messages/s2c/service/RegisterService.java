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
package dk.dma.navnet.messages.s2c.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;
import dk.dma.navnet.messages.s2c.ServerRequestMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class RegisterService extends ServerRequestMessage<RegisterServiceResult> {

    final String serviceName;

    // Area
    public RegisterService(TextMessageReader pr) throws IOException {
        super(MessageType.REGISTER_SERVICE, pr);
        this.serviceName = requireNonNull(pr.takeString());
    }

    /**
     * @param messageType
     */
    public RegisterService(String serviceName) {
        super(MessageType.REGISTER_SERVICE);
        this.serviceName = requireNonNull(serviceName);
    }

    public String getServiceName() {
        return serviceName;
    }

    /** {@inheritDoc} */
    @Override
    protected void write1(TextMessageWriter w) {
        w.writeString(serviceName);
    }

    public RegisterServiceResult createReply() {
        return new RegisterServiceResult(getReplyTo());
    }
}
