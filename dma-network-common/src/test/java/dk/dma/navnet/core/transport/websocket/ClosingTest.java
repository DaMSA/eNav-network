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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
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
public abstract class ClosingTest {

    ClientTransportFactory ctf;
    ServerTransportFactory stf;

    ClosingTest(ClientTransportFactory ctf, ServerTransportFactory stf) {
        this.ctf = ctf;
        this.stf = stf;
    }

    @Ignore
    @Test
    public void connectClose() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(2);
        stf.startAccept(new Supplier<Transport>() {
            public Transport get() {
                return new Transport() {
                    public void onClosed(int code, String message) {
                        System.out.println(code);
                        assertEquals(1000, code);
                        cdl.countDown();

                    }

                    public void onConnected(TransportSession spi) {
                        super.onConnected(spi);
                        close();
                    }
                };
            }
        });
        // Client
        ctf.connect(new Transport() {
            public void onClosed(int code, String message) {
                System.out.println("GOT " + code);
                cdl.countDown();
            }
        }, 1, TimeUnit.SECONDS);
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void normalClose() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(2);
        stf.startAccept(new Supplier<Transport>() {
            public Transport get() {
                return new Transport() {
                    public void onClosed(int code, String message) {
                        assertEquals(1000, code);
                        cdl.countDown();

                    }

                    public void onConnected(TransportSession spi) {
                        super.onConnected(spi);
                        sendText("CloseMe");
                    }
                };
            }
        });
        // Client
        ctf.connect(new Transport() {
            public void onReceivedText(String text) {
                assertEquals("CloseMe", text);
                close();
            }

            public void onClosed(int code, String message) {
                assertEquals(1000, code);
                cdl.countDown();
            }
        }, 1, TimeUnit.SECONDS);
        assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }

}
