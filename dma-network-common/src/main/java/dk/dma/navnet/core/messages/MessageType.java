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
package dk.dma.navnet.core.messages;

import static java.util.Objects.requireNonNull;

import java.util.Map.Entry;
import java.util.TreeMap;

import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;
import dk.dma.navnet.core.messages.transport.ConnectedMessage;
import dk.dma.navnet.core.messages.transport.HelloMessage;
import dk.dma.navnet.core.messages.transport.PositionReportMessage;
import dk.dma.navnet.core.messages.transport.WelcomeMessage;

/**
 * The type of messages that can be sent around in the system.
 * 
 * @author Kasper Nielsen
 */
public enum MessageType {
    /* Broadcast */
    /** Broadcasts a message. */
    BROADCAST(210, BroadcastMsg.class), // 1. message from server 2 client
    CONNECTED(3, ConnectedMessage.class), // 1. message from client 2 server
    FIND_SERVICE(120, FindService.class), // 2. message from server 2 client
    // 4 OPTIONS
    // Channel Switched + men er jo naesten det samme som reconnect
    // nej lige saa snart man er connected, starter man med at sende beskeder der
    // Client maa saa vente til den har receivet faerdigt paa den anden hvorefter den
    // lukker den gamle kanal
    // Man kunne ogsaa receive beskeder over begge kanaller.
    // Hvis de har et fortloebende id kan man jo bare smide dublikater vaek

    // TimedOut???, eller er det en close besked

    FIND_SERVICE_ACK(121, FindServiceResult.class), //
    // KEEP_ALIVE(9, KeepAlive.class), //

    HELLO(2, HelloMessage.class), // throws ServiceRegisterException
    POSITION_REPORT(8, PositionReportMessage.class), //

    /* ******************** Communication client<->server ******************* */
    REGISTER_SERVICE(100, RegisterService.class), //
    REGISTER_SERVICE_RESULT(101, RegisterServiceResult.class), // throws ServiceUnregisterException

    /* Service invocation */
    /** Invokes a service. */
    SERVICE_INVOKE(200, InvokeService.class), //
    /** The successful result of invoking a service. */
    SERVICE_INVOKE_RESULT(201, InvokeServiceResult.class), // throws ServiceFindException

    /* ******************** Communication client<->client ******************* */

    // servicen der skal unregistreres
    UNREGISTER_SERVICE(110, RegisterService.class), //

    UNREGISTER_SERVICE_ACK(111, RegisterService.class), //

    /** Invoking a service failed. */
    // SERVICE_INVOKE_ERROR(202, InvokeServiceError.class), //

    /* ***************** Auxiliary messages ******** */
    // 0 - 9 : lifecycle, connect/reconnect/disconnect.. keep/alive
    WELCOME(1, WelcomeMessage.class);

    final Class<? extends TransportMessage> cl;

    final int type;

    MessageType(int type, Class<? extends TransportMessage> cl) {
        if (type < 1 || type > 255) {
            throw new IllegalArgumentException("type must be 1>= type <=255");
        }
        this.type = type;
        this.cl = requireNonNull(cl);
    }

    public static Class<? extends TransportMessage> getType(int type) {
        return HelperHolder.TYPES[type].cl;
    }

    /** A little initialization-on-demand holder idiom helper class */
    private static class HelperHolder {
        static MessageType[] TYPES;
        static {
            TreeMap<Integer, MessageType> m = new TreeMap<>();
            for (MessageType mt : MessageType.values()) {
                m.put(mt.type, mt);
            }
            TYPES = new MessageType[m.lastKey() + 1];
            for (Entry<Integer, MessageType> e : m.entrySet()) {
                TYPES[e.getKey()] = e.getValue();
            }
        }
    }
}
