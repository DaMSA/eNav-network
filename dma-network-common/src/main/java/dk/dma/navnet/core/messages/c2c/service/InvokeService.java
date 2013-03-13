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

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;
import dk.dma.navnet.core.util.JSonUtil;

/**
 * 
 * @author Kasper Nielsen
 */
public class InvokeService extends AbstractRelayedMessage {

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
    protected void write0(TextMessageWriter w) {
        w.writeInt(status);
        w.writeString(conversationId);
        w.writeString(serviceType);
        w.writeString(messageType);
        w.writeString(message);
    }
}
