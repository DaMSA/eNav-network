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
package dk.dma.navnet.core.spi.transport.websocket.test;

import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.core.spi.transport.ServerTransportFactory;
import dk.dma.navnet.core.spi.transport.Transport;
import dk.dma.navnet.core.spi.transport.TransportListener;
import dk.dma.navnet.core.spi.transport.websocket.WebsocketTransports;

/**
 * 
 * @author Kasper Nielsen
 */
public class TestServer {

    public static void main(String[] args) throws Exception {
        ServerTransportFactory tf = WebsocketTransports.createServer(43234);
        tf.startAccept(new Consumer<Transport>() {
            @Override
            public void accept(Transport t) {
                System.out.println("Connected");
                t.setListener(new TransportListener() {
                    @Override
                    public void receivedText(String text) {
                        System.out.println("got " + text);
                    }
                });
            }
        });
    }
}
