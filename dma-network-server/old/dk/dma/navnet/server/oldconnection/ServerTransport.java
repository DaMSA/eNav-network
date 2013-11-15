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
package dk.dma.navnet.server.oldconnection;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.shore.ServerId;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;

/**
 * The server side transport.
 * 
 * @author Kasper Nielsen
 */
@ServerEndpoint(value = "/")
public class ServerTransport {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ServerTransport.class);

    /** The connection this transport is attached to, or null, if it is not attached to one. */
    volatile ServerConnection connection;

    /** The server. */
    private final ConnectionManager connectionManager = ConnectionManager.CONNECTION_MANAGER;

    /** The websocket session. */
    private volatile Session session = null;

    /** Whether or not we have received the first hello message from the client. */
    private ConnectionFuture future;

    /** {@inheritDoc} */
    public final void doClose(final ClosingCode reason) {
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
    public final void doSendTextAsync(String text) {
        Session s = session;
        Async r = s == null ? null : s.getAsyncRemote();
        if (r != null) {
            System.out.println("Sending " + text);
            r.sendText(text);
        }
    }

    // should only be used to send non-connection messages
    public final void doSendTransportMessage(TransportMessage m) {
        doSendTextAsync(m.toJSON());
    }

    @OnClose
    public final void onWebSocketClose(CloseReason closeReason) {
        session = null;
        // ClosingCode cc = ClosingCode.create(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
        ServerConnection connection = this.connection;
        if (connection != null) {
            connectionManager.disconnected(connection);
        }
    }

    @OnMessage
    public final void onTextMessage(String textMessage) {
        TransportMessage msg;
        System.out.println("Received: " + textMessage);
        try {
            msg = TransportMessage.parseMessage(textMessage);
        } catch (Exception e) {
            LOG.error("Failed to parse incoming message", e);
            doClose(ClosingCode.WRONG_MESSAGE.withMessage(e.getMessage()));
            return;
        }


        if (msg instanceof HelloMessage) {
            if (future == null) {
                String err = "Unknown messageType " + msg.getClass().getSimpleName();
                LOG.error(err);
                doClose(ClosingCode.WRONG_MESSAGE.withMessage(err));
            } else {
                future.finished((HelloMessage) msg);
            }
        } else if (msg instanceof ConnectionMessage) {
            ServerConnection connection = this.connection;
            if (connection != null) {
                connection.onConnectionMessage(this, (ConnectionMessage) msg);
            }
        } else {
            String err = "Unknown messageType " + msg.getClass().getSimpleName();
            LOG.error(err);
            doClose(ClosingCode.WRONG_MESSAGE.withMessage(err));
        }
    }

    @OnOpen
    public final void onWebsocketOpen(Session session) {
        this.session = session;
        // send a Welcome message to the client as the first thing
        ServerId id = connectionManager.server.getServerId();
        doSendTransportMessage(new WelcomeMessage(1, id, "enavServer/1.0"));
        future = new ConnectionFuture(connectionManager, this);
        future.helloSend();
    }
}
