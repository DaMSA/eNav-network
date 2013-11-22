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
package dk.dma.navnet.messages.c2c.broadcast;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.maritimecloud.broadcast.BroadcastMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;

/**
 * This message is send from the server to the client because the client was in proximity of broadcast that was sent to
 * the server.
 * 
 * @author Kasper Nielsen
 * @see BroadcastSend
 */
public class BroadcastDeliver extends ConnectionMessage {

    final String channel;

    final MaritimeId id;

    final String message;

    final PositionTime positionTime;

    /**
     * @param messageType
     */
    public BroadcastDeliver(MaritimeId id, PositionTime position, String channel, String message) {
        super(MessageType.BROADCAST_DELIVER);
        this.id = requireNonNull(id);
        this.positionTime = requireNonNull(position);
        this.channel = requireNonNull(channel);
        this.message = requireNonNull(message);
    }

    /**
     * @param messageType
     * @throws IOException
     */
    public BroadcastDeliver(TextMessageReader pr) throws IOException {
        super(MessageType.BROADCAST_DELIVER, pr);
        this.id = requireNonNull(MaritimeId.create(pr.takeString()));
        this.positionTime = requireNonNull(PositionTime.create(pr.takeDouble(), pr.takeDouble(), pr.takeLong()));
        this.channel = requireNonNull(pr.takeString());
        this.message = requireNonNull(pr.takeString());
    }

    public BroadcastDeliver cloneIt() {
        return new BroadcastDeliver(id, positionTime, channel, escape(message));
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    @SuppressWarnings("unchecked")
    public Class<BroadcastMessage> getClassFromChannel() throws ClassNotFoundException {
        return (Class<BroadcastMessage>) Class.forName(channel);
    }

    /**
     * @return the id
     */
    public MaritimeId getId() {
        return id;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the positionTime
     */
    public PositionTime getPositionTime() {
        return positionTime;
    }

    public BroadcastMessage tryRead() throws Exception {
        Class<BroadcastMessage> cl = getClassFromChannel();
        ObjectMapper om = new ObjectMapper();
        return om.readValue(getMessage(), cl);
    }

    /** {@inheritDoc} */
    @Override
    protected void write0(TextMessageWriter w) {
        w.writeString(id.toString());
        w.writeDouble(positionTime.getLatitude());
        w.writeDouble(positionTime.getLongitude());
        w.writeLong(positionTime.getTime());
        w.writeString(channel);
        w.writeString(message);
    }

    public static BroadcastDeliver create(MaritimeId sender, PositionTime position, BroadcastMessage message) {
        return new BroadcastDeliver(sender, position, message.channel(), persistAndEscape(message));
    }
}
