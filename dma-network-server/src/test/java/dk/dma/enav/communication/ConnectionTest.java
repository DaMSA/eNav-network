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
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import dk.dma.enav.communication.MaritimeNetworkConnection.State;

/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectionTest extends AbstractNetworkTest {

    @Test
    public void manyClients() throws Exception {
        newClients(20);
        assertEquals(20, si.getNumberOfConnections());
    }

    @Test
    public void singleClient() throws Exception {
        newClient(ID1);
        assertEquals(1, si.getNumberOfConnections());
        // Thread.sleep(1000);
        // assertEquals(1, si.getNumberOfConnections());
    }

    @Test
    public void singleClientClose() throws Exception {
        @SuppressWarnings("resource")
        MaritimeNetworkConnection pc = newClient(ID1);
        assertEquals(1, si.getNumberOfConnections());
        pc.awaitState(State.CONNECTED, 1, TimeUnit.SECONDS);
        assertEquals(1, si.getNumberOfConnections());
        pc.close();
        assertTrue(pc.getState() == State.CLOSED || pc.getState() == State.TERMINATED);
        pc.awaitState(State.TERMINATED, 1, TimeUnit.SECONDS);
        for (int i = 0; i < 100; i++) {
            if (si.getNumberOfConnections() == 0) {
                return;
            }
            Thread.sleep(15);
        }
        fail();
    }
}
