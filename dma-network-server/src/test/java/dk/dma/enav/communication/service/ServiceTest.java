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
package dk.dma.enav.communication.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.HelloService;
import test.stubs.HelloService.GetName;
import test.stubs.HelloService.Reply;
import dk.dma.enav.communication.AbstractNetworkTest;
import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.communication.MaritimeNetworkClient;
import dk.dma.enav.util.function.BiConsumer;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServiceTest extends AbstractNetworkTest {

    @Test
    public void oneClient() throws Exception {
        MaritimeNetworkClient c1 = newClient(ID1);
        c1.serviceRegister(HelloService.GET_NAME, HelloService.create("foo123")).awaitRegistered(4, TimeUnit.SECONDS);

        MaritimeNetworkClient c2 = newClient(ID6);
        ServiceEndpoint<GetName, Reply> end = c2.serviceLocate(HelloService.GET_NAME).nearest()
                .get(6, TimeUnit.SECONDS);
        assertEquals(ID1, end.getId());
        ConnectionFuture<Reply> f = end.invoke(new HelloService.GetName());
        assertEquals("foo123", f.get(4, TimeUnit.SECONDS).getName());
    }

    @Test
    public void manyClients() throws Exception {
        MaritimeNetworkClient c1 = newClient(ID1);
        c1.serviceRegister(HelloService.GET_NAME, HelloService.create("foo123")).awaitRegistered(4, TimeUnit.SECONDS);

        for (MaritimeNetworkClient c : newClients(20)) {
            ServiceEndpoint<GetName, Reply> end = c.serviceLocate(HelloService.GET_NAME).nearest()
                    .get(6, TimeUnit.SECONDS);
            assertEquals(ID1, end.getId());
            ConnectionFuture<Reply> f = end.invoke(new HelloService.GetName());
            assertEquals("foo123", f.get(4, TimeUnit.SECONDS).getName());
        }
    }

    @Test
    public void oneClientHandle() throws Exception {
        MaritimeNetworkClient c1 = newClient(ID1);
        c1.serviceRegister(HelloService.GET_NAME, HelloService.create("foo123")).awaitRegistered(4, TimeUnit.SECONDS);

        MaritimeNetworkClient c2 = newClient(ID6);
        ServiceEndpoint<GetName, Reply> end = c2.serviceLocate(HelloService.GET_NAME).nearest()
                .get(6, TimeUnit.SECONDS);
        assertEquals(ID1, end.getId());
        ConnectionFuture<Reply> f = end.invoke(new HelloService.GetName());
        final CountDownLatch cdl = new CountDownLatch(1);
        f.handle(new BiConsumer<HelloService.Reply, Throwable>() {
            public void accept(Reply l, Throwable r) {
                assertNull(r);
                assertEquals("foo123", l.getName());
                cdl.countDown();
            }
        });
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }
}
