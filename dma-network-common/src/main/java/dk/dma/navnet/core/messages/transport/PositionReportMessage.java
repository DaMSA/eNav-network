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

import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.ConnectionMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class PositionReportMessage extends ConnectionMessage {

    private final PositionTime positionTime;

    /**
     * @param messageType
     */
    public PositionReportMessage(double lat, double lon, long time) {
        this(PositionTime.create(lat, lon, time));
    }

    public PositionReportMessage(PositionTime position) {
        super(MessageType.POSITION_REPORT);
        this.positionTime = requireNonNull(position);
    }

    public PositionReportMessage(TextMessageReader pr) throws IOException {
        super(MessageType.POSITION_REPORT, pr);
        this.positionTime = PositionTime.create(pr.takeDouble(), pr.takeDouble(), pr.takeLong());
    }

    /**
     * @return the clientId
     */
    public PositionTime getPositionTime() {
        return positionTime;
    }

    /** {@inheritDoc} */
    @Override
    protected void write0(TextMessageWriter w) {
        w.writeDouble(positionTime.getLatitude());
        w.writeDouble(positionTime.getLongitude());
        w.writeLong(positionTime.getTime());
    }
}
