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
package dk.dma.enav.communication.server;

import org.junit.Test;

import test.stubs.HelloWorld;
import dk.dma.enav.communication.PersistentConnection;
import dk.dma.navnet.client.MaritimeNetworkConnectionBuilder;
import dk.dma.navnet.server.ENavNetworkServer;

/**
 * Tests that we can run both the server and the client on a custom port.
 * 
 * @author Kasper Nielsen
 */
public class CustomPortTest {

    @Test
    public void testNonDefaultPort() throws Exception {
        ENavNetworkServer server = new ENavNetworkServer(12345);
        server.start();
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create("mmsi://1234");
        b.setHost("localhost:12345");
        System.out.println("a");
        try (PersistentConnection c = b.build()) {
            System.out.println("b");
            c.broadcast(new HelloWorld());
        }
        System.out.println("c");
        server.shutdown();
    }
}
