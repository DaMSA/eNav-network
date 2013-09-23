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
package dk.dma.navnet.server;

import javax.websocket.server.ServerEndpoint;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.protocol.Transport;

/**
 * The server side transport.
 * 
 * @author Kasper Nielsen
 */
@ServerEndpoint(value = "/")
public class ServerTransport extends Transport {

    /** Whether or not we have received the first hello message from the client. */
    private boolean hasReceivedHelloFromClient;

    /** The server. */
    private final EmbeddableCloudServer server = EmbeddableCloudServer.SERVER;

    /** {@inheritDoc} */
    @Override
    public void onTransportConnect() {
        server.connectionManager.connectingTransports.add(this);
        // send a Welcome message to the client as the first thing
        doSendTransportMessage(new WelcomeMessage(1, server.getLocalId(), "enavServer/1.0"));
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportMessage(TransportMessage m) {
        if (m instanceof WelcomeMessage || m instanceof ConnectedMessage) {
            // Close Transport should never see this messages on the server
        } else if (m instanceof HelloMessage) {
            if (hasReceivedHelloFromClient) {
                // Close Transport should never see a hello message now
            } else {
                hasReceivedHelloFromClient = true;
                server.connectionManager.onMessageHello(this, (HelloMessage) m);
            }
        } else if (!hasReceivedHelloFromClient) {
            // Close transport, expecting HelloMessage as the first message from the client
        } else {
            ConnectionMessage cm = (ConnectionMessage) m;
            getConnection().onConnectionMessage(cm);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportError(Throwable cause) {
        ServerConnection sc = (ServerConnection) super.getConnection();
        server.connectionManager.disconnected(sc);
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportClose(ClosingCode reason) {
        ServerConnection con = (ServerConnection) super.getConnection();
        if (con != null) {
            server.connectionManager.disconnected(con);
        }
    }
}
