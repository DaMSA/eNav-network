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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;

/**
 * 
 * @author Kasper Nielsen
 */
public class Packets {

    public static final byte WITH_REPLY = 0x40;

    // SERVER <-> CLIENT
    public static final byte CONNECTION_CONNECT = 0x10;
    public static final byte CONNECTION_RECONNECT = 0x11;
    public static final byte CONNECTION_DISCONNECT = 0x12;
    public static final byte CONNECTION_DISCONNECTED = 0x13;
    public static final byte POSITION_REPORT = 0x20;
    public static final byte REGISTER_SERVICE = WITH_REPLY + 0x22;
    public static final byte FIND_SERVICE = WITH_REPLY + 0x23;
    public static final byte FIND_FOR_SHAPE = WITH_REPLY + 0x24;

    public static final byte TERMINATION_ERROR = 0x79;
    // CLIENT <-> CLIENT
    public static final byte REPLY_WITH_SUCCESS = 0x01;
    public static final byte REPLY_WITH_FAILURE = 0x02;

    // Messages from a server to the client

    // Messages that needs to routed

    // client messages
    public static final byte INVOKE_SERVICE = WITH_REPLY + 0x33;

    public final byte b;

    public final MaritimeId destination;
    public final long inReplyTo;
    public final long msgId;
    public final byte[] payload;
    public final MaritimeId src;

    Packets(byte b, MaritimeId src, MaritimeId dst, long msgId, long replyId, byte[] data) {
        this.b = b;
        this.src = src;
        this.destination = dst;
        this.msgId = msgId;
        this.inReplyTo = replyId;
        this.payload = requireNonNull(data);
    }

    @SuppressWarnings("unchecked")
    public <T> T payloadToObject() throws Exception {
        // System.out.println(Bytes.asList(payload));
        if (payload.length == 0) {
            return null;
        }
        ByteArrayInputStream bin = new ByteArrayInputStream(payload);
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        return (T) in.readObject();
    }

    public Packets replyWithFailure(Throwable cause) {
        return new Packets(REPLY_WITH_FAILURE, null, src, 0, msgId, writeObject(cause));
    }

    public Packets replyWith(Object result) {
        return new Packets(REPLY_WITH_SUCCESS, null, src, 0, msgId, writeObject(result));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Packets [b=" + b + ", src=" + src + ", destination=" + destination + ", msgId=" + msgId
                + ", inReplyTo=" + inReplyTo + ", payload=" + Arrays.toString(payload) + "]";
    }

    void write(Connection impl) throws IOException {
        DataOutputStream os = impl.os;
        os.write(b);

        impl.writeObject(src == null ? impl.pc.getLocalID() : src);
        impl.writeObject(destination == null ? impl.pc.getRemoteID() : destination);

        os.writeLong(msgId);
        os.writeLong(inReplyTo);
        os.writeInt(payload.length);
        if (payload.length > 0) {
            os.write(payload);
        }
    }

    static Packets logout() {
        return new Packets(CONNECTION_DISCONNECT, null, null, -1, -1, new byte[0]);
    }

    public static Packets logoutResponse() {
        return new Packets(CONNECTION_DISCONNECTED, null, null, -1, -1, new byte[0]);
    }

    static boolean needsReply(byte b) {
        return (WITH_REPLY & b) != 0;
    }

    public static Packets positionReport(PositionTime pt) {
        return new Packets(POSITION_REPORT, null, null, -1, -1, writeObject(pt));
    }

    static Packets read(Connection impl) throws Exception {
        byte b = impl.is.readByte();
        MaritimeId src = impl.readObject();
        MaritimeId dst = impl.readObject();
        long msgId = impl.is.readLong();
        long inReplyTo = impl.is.readLong();
        int bytes = impl.is.readInt();
        byte[] bb = new byte[bytes];
        if (bytes > 0) {
            impl.is.readFully(bb);
        }
        return new Packets(b, src, dst, msgId, inReplyTo, bb);
    }

    static byte[] writeObject(Object o) {
        if (o == null) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(20000);
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout))) {
                out.writeObject(o);
            }
            return bout.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
