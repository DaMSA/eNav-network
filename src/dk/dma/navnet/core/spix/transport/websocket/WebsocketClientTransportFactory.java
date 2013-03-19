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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import dk.dma.navnet.core.spix.transport.ClientTransportFactory;
import dk.dma.navnet.core.spix.transport.Transport;
import dk.dma.navnet.core.spix.transport.TransportListener;

/**
 * 
 * @author Kasper Nielsen
 */
class WebsocketClientTransportFactory extends ClientTransportFactory {

    private final URI uri;

    /**
     * @param uri
     */
    WebsocketClientTransportFactory(URI uri) {
        this.uri = requireNonNull(uri);
    }

    /** {@inheritDoc} */
    @Override
    public Transport connect(TransportListener listener, long timeout, TimeUnit unit) throws IOException {
        WebsocketClientTransport client = new WebsocketClientTransport(listener);
        try {
            client.client.start();
        } catch (Exception e) {
            throw new IOException(e);
        }
        try {
            client.client.connect(client, uri).get();
        } catch (InterruptedException | ExecutionException e) {
            try {
                client.client.stop();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
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
        return client;
    }

    static class WebsocketClientTransport extends AbstractTransportListener {
        /** The actual websocket client. Changes when reconnecting. */
        final WebSocketClient client = new WebSocketClient();

        final CountDownLatch connected = new CountDownLatch(1);

        /**
         * @param receiver
         * @param session
         */
        public WebsocketClientTransport(TransportListener listener) {
            setListener(listener);
        }

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketClose(int statusCode, String reason) {
            session = null;
            super.closed(reason);
            System.out.println("CLOSED");
        }

        /** {@inheritDoc} */
        @Override
        public void onWebSocketConnect(Session session) {
            this.session = session;
            connected.countDown();
        }

        /** {@inheritDoc} */
        @Override
        public final void onWebSocketError(Throwable cause) {
            // onError(cause);
        }

        /** {@inheritDoc} */
        @Override
        protected void close0() throws IOException {
            try {
                client.stop();
            } catch (Exception e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw new IOException(e);
            }
        }
    }
}
