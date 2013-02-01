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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Closeables;

/**
 * 
 * @author Kasper Nielsen
 */
public class ProxyTester {
    final CopyOnWriteArraySet<Connection> connections = new CopyOnWriteArraySet<>();

    final SocketAddress proxyAddress;

    final SocketAddress remoteAddress;

    final ExecutorService es = Executors.newSingleThreadExecutor();

    volatile ServerSocket ss;

    ProxyTester(SocketAddress proxyAddress, SocketAddress remoteAddress) {
        this.proxyAddress = requireNonNull(proxyAddress);
        this.remoteAddress = requireNonNull(remoteAddress);
    }

    public void start() throws IOException {
        ss = new ServerSocket();
        ss.bind(proxyAddress);
        System.out.println("Starting proxy");
        es.submit(new Runnable() {
            public void run() {
                for (;;) {
                    try {
                        final Socket in = ss.accept();
                        Socket out = new Socket();
                        out.connect(remoteAddress);
                        Connection con = new Connection(in, out);
                        con.inToOut.start();
                        con.outToIn.start();
                        connections.add(con);
                    } catch (Throwable t) {
                        return;
                    }
                }

            }
        });
    }

    void shutdown() throws InterruptedException {
        es.shutdown();
        Closeables.closeQuietly(ss);
        for (Connection c : connections) {
            Closeables.closeQuietly(c.incoming);
            Closeables.closeQuietly(c.outgoing);
        }
        es.awaitTermination(10, TimeUnit.SECONDS);
    }

    static class Connection {
        final Socket incoming;
        final Socket outgoing;

        final Thread inToOut;
        final Thread outToIn;

        Connection(Socket incoming, Socket outgoing) {
            this.incoming = requireNonNull(incoming);
            this.outgoing = requireNonNull(outgoing);
            inToOut = new Thread(new Runnable() {
                public void run() {
                    inToOut();
                }
            });
            outToIn = new Thread(new Runnable() {
                public void run() {
                    outToIn();
                }
            });

        }

        void inToOut() {
            for (;;) {
                try {
                    byte[] buffer = new byte[1024]; // Adjust if you want
                    int bytesRead;
                    InputStream is = incoming.getInputStream();
                    OutputStream os = outgoing.getOutputStream();
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                } catch (Throwable t) {
                    return;
                }
            }
        }

        void outToIn() {
            for (;;) {
                try {
                    byte[] buffer = new byte[1024]; // Adjust if you want
                    int bytesRead;
                    InputStream is = outgoing.getInputStream();
                    OutputStream os = incoming.getOutputStream();
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                } catch (Throwable t) {
                    return;
                }
            }
        }
    }
}
