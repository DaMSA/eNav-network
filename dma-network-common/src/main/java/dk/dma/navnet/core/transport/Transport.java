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

    private final Lock lock = new ReentrantLock();

    TransportSession spi;

    volatile CloseReason closeReason;

    volatile State state;

    public final void close(CloseReason reason) {
        requireNonNull(reason);
        lock.lock();
        try {
            if (state == State.CLOSED) {
                return;
            }
            closeReason = reason;
            if (spi != null) {
                spi.close(reason);
            }
        } finally {
            lock.unlock();
        }
    }

    public void onClosed(CloseReason reason) {}

    public void onConnected(TransportSession spi) {
        this.spi = requireNonNull(spi);
    }

    /**
     * @param message
     */
    public void onReceivedText(String message) {}

    public final void sendText(String text) {
        requireNonNull(text, "text is null");
        TransportSession spi = this.spi;
        if (spi == null) {
            throw new IllegalStateException("Not connected yet");
        }
        spi.sendText(text);
    }

    public enum State {
        INITIALIZED, CONNECTED, CLOSED;
    }
}
