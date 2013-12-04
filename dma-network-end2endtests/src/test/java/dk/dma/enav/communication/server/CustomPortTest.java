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

import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.enav.maritimecloud.MaritimeCloudClientConfiguration;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld;
import dk.dma.navnet.server.InternalServer;
import dk.dma.navnet.server.ServerConfiguration;

/**
 * Tests that we can run both the server and the client on a custom port.
 * 
 * @author Kasper Nielsen
 */
public class CustomPortTest {

    @Test
    public void testNonDefaultPort() throws Exception {
        ServerConfiguration sc = new ServerConfiguration();
        sc.setServerPort(12445);
        InternalServer server = new InternalServer(sc);
        server.start();
        MaritimeCloudClientConfiguration b = MaritimeCloudClientConfiguration.create("mmsi://1234");
        b.setHost("localhost:12445");
        System.out.println("a");
        try (MaritimeCloudClient c = b.build()) {
            System.out.println("b");
            c.broadcast(new HelloWorld());
        }
        System.out.println("c");
        server.shutdown();
        server.awaitTerminated(1, TimeUnit.SECONDS);
    }
}
