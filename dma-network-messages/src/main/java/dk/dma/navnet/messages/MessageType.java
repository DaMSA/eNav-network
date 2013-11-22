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

import java.util.Map.Entry;
import java.util.TreeMap;

import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.PositionReportMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastAck;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastDeliver;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSendAck;
import dk.dma.navnet.messages.c2c.service.InvokeService;
import dk.dma.navnet.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.messages.errors.ServerRequestError;
import dk.dma.navnet.messages.s2c.service.FindService;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.messages.s2c.service.RegisterService;
import dk.dma.navnet.messages.s2c.service.RegisterServiceResult;

/**
 * The type of messages that can be sent around in the system.
 * 
 * @author Kasper Nielsen
 */
public enum MessageType {
    /* ***************** Auxiliary messages ******** */
    // 0 - 9 : lifecycle, connect/reconnect/disconnect.. keep/alive

    /** This is the first message sent by the server to client. Whenever a Websocket connection has been created. */
    WELCOME(1, WelcomeMessage.class), // 1. message from server 2 client

    /** This is the first message from the client to server. Contains an optional reconnect token. */
    HELLO(2, HelloMessage.class), // 1. message from client 2 server

    /** The final handshake massage from the server, contains the connection id */
    CONNECTED(3, ConnectedMessage.class), // 2. message from server 2 client

    /** A keep alive message sent periodically. Contains current position/time. */
    POSITION_REPORT(9, PositionReportMessage.class),

    // Channel Switched + men er jo naesten det samme som reconnect
    // nej lige saa snart man er connected, starter man med at sende beskeder der
    // Client maa saa vente til den har receivet faerdigt paa den anden hvorefter den
    // lukker den gamle kanal
    // Man kunne ogsaa receive beskeder over begge kanaller.
    // Hvis de har et fortloebende id kan man jo bare smide dublikater vaek

    /* ******************** Communication client<->server ******************* */

    /** Registers a service with server. (client->server) */
    REGISTER_SERVICE(100, RegisterService.class), // throws ServiceRegisterException
    REGISTER_SERVICE_RESULT(101, RegisterServiceResult.class), // just an ack of the service???

    // servicen der skal unregistreres
    UNREGISTER_SERVICE(110, RegisterService.class), //
    UNREGISTER_SERVICE_ACK(111, RegisterServiceResult.class), // throws ServiceUnregisterException

    FIND_SERVICE(120, FindService.class), //
    FIND_SERVICE_ACK(121, FindServiceResult.class), // throws ServiceFindException

    /* Broadcast */

    /** Broadcasts a message (client->server). */
    BROADCAST_SEND(150, BroadcastSend.class), // client->server

    /** Acknowledgment of broadcast message (server->client). */
    BROADCAST_SEND_ACK(151, BroadcastSendAck.class),

    /** Relay of broadcast from server (server->client). */
    BROADCAST_DELIVER(152, BroadcastDeliver.class),

    /** Acknowledgment of successful broadcast for each client (server->client). */
    BROADCAST_DELIVER_ACK(153, BroadcastAck.class),

    /** The standard error message sent for an invalid request from the client */
    REQUEST_ERROR(199, ServerRequestError.class), // <- requestId, int error_code, String message

    /* ******************** Communication client<->client ******************* */

    /* Service invocation */
    /** Invokes a service. */
    SERVICE_INVOKE(200, InvokeService.class), //

    /** The successful result of invoking a service. */
    SERVICE_INVOKE_RESULT(201, InvokeServiceResult.class), //

    /** Invoking a service failed. */
    SERVICE_INVOKE_ERROR(255, ServerRequestError.class);// indeholder lidt additional info taenker jeg

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
