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

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import test.util.TestService;
import dk.dma.enav.communication.service.InvocationCallback;

/**
 * 
 * @author Kasper Nielsen
 */
public class ProperDisconnectTest extends AbstractNetworkTest {

    public ProperDisconnectTest() {
        super(true);
    }

    @Test
    @Ignore
    public void randomKilling() throws Exception {
        PersistentConnection c1 = newClient(ID1);
        // c1.serviceRegister(TestService.TEST_INIT,
        // new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
        // public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
        // context.complete(l.reply());
        // }
        // }).awaitRegistered(1, TimeUnit.SECONDS);

        // pt.killFirstConnection();
        c1.serviceRegister(TestService.TEST_INIT,
                new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
                    public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
                        context.complete(l.reply());
                    }
                }).awaitRegistered(1, TimeUnit.SECONDS);

        for (;;) {
            System.out.println(c1.getState());
            Thread.sleep(50);
        }
    }
}
