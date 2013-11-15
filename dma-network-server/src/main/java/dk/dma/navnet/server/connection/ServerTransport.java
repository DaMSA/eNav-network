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

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.shore.ServerId;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.server.InternalServer;

/**
 * 
 * @author Kasper Nielsen
 */
@ServerEndpoint(value = "/")
public class ServerTransport {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ServerTransport.class);

    /** The connection this transport is attached to, or null, if it is not attached to one. */
    volatile ServerConnection connection;

    /** The websocket session. */
    private volatile Session session = null;

    /** Whether or not we have received the first hello message from the client. */
    ServerConnectFuture connectFuture;

    final ConnectionManager cm;

    final InternalServer server;

    public ServerTransport(InternalServer server) {
        this.cm = requireNonNull(server.getService(ConnectionManager.class));
        this.server = requireNonNull(server);
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

    @OnMessage
    public void onTextMessage(String textMessage) {
        TransportMessage msg;
        System.out.println("Received: " + textMessage);
        try {
            msg = TransportMessage.parseMessage(textMessage);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Failed to parse incoming message", e);
            doClose(ClosingCode.WRONG_MESSAGE.withMessage(e.getMessage()));
            return;
        }
        if (connectFuture != null) {
            connectFuture.onMessage(msg);
        } else if (msg instanceof ConnectionMessage) {
            ConnectionMessage m = (ConnectionMessage) msg;
            connection.messageReceive(this, m);
        } else {
            String err = "Unknown messageType " + msg.getClass().getSimpleName();
            LOG.error(err);
            doClose(ClosingCode.WRONG_MESSAGE.withMessage(err));
        }
    }

    void sendText(String text) {
        Session session = this.session;
        if (session != null) {
            if (text.length() < 1000) {
                System.out.println("Sending " + text);
            }
            session.getAsyncRemote().sendText(text);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session; // wait on the server to send a hello message
        System.out.println("Hello " + cm);

        // send a Welcome message to the client as the first thing
        ServerId id = cm.server.getServerId();
        connectFuture = new ServerConnectFuture(this);
        sendText(new WelcomeMessage(1, id, "enavServer/1.0").toJSON());
    }

}
