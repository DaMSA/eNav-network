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
package test;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import test.util.TestService;
import test.util.TestService.TestInit;
import test.util.TestService.TestReply;
import dk.dma.enav.communication.MaritimeNetworkConnection;
import dk.dma.enav.communication.NetworkFuture;
import dk.dma.enav.communication.service.InvocationCallback;
import dk.dma.enav.model.MaritimeId;

/**
 * 
 * @author Kasper Nielsen
 */
@Ignore
public class ReconnectTest extends AbstractNetworkTest {
    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");
    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");

    public ReconnectTest() {
        super(true);
    }

    @Test
    @Ignore
    public void randomKilling() throws Exception {
        final AtomicInteger ai = new AtomicInteger();
        MaritimeNetworkConnection c1 = newClient(ID1);
        c1.serviceRegister(null, new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
            public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
                context.complete(l.reply());
                ai.incrementAndGet();
                System.out.println("Receive " + l);
            }
        }).awaitRegistered(1, TimeUnit.SECONDS);

        MaritimeNetworkConnection c6 = newClient(ID6);

        pt.killRandom(1000, TimeUnit.MILLISECONDS);
        Map<TestInit, NetworkFuture<TestService.TestReply>> set = new LinkedHashMap<>();
        assertEquals(2, si.getNumberOfConnections());
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                TestService.TestInit init = new TestService.TestInit(i * 100 + j, ID6, ID1);
                set.put(init, c6.serviceInvoke(ID1, init));
                System.out.println("SEND " + init);
            }
            for (Map.Entry<TestInit, NetworkFuture<TestService.TestReply>> f : set.entrySet()) {
                try {
                    TestReply reply = f.getValue().get(5, TimeUnit.SECONDS);
                    System.out.println("End " + reply.getInit());
                } catch (TimeoutException e) {
                    System.err.println(f.getKey());
                    throw e;
                }
            }
            set.clear();
        }

        assertEquals(100 * 100, ai.get());
        System.out.println(ai);
    }

    @Test
    public void randomKilling2() throws Exception {
        final AtomicInteger ai = new AtomicInteger();
        MaritimeNetworkConnection c1 = newClient(ID1);
        c1.serviceRegister(null, new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
            public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
                context.complete(l.reply());
                ai.incrementAndGet();
                System.out.println("Receive " + l);
            }
        }).awaitRegistered(1, TimeUnit.SECONDS);

        MaritimeNetworkConnection c6 = newClient(ID6);

        pt.killRandom(500, TimeUnit.MILLISECONDS);
        Map<TestInit, NetworkFuture<TestService.TestReply>> set = new LinkedHashMap<>();
        assertEquals(2, si.getNumberOfConnections());
        for (int j = 0; j < 10; j++) {
            TestService.TestInit init = new TestService.TestInit(j, ID6, ID1);
            set.put(init, c6.serviceInvoke(ID1, init));
            System.out.println("SEND " + init);
        }
        for (Map.Entry<TestInit, NetworkFuture<TestService.TestReply>> f : set.entrySet()) {
            try {
                TestReply reply = f.getValue().get(5, TimeUnit.SECONDS);
                System.out.println("End " + reply.getInit());
            } catch (TimeoutException e) {
                System.err.println(f.getKey());
                throw e;
            }
        }
        set.clear();

        // assertEquals(100 * 100, ai.get());
        System.out.println(ai);
    }

    @Test
    @Ignore
    public void singleClient() throws Exception {
        final AtomicInteger ai = new AtomicInteger();
        MaritimeNetworkConnection c1 = newClient(ID1);
        c1.serviceRegister(null, new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
            public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
                context.complete(l.reply());
                ai.incrementAndGet();
            }
        }).awaitRegistered(1, TimeUnit.SECONDS);

        MaritimeNetworkConnection c6 = newClient(ID6);

        assertEquals(2, si.getNumberOfConnections());
        for (int i = 0; i < 100; i++) {
            pt.killAll();
            TestInit ti = new TestInit(i, ID1, ID6);
            assertEquals(ti.getId(), c6.serviceInvoke(ID1, ti).get(5, TimeUnit.SECONDS).getInit().getId());
        }

        System.out.println(ai);
    }
}
