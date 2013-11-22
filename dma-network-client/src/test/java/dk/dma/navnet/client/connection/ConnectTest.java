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
package dk.dma.navnet.client.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.navnet.client.AbstractClientConnectionTest;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;


/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectTest extends AbstractClientConnectionTest {

    @Test
    public void connectTest() throws Exception {
        MaritimeCloudClient c = create();
        t.m.take();
        t.send(new ConnectedMessage("ABC", 0));
        assertTrue(c.connection().awaitConnected(1, TimeUnit.SECONDS));
        assertTrue(c.connection().isConnected());
    }

    /**
     * Tests that messages send before the connect finished is still delivered.
     */
    @Test
    public void connectedSlow() throws Exception {
        MaritimeCloudClient c = create();
        t.m.take();

        c.broadcast(new HelloWorld("foo1"));// enqueue before we have actually connected.
        Thread.sleep(50);
        t.send(new ConnectedMessage("ABC", 0));
        c.broadcast(new HelloWorld("foo2"));
        assertTrue(c.connection().awaitConnected(10, TimeUnit.SECONDS));
        assertTrue(c.connection().isConnected());
        assertEquals("foo1", t.take(BroadcastSend.class).tryRead(HelloWorld.class).getMessage());
        assertEquals("foo2", t.take(BroadcastSend.class).tryRead(HelloWorld.class).getMessage());
    }
}
