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
package dk.dma.navnet.messages.c2c;

import java.io.IOException;

import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ClientRelayedMessage extends ConnectionMessage {

    String destination;

    String source;

    /**
     * @param messageType
     */
    public ClientRelayedMessage(MessageType messageType) {
        super(messageType);
    }

    public ClientRelayedMessage(MessageType messageType, TextMessageReader pr) throws IOException {
        super(messageType, pr);
        this.source = pr.takeString();
        this.destination = pr.takeString();
    }

    /**
     * @return the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @return the src
     */
    public String getSource() {
        return source;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    protected final void write0(TextMessageWriter w) {
        w.writeString(source);
        w.writeString(destination);
        write1(w);
    }

    public abstract ClientRelayedMessage cloneIt();

    protected abstract void write1(TextMessageWriter w);
}
