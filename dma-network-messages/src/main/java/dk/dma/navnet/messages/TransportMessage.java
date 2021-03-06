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
package dk.dma.navnet.messages;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * A text only message.
 * 
 * @author Kasper Nielsen
 */
public abstract class TransportMessage {

    /** The type of message. */
    private final MessageType messageType;

    /** The raw message that was received or sent. */
    private String rawMessage;

    /**
     * Creates a new AbstractMessage.
     * 
     * @param messageType
     *            the type of message
     * @throws NullPointerException
     *             if the specified message type is null
     */
    protected TransportMessage(MessageType messageType) {
        this.messageType = requireNonNull(messageType);
    }

    /**
     * Returns the message type.
     * 
     * @return the message type
     */
    public final MessageType getMessageType() {
        return messageType;
    }

    public boolean equals(Object other) {
        return other instanceof TransportMessage && Objects.equals(rawMessage, ((TransportMessage) other).rawMessage);
    }

    public int hashCode() {
        return Objects.hashCode(rawMessage);
    }

    public String getReceivedRawMesage() {
        if (rawMessage == null) {
            throw new IllegalStateException();
        }
        return rawMessage;
    }

    public String toJSON() {
        TextMessageWriter w = new TextMessageWriter();
        w.writeInt(getMessageType().type);
        write(w);
        String s = w.sb.append("]").toString();
        return s;
    }

    protected abstract void write(TextMessageWriter w);

    public static TransportMessage parseMessage(String msg) throws IOException {
        TextMessageReader pr = new TextMessageReader(msg);
        int type = pr.takeInt();
        try {
            Class<? extends TransportMessage> cl = MessageType.getType(type);
            TransportMessage message = cl.getConstructor(TextMessageReader.class).newInstance(pr);
            message.rawMessage = msg;// for debugging purposes
            return message;
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }

    protected static String persist(Object o) {
        ObjectMapper om = new ObjectMapper();
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        try {
            return om.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not be persisted", e);
        }
    }

    protected static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    protected static String persistAndEscape(Object o) {
        return escape(persist(o));
    }

    public String toString() {
        return rawMessage == null ? "" : rawMessage;
    }
}
