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
import dk.dma.enav.communication.CloseReason;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Transport {

    TransportSession spi;

    public void onConnected(TransportSession spi) {
        this.spi = requireNonNull(spi);
    }

    public final void close() {
        if (spi != null) {
            spi.close();
        }
    }

    public final void close(CloseReason reason) {
        if (spi != null) {
            spi.close();
        }
    }

    public final void close(int code, String text) {
        if (spi != null) {
            spi.close();
        }
    }

    public final void sendText(String text) {
        requireNonNull(text, "text is null");
        TransportSession spi = this.spi;
        if (spi == null) {
            throw new IllegalStateException("Not connected yet");
        }
        spi.sendText(text);
    }

    public void onClosed(int code, String message) {}

    /**
     * @param message
     */
    public void onReceivedText(String message) {}
}
