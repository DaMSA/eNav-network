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
package dk.dma.navnet.core.messages.c2c.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.c2c.ClientRelayedMessage;
import dk.dma.navnet.core.messages.util.JSonUtil;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class InvokeServiceResult extends ClientRelayedMessage {

    final String message;

    final String replyType;

    final String uuid;

    public InvokeServiceResult(TextMessageReader pr) throws IOException {
        super(MessageType.SERVICE_INVOKE_RESULT, pr);
        this.uuid = requireNonNull(pr.takeString());
        this.message = requireNonNull(pr.takeString());
        this.replyType = requireNonNull(pr.takeString());
    }

    /**
     * @param messageType
     */
    public InvokeServiceResult(String uuid, String message, String replyType) {
        super(MessageType.SERVICE_INVOKE_RESULT);
        this.uuid = uuid;
        this.message = message;
        this.replyType = replyType;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the replyType
     */
    public String getReplyType() {
        return replyType;
    }

    /**
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /** {@inheritDoc} */
    @Override
    protected void write1(TextMessageWriter w) {
        w.writeString(uuid);
        w.writeString(message);
        w.writeString(replyType);
    }

    /** {@inheritDoc} */
    @Override
    public ClientRelayedMessage cloneIt() {
        InvokeServiceResult is = new InvokeServiceResult(uuid, JSonUtil.escape(message), replyType);
        is.setDestination(super.getDestination());
        is.setSource(super.getSource());
        return is;
    }
}
