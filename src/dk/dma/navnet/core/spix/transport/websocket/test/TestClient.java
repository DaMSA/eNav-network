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
package dk.dma.navnet.core.spix.transport.websocket.test;

import java.util.concurrent.TimeUnit;

import dk.dma.navnet.core.spix.transport.ClientTransportFactory;
import dk.dma.navnet.core.spix.transport.Transport;
import dk.dma.navnet.core.spix.transport.TransportListener;
import dk.dma.navnet.core.spix.transport.websocket.WebsocketTransports;

/**
 * 
 * @author Kasper Nielsen
 */
public class TestClient {

    public static void main(String[] args) throws Exception {
        ClientTransportFactory tf = WebsocketTransports.createClient("localhost:43234");
        Transport t = tf.connect(new TransportListener() {
            public void receivedText(String t) {
                System.out.println("Got " + t);
            }
        }, 1, TimeUnit.SECONDS);
        t.sendText("hej");
        Thread.sleep(10000);
        t.close();
    }
}
