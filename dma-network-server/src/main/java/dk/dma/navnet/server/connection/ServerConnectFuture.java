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
package dk.dma.navnet.server.connection;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.server.target.Target;
import dk.dma.navnet.server.target.TargetManager;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConnectFuture {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ServerConnectFuture.class);

    final ServerTransport serverTransport;

    /**
     * @param serverTransport
     */
    public ServerConnectFuture(ServerTransport serverTransport) {
        this.serverTransport = requireNonNull(serverTransport);
    }

    /**
     * 
     */
    public void helloSend() {}

    /**
     * @param msg
     */
    public void onMessage(TransportMessage m) {
        if (m instanceof HelloMessage) {
            HelloMessage hm = (HelloMessage) m;
            TargetManager tm = serverTransport.cm.targetManager;
            Target target = tm.getTarget(hm.getClientId());
            target.fullyLock();
            try {
                ServerConnection connection = target.getConnection();
                if (connection == null) {
                    connection = new ServerConnection(target, serverTransport.server);
                    connection.transport = serverTransport;
                } else {
                    ServerTransport st = connection.transport;
                    if (st != null) {
                        connection.transport = null;
                        st.doClose(ClosingCode.DUPLICATE_CONNECT);
                    }
                }
                serverTransport.sendText(new ConnectedMessage(connection.getConnectionId(), 0).toJSON());
                serverTransport.connection = connection;
                serverTransport.connectFuture = null;
                target.setLatestPosition(PositionTime.create(hm.getLat(), hm.getLon(), System.currentTimeMillis()));
                target.setConnection(connection);
                // see if we already have a connection

            } finally {
                target.fullyUnlock();
            }

            // InternalClient client = connection.connectionManager.client;
            // PositionTime pt = client.readCurrentPosition();
            // transport.sendText(new HelloMessage(client.getLocalId(), "enavClient/1.0", "", reconnectId, pt
            // .getLatitude(), pt.getLongitude()).toJSON());
            // receivedHelloMessage = true;
        } else {
            String err = "Expected a welcome message, but was: " + m.getClass().getSimpleName();
            LOG.error(err);
            // transport.doClose(ClosingCode.WRONG_MESSAGE.withMessage(err));
        }


    }
}
