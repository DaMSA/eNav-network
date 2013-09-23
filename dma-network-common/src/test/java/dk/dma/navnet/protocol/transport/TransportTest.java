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


/**
 * 
 * @author Kasper Nielsen
 */
public class TransportTest {
    //
    // TransportClientFactory ctf;
    // TransportServerFactory stf;
    //
    // @Before
    // public void setup() {
    // ctf = TransportClientFactory.createClient("localhost:12345");
    // stf = TransportServerFactory.createServer(12345);
    // }

    // @Test(expected = IOException.class)
    // public void notConnectable() throws IOException {
    // ctf.connect(new Transport() {}, 1, TimeUnit.SECONDS);
    // }
    //
    // @Test
    // public void transportRecieved() throws IOException, InterruptedException {
    // final CountDownLatch cdl = new CountDownLatch(1);
    // stf.startAccept(new Supplier<Transport>() {
    // public Transport get() {
    // cdl.countDown();
    // return new Transport() {};
    // }
    // });
    // ctf.connect(new Transport() {}, 1, TimeUnit.SECONDS);
    // assertTrue(cdl.await(1, TimeUnit.SECONDS));
    // }
    //
    // @Test
    // public void sendTextFromServer() throws Exception {
    // final CountDownLatch cdl = new CountDownLatch(1);
    // stf.startAccept(new Supplier<Transport>() {
    // public Transport get() {
    // return new Transport() {
    // public void onTransportConnect() {
    // rawSend("Hello321");
    // }
    // };
    // }
    // });
    //
    // // Client
    //
    // ctf.connect(new Transport() {
    // public void onTransportConnect() {
    // assertEquals(1, cdl.getCount());
    // }
    //
    // public void rawReceive(String text) {
    // assertEquals("Hello321", text);
    // cdl.countDown();
    // }
    // }, 1, TimeUnit.SECONDS);
    // assertTrue(cdl.await(1, TimeUnit.SECONDS));
    // }
    //
    // @Test
    // public void sendTextFromClient() throws Exception {
    // final CountDownLatch cdl = new CountDownLatch(1);
    // final Transport ts = new Transport() {
    // public void onTransportConnect() {
    // assertEquals(1, cdl.getCount());
    // }
    //
    // public void rawReceive(String text) {
    // assertEquals("Hello321", text);
    // cdl.countDown();
    // }
    // };
    // stf.startAccept(new Supplier<Transport>() {
    // public Transport get() {
    // return ts;
    // }
    // });
    // ctf.connect(new Transport() {
    // public void onTransportConnect() {
    // rawSend("Hello321");
    // }
    // }, 1, TimeUnit.SECONDS);
    // assertTrue(cdl.await(1, TimeUnit.SECONDS));
    // }
    //
    // @Test
    // public void pingPong() throws Exception {
    // final CountDownLatch cdl = new CountDownLatch(1);
    // stf.startAccept(new Supplier<Transport>() {
    // public Transport get() {
    // return new Transport() {
    // public void rawReceive(String text) {
    // Integer i = Integer.parseInt(text);
    // rawSend("" + (i + 1));
    // }
    // };
    // }
    // });
    //
    // ctf.connect(new Transport() {
    // public void onTransportConnect() {
    // rawSend("1");
    // }
    //
    // public void rawReceive(String text) {
    // Integer i = Integer.parseInt(text);
    // if (i.equals(100)) {
    // cdl.countDown();
    // } else {
    // rawSend("" + (i + 1));
    // }
    //
    // }
    // }, 1, TimeUnit.SECONDS);
    // assertTrue(cdl.await(1, TimeUnit.SECONDS));
    // }

    // @After
    // public void teardown() throws IOException {
    // stf.shutdown();
    // ctf.shutdown();
    // }
}
