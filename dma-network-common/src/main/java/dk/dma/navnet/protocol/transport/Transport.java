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
package dk.dma.navnet.protocol.transport;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.protocol.AbstractProtocol;
import dk.dma.navnet.protocol.connection.Connection;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Transport extends AbstractProtocol {

    public CloseCode close = null;

    final CountDownLatch closedLatch = new CountDownLatch(1);

    private volatile Connection connection;

    /** A latch that is released when we receive a connected message from the remote end. */
    final CountDownLatch openedLatch = new CountDownLatch(1);

    volatile Session session = null;

    /** {@inheritDoc} */
    public final void close(final ClosingCode reason) {
        Session s = session;
        try {
            if (s != null) {
                s.close(new CloseReason(new CloseCode() {
                    public int getCode() {
                        return reason.getId();
                    }
                }, reason.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    protected void onTransportClose(ClosingCode reason) {}

    protected void onTransportConnect() {}

    protected void onTransportError(Throwable cause) {}

    protected abstract void onTransportMessage(TransportMessage message);

    /** {@inheritDoc} */
    @OnClose
    public final void onWebSocketClose(javax.websocket.CloseReason closeReason) {
        try {
            session = null;
            ClosingCode cc = ClosingCode.create(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
            onTransportClose(cc);
        } finally {
            closedLatch.countDown();
        }
    }

    @OnMessage
    public final void onWebsocketMessage(String message) {
        System.out.println("Received: " + message);
        try {
            onTransportMessage(TransportMessage.parseMessage(message));
        } catch (Throwable e) {
            close(ClosingCode.WRONG_MESSAGE.withMessage(e.getMessage()));
        }
    }

    @OnOpen
    public final void onWebsocketOpen(Session session) {
        this.session = session;
        openedLatch.countDown();
        onTransportConnect();
    }

    /** {@inheritDoc} */
    public final void sendTextAsync(String text) {
        Session s = session;
        Basic r = s == null ? null : s.getBasicRemote();
        if (r != null) {
            try {
                r.sendText(text);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public final void sendTransportMessage(TransportMessage m) {
        String msg = m.toJSON();
        System.out.println("Sending " + msg);
        sendTextAsync(msg);
    }

    /**
     * @param connection
     *            the connection to set
     */
    public final void setConnection(Connection connection) {
        // fullyLock();
        try {
            this.connection = connection;
        } finally {
            // fullyUnlock();
        }
    }
}
