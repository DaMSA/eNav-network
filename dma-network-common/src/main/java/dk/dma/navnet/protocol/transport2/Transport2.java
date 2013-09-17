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
package dk.dma.navnet.protocol.transport2;

import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.TransportMessage;

/**
 * 
 * @author Kasper Nielsen
 */
@ClientEndpoint
@ServerEndpoint(value = "/")
public abstract class Transport2 {

    volatile Session session = null;

    public CloseCode close = null;

    /** A latch that is released when we receive a connected message from the remote end. */
    final CountDownLatch openedLatch = new CountDownLatch(1);

    final CountDownLatch closedLatch = new CountDownLatch(1);

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        openedLatch.countDown();
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        try {
            onTransportMessage(TransportMessage.parseMessage(message));
        } catch (Throwable e) {
            close(ClosingCode.WRONG_MESSAGE.withMessage(e.getMessage()));
        }
    }

    protected abstract void onTransportMessage(TransportMessage message);

    /** {@inheritDoc} */
    public final void sendTextAsync(String text) {
        Session s = session;
        Async r = s == null ? null : s.getAsyncRemote();
        if (r != null) {
            r.sendText(text);
        }
    }

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

    /** {@inheritDoc} */
    @OnClose
    public final void onWebSocketClose(javax.websocket.CloseReason closeReason) {
        try {
            session = null;
            ClosingCode cc = ClosingCode.create(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
        } finally {
            closedLatch.countDown();
        }
    }
}
