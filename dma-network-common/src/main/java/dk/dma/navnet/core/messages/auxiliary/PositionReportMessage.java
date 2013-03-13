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
package dk.dma.navnet.core.messages.auxiliary;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.AbstractMessage;
import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class PositionReportMessage extends AbstractMessage {

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
        this(pr.takeDouble(), pr.takeDouble(), pr.takeLong());
    }

    /**
     * @return the clientId
     */
    public PositionTime getPositionTime() {
        return positionTime;
    }

    /** {@inheritDoc} */
    @Override
    protected void write(TextMessageWriter w) {
        w.writeDouble(positionTime.getLatitude());
        w.writeDouble(positionTime.getLongitude());
        w.writeLong(positionTime.getTime());
    }
}
