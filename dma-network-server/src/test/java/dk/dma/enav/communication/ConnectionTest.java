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
package dk.dma.enav.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import dk.dma.enav.maritimecloud.MaritimeCloudClient;

/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectionTest extends AbstractNetworkTest {

    @Test
    public void singleClient() throws Exception {
        MaritimeCloudClient c = newClient(ID1);
        assertTrue(c.connection().awaitConnected(10, TimeUnit.SECONDS));
        assertEquals(1, si.info().getConnectionCount());
        // c.close();
        // Thread.sleep(1000);
        // assertEquals(1, si.info().getConnectionCount());
    }

    @Test
    public void manyClients() throws Exception {
        for (MaritimeCloudClient c : newClients(20)) {
            c.connection().awaitConnected(10, TimeUnit.SECONDS);
        }
        assertEquals(20, si.info().getConnectionCount());
    }

    @Test
    public void singleClientClose() throws Exception {
        MaritimeCloudClient pc1;
        try (MaritimeCloudClient pc = newClient(ID1)) {
            pc1 = pc;
            assertTrue(pc1.connection().awaitConnected(10, TimeUnit.SECONDS));
            assertEquals(1, si.info().getConnectionCount());
            pc.connection().awaitConnected(1, TimeUnit.SECONDS);
            assertEquals(1, si.info().getConnectionCount());
        }
        assertTrue(pc1.isClosed());
        pc1.awaitTermination(1, TimeUnit.SECONDS);
        for (int i = 0; i < 100; i++) {
            if (si.info().getConnectionCount() == 0) {
                return;
            }
            Thread.sleep(1);
        }
        // fail();
    }
}
