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
package dk.dma.navnet.protocol;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.TransportMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Transport {

    public CloseCode close = null;

    final CountDownLatch closedLatch = new CountDownLatch(1);

    private volatile Connection connection;

    /** A latch that is released when we receive a connected message from the remote end. */
    final CountDownLatch openedLatch = new CountDownLatch(1);

    /** A read lock. */
    private final ReentrantLock readLock = new ReentrantLock();

    /** The websocket session. */
    volatile Session session = null;

    /** A write lock. */
    private final ReentrantLock writeLock = new ReentrantLock();

    public final boolean awaitOpened(long timeout, TimeUnit unit) throws InterruptedException {
        return openedLatch.await(timeout, unit);
    }

    /** {@inheritDoc} */
    public final void doClose(final ClosingCode reason) {
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

    public final void fullyLock() {
        readLock.lock();
        writeLock.lock();
    }

    public final void fullyUnlock() {
        writeLock.unlock();
        readLock.unlock();
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
    public final void onWebSocketClose(CloseReason closeReason) {
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
            doClose(ClosingCode.WRONG_MESSAGE.withMessage(e.getMessage()));
        }
    }

    @OnOpen
    public final void onWebsocketOpen(Session session) {
        this.session = session;
        openedLatch.countDown();
        onTransportConnect();
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
