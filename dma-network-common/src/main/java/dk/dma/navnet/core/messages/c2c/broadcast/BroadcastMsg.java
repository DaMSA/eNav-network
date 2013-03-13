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
package dk.dma.navnet.core.messages.c2c.broadcast;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.communication.broadcast.BroadcastMessage;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.AbstractMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;
import dk.dma.navnet.core.util.JSonUtil;

/**
 * 
 * @author Kasper Nielsen
 */
public class BroadcastMsg extends AbstractMessage {
    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(BroadcastMsg.class);

    final String channel;

    final MaritimeId id;

    final String message;

    final PositionTime positionTime;

    /**
     * @param messageType
     */
    public BroadcastMsg(MaritimeId id, PositionTime position, String channel, String message) {
        super(MessageType.BROADCAST);
        this.id = requireNonNull(id);
        this.positionTime = requireNonNull(position);
        this.channel = requireNonNull(channel);
        this.message = requireNonNull(message);
    }

    public static BroadcastMsg create(MaritimeId sender, PositionTime position, BroadcastMessage message) {
        return new BroadcastMsg(sender, position, message.channel(), JSonUtil.persistAndEscape(message));
    }

    /**
     * @param messageType
     * @throws IOException
     */
    public BroadcastMsg(TextMessageReader pr) throws IOException {
        this(MaritimeId.create(pr.takeString()), PositionTime.create(pr.takeDouble(), pr.takeDouble(), pr.takeLong()),
                pr.takeString(), pr.takeString());
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

    public BroadcastMessage tryRead() {
        try {
            Class<BroadcastMessage> cl = getClassFromChannel();

            ObjectMapper om = new ObjectMapper();
            return om.readValue(getMessage(), cl);
        } catch (Exception e) {
            LOG.error("Exception while trying to deserialize an incoming broadcast message ", e);
            LOG.error(toJSON());
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void write(TextMessageWriter w) {
        w.writeString(id.toString());
        w.writeDouble(positionTime.getLatitude());
        w.writeDouble(positionTime.getLongitude());
        w.writeLong(positionTime.getTime());
        w.writeString(channel);
        w.writeString(message);
    }
}
