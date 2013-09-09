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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import dk.dma.enav.communication.CloseReason;

/**
 * 
 * @author Kasper Nielsen
 */
@ClientEndpoint
@ServerEndpoint(value = "/")
public class TransportClientListener implements SomeListener {

    volatile Session session = null;

    public CloseReason close = null;
    /** A latch that is released when we receive a connected message from the remote end. */
    final CountDownLatch connected = new CountDownLatch(1);

    /** The upstream protocol layer. */
    private final Transport transport;

    /**
     * Creates a new listener.
     * 
     * @param transport
     *            the upstream protocol layer
     */
    TransportClientListener(Transport transport) {
        this.transport = requireNonNull(transport);
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            transport.rawReceive(message);
        } catch (Exception e) {
            // close connection, for example parse error
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        connected.countDown();
        transport.setSession(this);
    }

    /** {@inheritDoc} */
    public final void sendTextAsync(String text) {
        Session s = session;
        Async r = s == null ? null : s.getAsyncRemote();
        if (r != null) {
            r.sendText(text);
        }
    }

    /** {@inheritDoc} */
    public final void sendText(String text) {
        Session s = session;
        Basic r = s == null ? null : s.getBasicRemote();
        if (r != null) {
            try {
                r.sendText(text);
            } catch (IOException e) {
                e.printStackTrace();
                // We nede to reconnect
            }
        }
    }

    /** {@inheritDoc} */
    @OnClose
    public final void onWebSocketClose(javax.websocket.CloseReason closeReason) {
        session = null;
        transport.closedByWebsocket(CloseReason.create(closeReason.getCloseCode().getCode(),
                closeReason.getReasonPhrase()));
    }

    /** {@inheritDoc} */
    public final void close(final CloseReason reason) {
        Session s = session;
        try {
            if (s != null) {
                s.close(new javax.websocket.CloseReason(new CloseCode() {
                    public int getCode() {
                        return reason.getId();
                    }
                }, reason.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
