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
public class InvokeService extends ClientRelayedMessage {

    final String conversationId;

    final String message;

    final String messageType;

    final String serviceType;

    final int status;

    /**
     * @param isInitialMessage
     * @param conversationId
     * @param serviceType
     * @param messageType
     * @param message
     */
    public InvokeService(int status, String conversationId, String serviceType, String messageType, String message) {
        super(MessageType.SERVICE_INVOKE);
        this.status = status;
        this.conversationId = conversationId;
        this.serviceType = serviceType;
        this.messageType = messageType;
        this.message = message;
    }

    /**
     * @param messageType
     * @throws IOException
     */
    public InvokeService(TextMessageReader pr) throws IOException {
        super(MessageType.SERVICE_INVOKE, pr);
        status = pr.takeInt();
        conversationId = pr.takeString();
        serviceType = pr.takeString();
        messageType = pr.takeString();
        message = pr.takeString();
    }

    /** {@inheritDoc} */
    @Override
    public ClientRelayedMessage cloneIt() {
        InvokeService is = new InvokeService(status, conversationId, serviceType, messageType, JSonUtil.escape(message));
        is.setDestination(super.getDestination());
        is.setSource(super.getSource());
        return is;
    }

    /**
     * @param result
     */
    public InvokeServiceResult createReply(Object result) {
        InvokeServiceResult isa = new InvokeServiceResult(conversationId, JSonUtil.persistAndEscape(result), result
                .getClass().getName());
        isa.setDestination(getSource());
        isa.setSource(getDestination());
        return isa;
    }

    /**
     * @return the conversationId
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the messageType
     */
    public String getServiceMessageType() {
        return messageType;
    }

    /**
     * @return the serviceType
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "InvokeService [conversationId=" + conversationId + ", message=" + message + ", messageType="
                + messageType + ", serviceType=" + serviceType + ", status=" + status + ", destination="
                + getDestination() + ", source=" + getSource() + "]";
    }

    /** {@inheritDoc} */
    @Override
    protected void write1(TextMessageWriter w) {
        w.writeInt(status);
        w.writeString(conversationId);
        w.writeString(serviceType);
        w.writeString(messageType);
        w.writeString(message);
    }
}
