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
package dk.dma.navnet.core.transport.websocket;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.WebSocketClient;

import dk.dma.navnet.core.transport.ClientTransportFactory;
import dk.dma.navnet.core.transport.Transport;

/**
 * 
 * @author Kasper Nielsen
 */
class WebsocketClientTransportFactory extends ClientTransportFactory {

    private final URI uri;

    /** The actual websocket client. Changes when reconnecting. */
    WebSocketClient client;

    /**
     * @param uri
     */
    WebsocketClientTransportFactory(URI uri) {
        this.uri = requireNonNull(uri);
    }

    synchronized WebSocketClient client() throws IOException {
        if (client == null) {
            client = new WebSocketClient();
            try {
                client.start();
            } catch (Exception e) {
                throw new IOException(e);
            }

        }
        return client;
    }

    /** {@inheritDoc} */
    @Override
    public void connect(Transport listener, long timeout, TimeUnit unit) throws IOException {
        WebsocketClientTransport client = new WebsocketClientTransport(listener);
        System.out.println("Connecting to " + uri);
        try {
            client().connect(client, uri).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                throw new InterruptedIOException();
            } else if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
        try {
            client.connected.await(timeout, unit);
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    /** {@inheritDoc} */
    public void shutdown() throws IOException {
        try {
            client.stop();
        } catch (Exception e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
    }

    static class WebsocketClientTransport extends AbstractTransportListener {

        /**
         * @param receiver
         * @param session
         */
        public WebsocketClientTransport(Transport listener) {
            super(listener);
        }

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketError(Throwable cause) {
            // onError(cause);
        }

    }
}
