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
package dk.dma.navnet.messages.s2c.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.messages.MessageType;
import dk.dma.navnet.messages.TextMessageReader;
import dk.dma.navnet.messages.TextMessageWriter;
import dk.dma.navnet.messages.s2c.ServerRequestMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class FindService extends ServerRequestMessage<FindServiceResult> {

    final String serviceName;

    final int meters;

    /**
     * @return the meters
     */
    public int getMeters() {
        return meters;
    }

    final int max;

    public FindService(TextMessageReader pr) throws IOException {
        super(MessageType.FIND_SERVICE, pr);
        this.serviceName = requireNonNull(pr.takeString());
        this.meters = pr.takeInt();
        this.max = pr.takeInt();
    }

    /**
     * @param messageType
     */
    public FindService(String serviceName, int meters, int max) {
        super(MessageType.FIND_SERVICE);
        this.serviceName = requireNonNull(serviceName);
        this.meters = meters;
        this.max = max;

    }

    public int getMax() {
        return max;
    }

    public String getServiceName() {
        return serviceName;
    }

    /** {@inheritDoc} */
    @Override
    protected void write1(TextMessageWriter w) {
        w.writeString(serviceName);
        w.writeInt(meters);
        w.writeInt(max);
    }

    public FindServiceResult createReply(String[] array) {
        return new FindServiceResult(getReplyTo(), array);
    }
}
