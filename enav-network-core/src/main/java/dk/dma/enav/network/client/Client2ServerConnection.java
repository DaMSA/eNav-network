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
package dk.dma.enav.network.client;

import static java.util.Objects.requireNonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
class Client2ServerConnection extends PersistentConnection {
    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(Client2ServerConnection.class);

    private final ClientNetwork client;

    /**
     * @param server
     */
    private Client2ServerConnection(ClientNetwork client, Executor e, MaritimeId localId, MaritimeId remoteId,
            SocketAddress remoteAddress, UUID connectionId, AtomicLong bytesWritten, AtomicLong bytesRead) {
        super(e, localId, remoteId, remoteAddress, connectionId, bytesWritten, bytesRead);
        this.client = requireNonNull(client);
    }

    /** {@inheritDoc} */
    @Override
    protected void packetRead0(Packets p) throws Exception {
        client.packetRead0(this, p);
    }

    static PersistentConnection connect(ClientNetwork n, MaritimeId localId, SocketAddress remoteAddress)
            throws IOException {
        AtomicLong bytesWritten = new AtomicLong();
        AtomicLong bytesRead = new AtomicLong();
        LOG.debug("Attempting to connect to " + remoteAddress);
        try {
            Socket s = new Socket();
            s.connect(remoteAddress);

            LOG.debug("Starting handshake with " + s.getRemoteSocketAddress());
            DataOutputStream oos = new DataOutputStream(new CountingOutputStream(s.getOutputStream(), bytesWritten));
            DataInputStream ois = new DataInputStream(new CountingInputStream(s.getInputStream(), bytesRead));

            // Send login request
            oos.writeByte(Packets.CONNECTION_CONNECT);
            Connection.writeObject(oos, localId);

            ois.readByte();// should be login_response
            MaritimeId serverId = (MaritimeId) Connection.readObject(ois);

            UUID connectionId = (UUID) Connection.readObject(ois);

            Client2ServerConnection pc = new Client2ServerConnection(n, n.es, localId, serverId, remoteAddress,
                    connectionId, bytesWritten, bytesRead);
            pc.accept(s, ois, oos);
            LOG.debug("Handshake with " + s.getRemoteSocketAddress() + " completed succesfully");
            return pc;
        } catch (Exception e) {
            LOG.error("Failed to connect to socket", e);
            throw new IOException("Could not connect");
        }

    }
}
