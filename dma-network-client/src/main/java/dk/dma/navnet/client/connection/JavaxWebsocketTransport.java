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
package dk.dma.navnet.client.connection;

import java.io.IOException;
import java.net.URI;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.maritimecloud.ClosingCode;

/**
 * The client implementation of a transport. Every time the client connects to a server a new transport is created.
 * Unlike {@link ClientConnection} which will persist over multiple connects, and provide smooth reconnect.
 * 
 * @author Kasper Nielsen
 */
@ClientEndpoint
public final class JavaxWebsocketTransport extends ClientTransport {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(JavaxWebsocketTransport.class);

    /** The websocket session. */
    volatile Session session = null;

    JavaxWebsocketTransport(ClientConnectFuture connectFuture, ClientConnection connection) {
        super(connectFuture, connection);
    }

    /** {@inheritDoc} */
    void doClose(final ClosingCode reason) {
        Session session = this.session;
        if (session != null) {
            CloseReason cr = new CloseReason(new CloseCode() {
                public int getCode() {
                    return reason.getId();
                }
            }, reason.getMessage());

            try {
                session.close(cr);
            } catch (Exception e) {
                LOG.error("Failed to close connection", e);
            }
        }
    }

    /** {@inheritDoc} */
    @OnClose
    public void onClose(CloseReason closeReason) {
        session = null;
        ClosingCode reason = ClosingCode.create(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
        connection.transportDisconnected(this, reason);
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session; // wait on the server to send a hello message
    }

    @OnMessage
    public void onTextMessage(String textMessage) {
        super.onTextMessage(textMessage);
    }

    public void sendText(String text) {
        Session session = this.session;
        if (session != null) {
            if (text.length() < 1000) {
                System.out.println("Sending : " + text);
                // System.out.println("Sending " + this + " " + text);
            }
            session.getAsyncRemote().sendText(text);
        }
    }

    void connect(URI uri) throws IOException {
        WebSocketContainer ws = ContainerProvider.getWebSocketContainer();
        try {
            ws.connectToServer(this, uri);
        } catch (DeploymentException e) {
            throw new IllegalStateException("Internal Error", e);
        }
    }
}
