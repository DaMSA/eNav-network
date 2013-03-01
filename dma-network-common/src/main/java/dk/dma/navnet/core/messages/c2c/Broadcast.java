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
package dk.dma.navnet.core.messages.c2c;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.AbstractMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.ProtocolReader;
import dk.dma.navnet.core.messages.ProtocolWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class Broadcast extends AbstractMessage {
    final String channel;

    final MaritimeId id;

    final String message;

    final PositionTime positionTime;

    /**
     * @param messageType
     */
    public Broadcast(MaritimeId id, PositionTime position, String channel, String message) {
        super(MessageType.BROADCAST);
        this.id = requireNonNull(id);
        this.positionTime = requireNonNull(position);
        this.channel = requireNonNull(channel);
        this.message = requireNonNull(message);
    }
    /**
     * @param messageType
     * @throws IOException
     */
    public Broadcast(ProtocolReader pr) throws IOException {
        this(MaritimeId.create(pr.takeString()), PositionTime.create(pr.takeDouble(), pr.takeDouble(), pr.takeLong()),
                pr.takeString(), pr.takeString());
    }

    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
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

    /** {@inheritDoc} */
    @Override
    protected void write(ProtocolWriter w) {
        w.writeString(id.toString());
        w.writeDouble(positionTime.getLatitude());
        w.writeDouble(positionTime.getLongitude());
        w.writeLong(positionTime.getTime());
        w.writeString(channel);
        w.writeString(message);
    }
}
