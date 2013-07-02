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
package dk.dma.enav.communication;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
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
        PersistentConnection pc1 = newClient(ID1);

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
    @Ignore
    public void serviceInvoke() throws Exception {
        PersistentConnection pc1 = newClient(ID1);
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