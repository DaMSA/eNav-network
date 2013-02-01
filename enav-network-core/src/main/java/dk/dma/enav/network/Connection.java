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
package dk.dma.enav.network;

import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Connection {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(Connection.class);

    final CountDownLatch fullyClosed = new CountDownLatch(2);

    /** The input stream to read from. */
    final DataInputStream is;

    final String name = "Connection";

    /** The output stream to write to. */
    final DataOutputStream os;

    final BlockingDeque<Packets> outgoingPackets;

    final PersistentConnection pc;

    final Reader rt;

    /** The underlying socket of this connection. */
    final Socket s;

    final Writer wt;

    public Connection(PersistentConnection con, Socket s, DataInputStream is, DataOutputStream os) {
        this.pc = requireNonNull(con);
        this.s = requireNonNull(s);
        this.is = requireNonNull(is);
        this.os = requireNonNull(os);
        this.outgoingPackets = con.outgoingPacketsQueue;
        this.wt = new Writer();
        this.rt = new Reader();
    }

    protected abstract void failed(Throwable cause);

    protected abstract void packetRead(Packets p) throws Exception;

    @SuppressWarnings("unchecked")
    public <T> T readObject() throws Exception {
        return (T) readObject(is);
    }

    synchronized void readOrWriteFailed(Throwable cause) {

    }

    public void start(Executor e) {
        e.execute(wt);
        e.execute(rt);
    }

    synchronized void threadExited(Object o) {
        fullyClosed.countDown();
        if (fullyClosed.getCount() == 0) {
            Closeables.closeQuietly(os);
            Closeables.closeQuietly(is);
            Closeables.closeQuietly(s);
        }
    }

    public void writeObject(Object o) throws IOException {
        writeObject(os, o);
    }

    public static Object readObject(DataInputStream dis) throws Exception {
        int size = dis.readInt();
        byte[] input = new byte[size];
        dis.readFully(input);
        ByteArrayInputStream bin = new ByteArrayInputStream(input);
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        return in.readObject();
    }

    public static void writeObject(DataOutputStream dos, Object o) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(20000);
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));) {
            out.writeObject(o);
        }
        dos.writeInt(bout.size());
        dos.write(bout.toByteArray());
        dos.flush();
    }

    class Reader extends InterruptableRunnable<Packets> {

        /**
         * @param connection
         */
        Reader() {
            super(name + "-ReadThread");
        }

        protected void finished() {
            threadExited(this);
        }

        /** {@inheritDoc} */
        @Override
        protected void repeat() {
            Packets p = null;
            try {
                p = Packets.read(Connection.this);
                packetRead(p);
            } catch (Throwable e) {
                if (!isStopped()) {
                    readOrWriteFailed(e);
                    LOG.error("Reader thread failed " + Connection.this, e);
                    wt.shutdown();
                    return;
                }
            }
            if (p != null && p.b == Packets.CONNECTION_DISCONNECT) {
                shutdown();
                try {
                    s.shutdownInput();
                } catch (IOException ignore) {}
            } else if (p != null && p.b == Packets.CONNECTION_DISCONNECTED) {
                shutdown();
                try {
                    s.shutdownOutput();
                } catch (IOException ignore) {}
            }
        }
    }

    class Writer extends InterruptableRunnable<Packets> {

        Writer() {
            super(name + "-WriteThread");
        }

        protected void finished() {
            threadExited(this);
        }

        /** {@inheritDoc} */
        @Override
        protected void repeat() throws InterruptedException {
            Packets p = outgoingPackets.take();
            lock();
            try {
                p.write(Connection.this);
                if (p.b == Packets.CONNECTION_DISCONNECT) {
                    shutdown();
                } else if (p.b == Packets.CONNECTION_DISCONNECTED) {
                    shutdown();
                }
            } catch (Throwable e) {
                if (!isStopped()) {
                    System.out.println("writing packet " + p);
                    LOG.error("Reader thread failed", e);
                    // TODO this will not work if queue is at capacity constaint
                    outgoingPackets.offerFirst(p);
                    rt.shutdown();
                }
            } finally {
                unlock();
            }
        }
    }
}
