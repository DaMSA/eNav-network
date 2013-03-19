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
package dk.dma.navnet.core.spix.transport.websocket;

import java.io.IOException;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import dk.dma.navnet.core.spix.transport.Transport;

/**
 * 
 * @author Kasper Nielsen
 */
abstract class AbstractTransportListener extends Transport implements WebSocketListener {
    Session session;

    /** {@inheritDoc} */
    @Override
    public final void onWebSocketBinary(byte[] payload, int offset, int len) {
        System.out.println("GOT BINARY");
        // tryClose(CloseReason.BAD_DATA.getId(), "Expected text only");
    }

    /** {@inheritDoc} */
    @Override
    public final void onWebSocketText(String message) {
        receivedText(message);
    }

    /** {@inheritDoc} */
    @Override
    public void sendText0(String text) {
        System.out.println("SEDING ");
        Session s = session;
        RemoteEndpoint r = s == null ? null : s.getRemote();
        if (r != null) {
            try {
                r.sendString(text);
            } catch (IOException e) {
                e.printStackTrace();
                // ignore
            }
        } else {
            System.out.println("Could not send");
        }

    }
}
