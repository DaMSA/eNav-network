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
package dk.dma.navnet.server;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.spi.AbstractMessageTransport;

/**
 * 
 * @author Kasper Nielsen
 */
class ServerTransport extends AbstractMessageTransport {

    final ConnectionManager cm;

    boolean isConnecting = true;

    ServerConnection connection;

    ServerTransport(ConnectionManager cm) {
        this.cm = requireNonNull(cm);
    }

    /** {@inheritDoc} */
    @Override
    protected void onReceivedText0(AbstractTextMessage m) {
        if (isConnecting) {
            isConnecting = false;
            if (m instanceof HelloMessage) {
                if (cm.connectingTransports.remove(this)) {// check that nobody else has removed the connection
                    HelloMessage hm = (HelloMessage) m;
                    UUID uuid = UUID.randomUUID();
                    PositionTime pt = new PositionTime(hm.getLat(), hm.getLon(), -1);
                    ServerConnection sc = connection = new ServerConnection(cm, this, hm.getClientId(), pt);
                    cm.clients.put(hm.getClientId().toString(), sc);
                    sendMessage(new ConnectedMessage(uuid.toString()));
                    cm.server.tracker.update(sc, pt);
                }
            } else {
                // oops
            }
        } else {
            super.onReceivedText0(m);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void connected() {
        sendMessage(new WelcomeMessage(1, cm.server.id, "enavServer/1.0"));
    }

    /** {@inheritDoc} */
    @Override
    protected void closed(int statusCode, String reason) {
        cm.server.tracker.remove(connection);
        cm.server.connections.disconnected(this);
        super.closed(statusCode, reason);
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable cause) {
        cm.server.tracker.remove(connection);
        cm.server.connections.disconnected(this);
    }
}
