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
import java.net.InetSocketAddress;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.core.transport.ServerTransportFactory;
import dk.dma.navnet.core.transport.Transport;

/**
 * 
 * @author Kasper Nielsen
 */
public class WebsocketServerTransportFactory extends ServerTransportFactory {

    /** The jetty server */
    private final Server server;

    public WebsocketServerTransportFactory(InetSocketAddress sa) {
        server = new Server(sa);
        // Sets setReuseAddress
        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        connector.setReuseAddress(true);
    }

    /** {@inheritDoc} */
    @Override
    public void startAccept(final Supplier<Transport> supplier) throws IOException {
        requireNonNull(supplier);
        // Creates the web socket handler that accept incoming requests
        WebSocketHandler wsHandler = new WebSocketHandler() {
            public void configure(WebSocketServletFactory factory) {
                factory.setCreator(new WebSocketCreator() {
                    public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
                        WebsocketTransportSession st = new WebsocketTransportSession(supplier.get());
                        return st;
                    }
                });
            }
        };

        server.setHandler(wsHandler);
        try {
            server.start();
            System.out.println("Ready to accept incoming sockets");
        } catch (Exception e) {
            try {
                server.stop();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() throws IOException {
        try {
            server.stop();
        } catch (Exception e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
    }
}
