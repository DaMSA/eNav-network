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

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import dk.dma.enav.maritimecloud.ClosingCode;

/**
 * The client implementation of a transport. Every time the client connects to a server a new transport is created.
 * Unlike {@link ClientConnection} which will persist over multiple connects, and provide smooth reconnect.
 * 
 * @author Kasper Nielsen
 */
public final class JavaxWebsocketTransport extends ClientTransport {

    /** The websocket session. */
    private final WebSocketConnection mConnection = new WebSocketConnection();

    JavaxWebsocketTransport(ClientConnectFuture connectFuture, ClientConnection connection) {
        super(connectFuture, connection);
    }

    /** {@inheritDoc} */
    void doClose(final ClosingCode reason) {
        mConnection.disconnect();
    }

    /** {@inheritDoc} */
    public void onClose(int code, String reasons) {
        ClosingCode reason = ClosingCode.create(code, reasons);
        connection.transportDisconnected(this, reason);
    }

    public void onTextMessage(String textMessage) {
        super.onTextMessage(textMessage);
    }

    public void sendText(String text) {
        if (mConnection.isConnected()) {
            if (text.length() < 1000) {
                System.out.println("Sending : " + text);
                // System.out.println("Sending " + this + " " + text);
            }
            mConnection.sendTextMessage(text);
        }
    }

    void connect(URI uri) throws IOException {
        try {
            mConnection.connect(uri.toString(), new WebSocketHandler() {

                /** {@inheritDoc} */
                @Override
                public void onClose(int code, String reason) {
                    JavaxWebsocketTransport.this.onClose(code, reason);
                }

                /** {@inheritDoc} */
                @Override
                public void onTextMessage(String payload) {
                    JavaxWebsocketTransport.this.onTextMessage(payload);
                }
            });
        } catch (WebSocketException e) {
            throw new IOException(e);
        }

    }
}
