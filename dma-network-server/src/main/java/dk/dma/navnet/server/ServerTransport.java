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

import static java.util.Objects.requireNonNull;
import dk.dma.enav.communication.CloseReason;
import dk.dma.navnet.core.messages.TransportMessage;
import dk.dma.navnet.core.messages.transport.ConnectedMessage;
import dk.dma.navnet.core.messages.transport.HelloMessage;
import dk.dma.navnet.core.messages.transport.WelcomeMessage;
import dk.dma.navnet.protocol.transport.Transport;

/**
 * The server side transport.
 * 
 * @author Kasper Nielsen
 */
class ServerTransport extends Transport {

    /** Whether or not we have received the first hello message from the client. */
    private boolean helloReceived;

    /** The server. */
    private final EmbeddableCloudServer server;

    ServerTransport(EmbeddableCloudServer server) {
        this.server = requireNonNull(server);
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportConnect() {
        // send a Welcome message to the client as the first thing
        sendTransportMessage(new WelcomeMessage(1, server.getLocalId(), "enavServer/1.0"));
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportMessage(TransportMessage m) {
        if (m instanceof WelcomeMessage || m instanceof ConnectedMessage) {
            // Close Transport should never see this messages on the server
        } else if (m instanceof HelloMessage) {
            if (helloReceived) {
                // Close Transport should never see a hello message now
            } else {
                helloReceived = true;
                server.connectionManager.onMessageHello(this, (HelloMessage) m);
            }
        } else if (!helloReceived) {
            // Close transport, expecting HelloMessage as the first message from the client
        } else {
            super.onTransportMessage(m);
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
    public void onTransportClose(CloseReason reason) {
        ServerConnection con = (ServerConnection) super.getConnection();
        if (con != null) {
            server.connectionManager.disconnected(con);
        }
    }
}
