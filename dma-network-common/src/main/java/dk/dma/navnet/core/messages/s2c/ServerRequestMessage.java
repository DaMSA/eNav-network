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
package dk.dma.navnet.core.messages.s2c;

import java.io.IOException;

import dk.dma.navnet.core.messages.ConnectionMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ServerRequestMessage<T> extends ConnectionMessage {

    long replyTo;

    public ServerRequestMessage(MessageType messageType, TextMessageReader pr) throws IOException {
        super(messageType, pr);
        this.replyTo = pr.takeLong();
    }

    /**
     * @param messageType
     */
    public ServerRequestMessage(MessageType messageType) {
        super(messageType);
    }

    public void setReplyTo(long replyTo) {
        this.replyTo = replyTo;
    }

    public long getReplyTo() {
        return replyTo;
    }

    /** {@inheritDoc} */
    @Override
    protected final void write0(TextMessageWriter w) {
        w.writeLong(replyTo);
        write1(w);
    }

    protected abstract void write1(TextMessageWriter w);
}
