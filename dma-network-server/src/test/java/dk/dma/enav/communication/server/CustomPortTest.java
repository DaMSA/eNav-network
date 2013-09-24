/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.enav.communication.server;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.HelloWorld;
import dk.dma.enav.communication.MaritimeNetworkConnectionBuilder;
import dk.dma.enav.communication.MaritimeNetworkConnection;
import dk.dma.navnet.server.EmbeddableCloudServer;

/**
 * Tests that we can run both the server and the client on a custom port.
 * 
 * @author Kasper Nielsen
 */
public class CustomPortTest {

    @Test
    public void testNonDefaultPort() throws Exception {
        EmbeddableCloudServer server = new EmbeddableCloudServer(12445);
        server.start();
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create("mmsi://1234");
        b.setHost("localhost:12445");
        System.out.println("a");
        try (MaritimeNetworkConnection c = b.build()) {
            System.out.println("b");
            c.broadcast(new HelloWorld());
        }
        System.out.println("c");
        server.shutdown();
        server.awaitTerminated(1, TimeUnit.SECONDS);
    }
}
