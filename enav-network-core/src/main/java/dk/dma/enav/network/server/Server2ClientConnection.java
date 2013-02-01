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
package dk.dma.enav.network.server;

import static java.util.Objects.requireNonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;

import dk.dma.commons.util.io.CountingInputStream;
import dk.dma.commons.util.io.CountingOutputStream;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.network.Connection;
import dk.dma.enav.network.Packets;
import dk.dma.enav.network.PersistentConnection;

/**
 * 
 * @author Kasper Nielsen
 */
class Server2ClientConnection extends PersistentConnection {
    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(Server2ClientConnection.class);

    private final ENavNetworkServer server;

    /**
     * @param server
     */
    private Server2ClientConnection(ENavNetworkServer server, Executor e, MaritimeId localId, MaritimeId remoteId,
            UUID connectionId, AtomicLong bytesWritten, AtomicLong bytesRead) {
        super(e, localId, remoteId, connectionId, bytesWritten, bytesRead);
        this.server = requireNonNull(server);
    }

    /** {@inheritDoc} */
    @Override
    protected void packetRead0(Packets p) throws Exception {
        server.packetRead(this, p);
    }

    static void connect(ConnectionManager cm, Socket s) {
        AtomicLong bytesWritten = new AtomicLong();
        AtomicLong bytesRead = new AtomicLong();
        try {
            LOG.debug("Starting handshake with " + s.getRemoteSocketAddress());

            DataOutputStream oos = new DataOutputStream(new CountingOutputStream(s.getOutputStream(), bytesWritten));
            DataInputStream ois = new DataInputStream(new CountingInputStream(s.getInputStream(), bytesRead));

            // receive login request
            byte b = ois.readByte();
            if (b == Packets.CONNECTION_CONNECT) {
                MaritimeId id = (MaritimeId) Connection.readObject(ois);
                oos.writeByte(1);
                Connection.writeObject(oos, cm.server.id);
                UUID connectionId = UUID.randomUUID();

                Connection.writeObject(oos, connectionId);
                LOG.debug("Handshake with " + s.getRemoteSocketAddress() + " completed succesfully");

                Server2ClientConnection c = new Server2ClientConnection(cm.server, cm.es, cm.server.id, id,
                        connectionId, bytesWritten, bytesRead);

                c.accept(s, ois, oos);
                cm.connected(c);
            } else {
                oos.writeUTF("Must send either Login_request og Login_reconnect");
                Closeables.closeQuietly(oos);
                Closeables.closeQuietly(ois);
                Closeables.closeQuietly(s);
                throw new IOException("Unexpected connection string " + b);
            }

        } catch (Exception e) {
            LOG.error("Failed to connect to socket", e);
        }

    }
}
