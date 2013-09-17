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
package dk.dma.navnet.protocol.transport;

import org.junit.Before;

/**
 * 
 * @author Kasper Nielsen
 */
public class ClosingTest {

    TransportClientFactory ctf;
    TransportServerFactory stf;

    @Before
    public void setup() {
        ctf = TransportClientFactory.createClient("localhost:12346");
        stf = TransportServerFactory.createServer(12346);
    }

    // @Ignore
    // @Test
    // public void connectClose() throws Exception {
    // final CountDownLatch cdl = new CountDownLatch(2);
    // stf.startAccept(new Supplier<Transport>() {
    // public Transport get() {
    // return new Transport() {
    // public void onTransportClose(ClosingCode reason) {
    // assertEquals(1000, reason.getId());
    // cdl.countDown();
    //
    // }
    //
    // public void onTransportConnect() {
    // close(ClosingCode.NORMAL);
    // }
    // };
    // }
    // });
    // // Client
    // ctf.connect(new Transport() {
    // public void onTransportClose(ClosingCode reason) {
    // System.out.println("GOT " + reason.getId());
    // cdl.countDown();
    // }
    // }, 1, TimeUnit.SECONDS);
    // assertTrue(cdl.await(1, TimeUnit.SECONDS));
    // }
    //
    // @Test
    // public void normalClose() throws Exception {
    // final CountDownLatch cdl = new CountDownLatch(2);
    // stf.startAccept(new Supplier<Transport>() {
    // public Transport get() {
    // return new Transport() {
    // public void onTransportClose(ClosingCode reason) {
    // assertEquals(1000, reason.getId());
    // cdl.countDown();
    //
    // }
    //
    // public void onTransportConnect() {
    // rawSend("CloseMe");
    // }
    // };
    // }
    // });
    // // Client
    // ctf.connect(new Transport() {
    // public void rawReceive(String text) {
    // assertEquals("CloseMe", text);
    // close(ClosingCode.NORMAL);
    // }
    //
    // public void onTransportClose(ClosingCode reason) {
    // assertEquals(1000, reason.getId());
    // cdl.countDown();
    // }
    // }, 1, TimeUnit.SECONDS);
    // assertTrue(cdl.await(1, TimeUnit.SECONDS));
    // }

}
