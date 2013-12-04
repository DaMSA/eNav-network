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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import dk.dma.enav.maritimecloud.ClosingCode;
import dk.dma.enav.maritimecloud.MaritimeCloudConnection;

/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectionStateListenerTest extends AbstractNetworkTest {

    @Test
    public void connected() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        newClient(newBuilder(ID1).addListener(new AbstractConnectionTestListener() {
            public void connected() {
                assertEquals(1, cdl.getCount());
                cdl.countDown();
            }
        }));
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void connectedTwoListeners() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(2);
        newClient(newBuilder(ID1).addListener(new AbstractConnectionTestListener() {
            public void connected() {
                cdl.countDown();
            }
        }).addListener(new AbstractConnectionTestListener() {
            public void connected() {
                cdl.countDown();
            }
        }));
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    @Test
    @Ignore
    public void closed() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        newClient(newBuilder(ID1).addListener(new AbstractConnectionTestListener() {
            public void disconnected(ClosingCode reason) {
                assertEquals(1000, reason.getId());
                cdl.countDown();
            }
        }));
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    static class AbstractConnectionTestListener extends MaritimeCloudConnection.Listener {

    }
}
