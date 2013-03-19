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
package dk.dma.navnet.core.spix.transport;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Transport {

    TransportListener listener;
    volatile boolean isConnected;
    private final Lock lock = new ReentrantLock();

    public void close() throws IOException {
        close0();
    }

    protected abstract void close0() throws IOException;

    protected void connected() {
        System.out.println("CONNECTED");
        isConnected = true;
        listener.connected();
    }

    protected void closed(String text) {
        listener.connectionClosed();
        // closeReason.accept(new CloseReason());
    }

    protected void receivedText(String text) {
        listener.receivedText(text);
    }

    public final void sendText(String text) {
        System.out.println("SENDING");
        if (!isConnected) {
            throw new IllegalStateException("Not connected yet");
        }
        requireNonNull(text, "text is null");
        sendText0(text);
    }

    protected abstract void sendText0(String text);

    public void setListener(TransportListener listener) {
        requireNonNull(listener);
        lock.lock();
        try {
            if (this.listener != null) {
                throw new IllegalStateException("receiver has already been set");
            }
            this.listener = listener;
        } finally {
            lock.unlock();
        }
    }
}
