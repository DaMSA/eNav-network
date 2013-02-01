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
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jsr166e.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.network.client.NetworkFutureImpl;

/**
 * <p>
 * We use a dedicated thread for both reading and writing. If we want to support a lot of connections we need to move to
 * asynchronous I/O.
 * 
 * @author Kasper Nielsen
 */
public abstract class PersistentConnection {

    /** Used for naming the connections. */
    private final static AtomicLong CONNECTION_COUNTER = new AtomicLong();

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(PersistentConnection.class);

    /** The number of bytes read by this connection. */
    final AtomicLong bytesRead;

    /** The number of bytes written by this connection. */
    final AtomicLong bytesWritten;

    volatile Connection connection;

    final UUID connectionId;

    /** A thread started when trying to reconnect. */
    final Executor e;

    volatile boolean isClosed = false;

    /** The local id of this connection. */
    private final MaritimeId localId;

    /** The next message id. */
    private final AtomicLong messageId = new AtomicLong();

    /** The name of this connection. */
    final String name = "connection-" + CONNECTION_COUNTER.incrementAndGet();

    /** A queue with outgoing packets. */
    final BlockingDeque<Packets> outgoingPacketsQueue = new LinkedBlockingDeque<>();

    /** The remote socket address of this connection. (primary used for reconnects by the client) */
    private final SocketAddress remoteAddress;

    /** The remote id of this connection. */
    private final MaritimeId remoteId;

    final ConcurrentHashMap<Long, CompletableFuture<?>> replies = new ConcurrentHashMap<>();

    final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.OPEN);

    public PersistentConnection(Executor e, MaritimeId localId, MaritimeId remoteId, SocketAddress remoteAddress,
            UUID connectionId, AtomicLong bytesWritten, AtomicLong bytesRead) {
        this.localId = requireNonNull(localId);
        this.remoteId = requireNonNull(remoteId);
        this.remoteAddress = requireNonNull(remoteAddress);
        this.connectionId = requireNonNull(connectionId);
        this.e = requireNonNull(e);
        this.bytesRead = requireNonNull(bytesRead);
        this.bytesWritten = requireNonNull(bytesWritten);
    }

    protected PersistentConnection(Executor e, MaritimeId localId, MaritimeId remoteId, UUID connectionId,
            AtomicLong bytesWritten, AtomicLong bytesRead) {
        this.localId = requireNonNull(localId);
        this.remoteId = requireNonNull(remoteId);
        this.remoteAddress = null;
        this.connectionId = requireNonNull(connectionId);
        this.e = requireNonNull(e);
        this.bytesRead = requireNonNull(bytesRead);
        this.bytesWritten = requireNonNull(bytesWritten);
    }

    public void accept(Socket s, DataInputStream is, DataOutputStream os) throws Exception {
        connection = new Connection(this, s, is, os) {

            @Override
            protected void failed(Throwable cause) {}

            @Override
            protected void packetRead(Packets p) throws Exception {
                packetReadx(p);
            }
        };
        connection.start(e);
    }

    public synchronized void close() {
        if (!isClosed) {
            outgoingPacketsQueue.add(Packets.logout());
            isClosed = true;
        }
    }

    public void closeIt() {

    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public MaritimeId getLocalID() {
        return localId;
    }

    public MaritimeId getRemoteID() {
        return remoteId;
    }

    public boolean isClosed() {
        return isClosed;
    }

    boolean isServer() {
        return remoteAddress == null;
    }

    protected abstract void packetRead0(Packets p) throws Exception;

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void packetReadx(Packets p) throws Exception {
        if (p.inReplyTo > 0 && (p.b == Packets.REPLY_WITH_SUCCESS || p.b == Packets.REPLY_WITH_FAILURE)
                && p.destination.equals(localId)) {
            CompletableFuture<?> f = replies.remove(p.inReplyTo);
            if (f == null) {
                System.err.println("Orphaned packet with id " + p.inReplyTo + " registered " + replies.keySet()
                        + ", local " + localId + " p = " + p);
                System.err.println(p.destination.equals(localId));
            } else if (p.b == Packets.REPLY_WITH_FAILURE) {
                f.completeExceptionally((Throwable) p.payloadToObject());
            } else {
                ((CompletableFuture) f).complete(p.payloadToObject());
            }
        } else {
            packetRead0(p);
        }
    }

    public synchronized void packetWrite(Packets p) {
        outgoingPacketsQueue.add(p);
    }

    void tryReconnect() {
        if (remoteAddress != null) {// test we are a client

        }
        LOG.info("Trying to reconnect");
    }

    public <T> NetworkFutureImpl<T> withReply(MaritimeId to, byte type, Object o) {
        NetworkFutureImpl<T> f = new NetworkFutureImpl<>();
        long replyId = messageId.incrementAndGet();
        replies.put(replyId, f);
        // System.out.println("registering replyId" + replyId);
        // new Exception().printStackTrace();
        Packets pp = new Packets(type, null, to, replyId, 0, Packets.writeObject(o));
        packetWrite(pp);
        return f;
    }

    @SuppressWarnings("unchecked")
    static <T> T readObject(byte[] bytes) throws Exception {
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        return (T) in.readObject();

    }

    public enum ConnectionState {
        OPEN, SHUTDOWN, TERMINATED;
    }
}
