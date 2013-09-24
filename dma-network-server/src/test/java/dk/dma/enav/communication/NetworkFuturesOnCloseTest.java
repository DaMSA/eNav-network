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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.HelloService;
import test.stubs.HelloService.GetName;
import test.stubs.HelloService.Reply;
import dk.dma.enav.communication.service.ServiceEndpoint;

/**
 * Tests
 * 
 * @author Kasper Nielsen
 */
@SuppressWarnings("resource")
public class NetworkFuturesOnCloseTest extends AbstractNetworkTest {

    @Test
    public void serviceFind() throws Exception {
        MaritimeNetworkConnection pc1 = newClient(ID1);

        ConnectionFuture<ServiceEndpoint<GetName, Reply>> f = pc1.serviceFind(HelloService.GET_NAME).nearest();
        ConnectionFuture<ServiceEndpoint<GetName, Reply>> f2 = f.timeout(4, TimeUnit.SECONDS);

        pc1.close();
        try {
            System.out.println(f.get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof ConnectionClosedException);
        }

        pc1.close();
        try {
            System.out.println(f2.get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof ConnectionClosedException);
        }
    }

    @Test
    public void serviceInvoke() throws Exception {
        MaritimeNetworkConnection pc1 = newClient(ID1);
        newClient(ID2);

        ConnectionFuture<ServiceEndpoint<GetName, Reply>> f = pc1.serviceFind(HelloService.GET_NAME).nearest();
        pc1.close();
        try {
            f.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof ConnectionClosedException);
        }
    }
}
