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
package dk.dma.navnet.core.transport;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.communication.CloseReason;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Transport {

    /** A single lock protected the current state. */
    private final Lock lock = new ReentrantLock();

    /** The transport session, or null if not yet connected. */
    private volatile TransportSession session;

    /** If closed, the reason why this transport was closed. */
    volatile CloseReason closeReason;

    /** The current state of the transport */
    volatile State state;

    public final void close(CloseReason reason) {
        requireNonNull(reason);
        lock.lock();
        try {
            if (state == State.CLOSED) {
                return;
            }
            closeReason = reason;
            if (session != null) {
                session.close(reason);
            }
        } finally {
            lock.unlock();
        }
    }

    public void onClosed(CloseReason reason) {}

    public void onConnected(TransportSession session) {
        requireNonNull(session);
        lock.lock();
        try {
            this.session = session;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Receives a string message. The default implementation does nothing.
     * 
     * @param message
     *            the string message
     */
    public void onReceivedText(String message) {};

    /**
     * Asynchronous sends a text to the remote end.
     * 
     * @param text
     *            the text to send
     * @throws IllegalStateException
     *             if the transport is not yet connected
     * @throws NullPointerException
     *             if the specified text is null
     */
    public final void sendText(String text) {
        requireNonNull(text, "text is null");
        TransportSession session = this.session;
        if (session == null) {
            throw new IllegalStateException("Not connected yet");
        }
        session.sendText(text);
    }

    public State getState() {
        return state;
    }

    public enum State {
        INITIALIZED, CONNECTED, CLOSED;
    }
}
