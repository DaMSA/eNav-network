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
package dk.dma.navnet.core.transport.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.core.transport.ClientTransportFactory;
import dk.dma.navnet.core.transport.ServerTransportFactory;
import dk.dma.navnet.core.transport.Transport;
import dk.dma.navnet.core.transport.TransportSession;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractTransportTest {

    ClientTransportFactory ctf;
    ServerTransportFactory stf;

    AbstractTransportTest(ClientTransportFactory ctf, ServerTransportFactory stf) {
        this.ctf = ctf;
        this.stf = stf;
    }

    @Before
    public void setup() {

    }

    @Test(expected = IOException.class)
    public void notConnectable() throws IOException {
        ctf.connect(new Transport() {}, 1, TimeUnit.SECONDS);
    }

    @Test
    public void transportRecieved() throws IOException, InterruptedException {
        final CountDownLatch cdl = new CountDownLatch(1);
        stf.startAccept(new Supplier<Transport>() {
            public Transport get() {
                cdl.countDown();
                return new Transport() {};
            }
        });
        ctf.connect(new Transport() {}, 1, TimeUnit.SECONDS);
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void sendTextFromServer() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        stf.startAccept(new Supplier<Transport>() {
            public Transport get() {
                return new Transport() {
                    public void onConnected(TransportSession spi) {
                        super.onConnected(spi);
                        sendText("Hello321");
                    }
                };
            }
        });

        // Client

        ctf.connect(new Transport() {
            public void onConnected(TransportSession spi) {
                super.onConnected(spi);
                assertEquals(1, cdl.getCount());
            }

            public void onReceivedText(String text) {
                assertEquals("Hello321", text);
                cdl.countDown();
            }
        }, 1, TimeUnit.SECONDS);
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void sendTextFromClient() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Transport ts = new Transport() {
            public void onConnected(TransportSession spi) {
                super.onConnected(spi);
                assertEquals(1, cdl.getCount());
            }

            public void onReceivedText(String text) {
                assertEquals("Hello321", text);
                cdl.countDown();
            }
        };
        stf.startAccept(new Supplier<Transport>() {
            public Transport get() {
                return ts;
            }
        });
        ctf.connect(new Transport() {
            public void onConnected(TransportSession spi) {
                super.onConnected(spi);
                sendText("Hello321");
            }
        }, 1, TimeUnit.SECONDS);
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void pingPong() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        stf.startAccept(new Supplier<Transport>() {
            public Transport get() {
                return new Transport() {
                    public void onReceivedText(String text) {
                        Integer i = Integer.parseInt(text);
                        sendText("" + (i + 1));
                    }
                };
            }
        });

        ctf.connect(new Transport() {
            public void onConnected(TransportSession spi) {
                super.onConnected(spi);
                sendText("1");
            }

            public void onReceivedText(String text) {
                Integer i = Integer.parseInt(text);
                if (i.equals(100)) {
                    cdl.countDown();
                } else {
                    sendText("" + (i + 1));
                }

            }
        }, 1, TimeUnit.SECONDS);
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    @After
    public void teardown() throws IOException {
        stf.shutdown();
        ctf.shutdown();
    }
}
