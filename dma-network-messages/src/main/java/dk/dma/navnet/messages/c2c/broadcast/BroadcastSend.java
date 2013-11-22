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
import dk.dma.enav.maritimecloud.broadcast.BroadcastOptions;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.PositionTimeMessage;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;
import dk.dma.navnet.messages.s2c.ServerRequestMessage;

/**
 * This message is send from the client to server. Unlike {@link BroadcastDeliver} which is the relay message from the
 * server to the clients that need to receive the broadcast.
 * 
 * @author Kasper Nielsen
 */
public class BroadcastSend extends ServerRequestMessage<BroadcastSendAck> implements PositionTimeMessage {

    final String channel;

    final int distance;

    final MaritimeId id;

    final String message;

    final PositionTime positionTime;

    final boolean receiverAck;

    /**
     * @return the receiverAck
     */
    public boolean isReceiverAck() {
        return receiverAck;
    }

    /**
     * @param messageType
     */
    public BroadcastSend(MaritimeId id, PositionTime position, String channel, String message, int distance,
            boolean receiverAck) {
        super(MessageType.BROADCAST_SEND);
        this.id = requireNonNull(id);
        this.positionTime = requireNonNull(position);
        this.channel = requireNonNull(channel);
        this.message = requireNonNull(message);
        this.distance = distance;
        this.receiverAck = receiverAck;
    }

    /**
     * @param messageType
     * @throws IOException
     */
    public BroadcastSend(TextMessageReader pr) throws IOException {
        super(MessageType.BROADCAST_SEND, pr);
        this.id = requireNonNull(MaritimeId.create(pr.takeString()));
        this.positionTime = requireNonNull(PositionTime.create(pr.takeDouble(), pr.takeDouble(), pr.takeLong()));
        this.channel = requireNonNull(pr.takeString());
        this.message = requireNonNull(pr.takeString());
        this.distance = pr.takeInt();
        this.receiverAck = pr.takeBoolean();
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    //
    // public BroadcastSend cloneIt() {
    // return new BroadcastSend(id, positionTime, channel, escape(message));
    // }

    @SuppressWarnings("unchecked")
    public Class<BroadcastMessage> getClassFromChannel() throws ClassNotFoundException {
        return (Class<BroadcastMessage>) Class.forName(channel);
    }

    /**
     * @return the distance
     */
    public int getDistance() {
        return distance;
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

    public <T extends BroadcastMessage> T tryRead(Class<T> type) throws Exception {
        return type.cast(tryRead());
    }

    /** {@inheritDoc} */
    @Override
    protected void write1(TextMessageWriter w) {
        w.writeString(id.toString());
        w.writeDouble(positionTime.getLatitude());
        w.writeDouble(positionTime.getLongitude());
        w.writeLong(positionTime.getTime());
        w.writeString(channel);
        w.writeString(message);
        w.writeInt(distance);
        w.writeBoolean(receiverAck);
    }

    public static BroadcastSend create(MaritimeId sender, PositionTime position, BroadcastMessage message,
            BroadcastOptions options) {
        return new BroadcastSend(sender, position, message.channel(), persistAndEscape(message),
                options.getBroadcastRadius(), options.isReceiverAckEnabled());
    }

    public BroadcastSendAck createReply() {
        return new BroadcastSendAck(getReplyTo());
    }
}
