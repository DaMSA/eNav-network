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
package dk.dma.navnet.messages.auxiliary;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;
import dk.dma.navnet.messages.TransportMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class HelloMessage extends TransportMessage {

    // include connection type
    private final MaritimeId clientId;

    private final String clientInfo;

    private final double lat;

    private final double lon;

    private final String reconnectId;

    /**
     * @param messageType
     */
    public HelloMessage(MaritimeId clientId, String clientInfo, String reconnectId, long lastReceivedMessageId,
            double lat, double lon) {
        super(MessageType.HELLO);
        this.clientId = requireNonNull(clientId);
        this.clientInfo = requireNonNull(clientInfo);
        this.reconnectId = reconnectId;
        this.lat = lat;
        this.lon = lon;
    }

    public HelloMessage(TextMessageReader pr) throws IOException {
        this(MaritimeId.create(pr.takeString()), pr.takeString(), pr.takeString(), 123, pr.takeDouble(), pr
                .takeDouble());
    }

    /**
     * @return the clientId
     */
    public MaritimeId getClientId() {
        return clientId;
    }

    /**
     * @return the clientInfo
     */
    public String getClientInfo() {
        return clientInfo;
    }

    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return the lon
     */
    public double getLon() {
        return lon;
    }

    /**
     * @return the reconnectId
     */
    public String getReconnectId() {
        return reconnectId;
    }

    /** {@inheritDoc} */
    @Override
    protected void write(TextMessageWriter w) {
        w.writeString(clientId.toString());
        w.writeString(clientInfo);
        w.writeString(reconnectId);
        w.writeDouble(lat);
        w.writeDouble(lon);
    }
}
