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
package dk.dma.navnet.core.messages.transport;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.shore.ServerId;
import dk.dma.navnet.core.messages.TransportMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class WelcomeMessage extends TransportMessage {

    /** The version of the protocol, must be one. */
    private final int protocolVersion;

    private final MaritimeId serverId;

    private final String serverInfo;

    public WelcomeMessage(TextMessageReader pr) throws IOException {
        this(pr.takeInt(), new ServerId(pr.takeString()), pr.takeString());
    }

    /**
     * @param messageType
     */
    public WelcomeMessage(int protocolVersion, MaritimeId serverId, String serverInfo) {
        super(MessageType.WELCOME);
        this.protocolVersion = 1;
        this.serverId = requireNonNull(serverId);
        this.serverInfo = requireNonNull(serverInfo);
    }

    /**
     * @return the protocolVersion
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * @return the serverId
     */
    public MaritimeId getServerId() {
        return serverId;
    }

    /**
     * @return the serverInfo
     */
    public String getServerInfo() {
        return serverInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void write(TextMessageWriter w) {
        w.writeInt(protocolVersion);
        w.writeString(serverId.toString());
        w.writeString(serverInfo);
    }
}
