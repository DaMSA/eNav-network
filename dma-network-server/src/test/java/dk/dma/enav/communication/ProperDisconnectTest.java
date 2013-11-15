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

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import test.util.TesstService;
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
        MaritimeNetworkClient c1 = newClient(ID1);
        // c1.serviceRegister(TestService.TEST_INIT,
        // new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
        // public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
        // context.complete(l.reply());
        // }
        // }).awaitRegistered(1, TimeUnit.SECONDS);

        // pt.killFirstConnection();
        c1.serviceRegister(TesstService.TEST_INIT,
                new InvocationCallback<TesstService.TestInit, TesstService.TestReply>() {
                    public void process(TesstService.TestInit l, Context<TesstService.TestReply> context) {
                        context.complete(l.reply());
                    }
                }).awaitRegistered(1, TimeUnit.SECONDS);

        for (;;) {
            Thread.sleep(50);
        }
    }
}
