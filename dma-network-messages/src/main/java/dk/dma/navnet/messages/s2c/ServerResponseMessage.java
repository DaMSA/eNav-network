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
package dk.dma.navnet.messages.s2c;

import java.io.IOException;

import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ServerResponseMessage extends ConnectionMessage {

    final long messageAck;

    /**
     * @param messageType
     */
    public ServerResponseMessage(MessageType type, long messageAck) {
        super(type);
        this.messageAck = messageAck;
    }

    public ServerResponseMessage(MessageType type, TextMessageReader pr) throws IOException {
        super(type, pr);
        this.messageAck = pr.takeLong();
    }

    public long getMessageAck() {
        return messageAck;
    }

    /** {@inheritDoc} */
    @Override
    protected final void write0(TextMessageWriter w) {
        w.writeLong(messageAck);
        write1(w);
    }

    protected void write1(TextMessageWriter w) {};
}
