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
import dk.dma.enav.communication.CloseReason;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.AbstractTextMessage;
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

    ServerTransport(ConnectionManager cm) {
        this.cm = requireNonNull(cm);
    }

    /** {@inheritDoc} */
    @Override
    protected void onReceivedText0(AbstractTextMessage m) {
        if (isConnecting) {
            isConnecting = false;
            if (m instanceof HelloMessage) {
                // check that nobody else has removed the connection, in which case it has already been closed
                if (cm.connectingTransports.remove(this)) {
                    HelloMessage hm = (HelloMessage) m;
                    String reconnectId = hm.getReconnectId();
                    String id = hm.getClientId().toString();
                    PositionTime pt = new PositionTime(hm.getLat(), hm.getLon(), -1);
                    for (;;) {
                        ServerConnection sc = cm.clients.get(id);
                        if (sc == null) {
                            sc = new ServerConnection(cm, hm.getClientId());
                        }
                        if (sc.connect(this, reconnectId, pt)) {
                            return;
                        }
                    }
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
        sendMessage(new WelcomeMessage(1, cm.server.getLocalId(), "enavServer/1.0"));
    }

    ServerConnection c() {
        return (ServerConnection) super.ac;
    }

    /** {@inheritDoc} */
    @Override
    protected void closed(CloseReason reason) {
        lock.lock();
        try {
            ServerConnection con = c();
            if (con != null) {
                cm.server.tracker.remove(c());
                cm.clients.remove(con.sid);
            }
            super.closed(reason);
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable cause) {
        cm.server.tracker.remove(c());
        cm.server.connections.disconnected(this);
    }
}
