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
package dk.dma.navnet.core.messages.c2c;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.ProtocolWriter;

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
    public InvokeService(ProtocolReader pr) throws IOException {
        super(MessageType.SERVICE_INVOKE, pr);
        status = pr.takeInt();
        conversationId = pr.takeString();
        message = pr.takeString();
        messageType = pr.takeString();
        serviceType = pr.takeString();
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
    protected void write0(ProtocolWriter w) {
        w.writeInt(status);
        w.writeString(conversationId);
        w.writeString(serviceType);
        w.writeString(messageType);
        w.writeString(message);
    }
}
