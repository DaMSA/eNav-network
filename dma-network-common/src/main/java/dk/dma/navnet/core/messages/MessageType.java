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
package dk.dma.navnet.core.messages;

import static java.util.Objects.requireNonNull;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
import dk.dma.navnet.core.messages.auxiliary.PositionReportMessage;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;

/**
 * The type of messages that can be sent around in the system.
 * 
 * @author Kasper Nielsen
 */
public enum MessageType {
    /* ***************** Auxiliary messages ******** */
    // 0 - 9 : lifecycle, connect/reconnect/disconnect.. keep/alive
    WELCOME(1, WelcomeMessage.class), // 1. message from server 2 client
    HELLO(2, HelloMessage.class), // 1. message from client 2 server
    CONNECTED(3, ConnectedMessage.class), // 2. message from server 2 client

    // Channel Switched + men er jo naesten det samme som reconnect
    // nej lige saa snart man er connected, starter man med at sende beskeder der
    // Client maa saa vente til den har receivet faerdigt paa den anden hvorefter den
    // lukker den gamle kanal
    // Man kunne ogsaa receive beskeder over begge kanaller.
    // Hvis de har et fortloebende id kan man jo bare smide dublikater vaek

    // TimedOut???, eller er det en close besked

    POSITION_REPORT(8, PositionReportMessage.class), //
    // KEEP_ALIVE(9, KeepAlive.class), //

    /* ******************** Communication client<->server ******************* */
    REGISTER_SERVICE(100, RegisterService.class), // throws ServiceRegisterException
    REGISTER_SERVICE_RESULT(101, RegisterServiceResult.class), //

    // servicen der skal unregistreres
    UNREGISTER_SERVICE(110, RegisterService.class), //
    UNREGISTER_SERVICE_ACK(111, RegisterService.class), // throws ServiceUnregisterException

    FIND_SERVICE(120, FindService.class), //
    FIND_SERVICE_ACK(121, FindServiceResult.class), // throws ServiceFindException

    /* ******************** Communication client<->client ******************* */

    /* Service invocation */
    /** Invokes a service. */
    SERVICE_INVOKE(200, InvokeService.class), //

    /** The successful result of invoking a service. */
    SERVICE_INVOKE_RESULT(201, InvokeServiceResult.class), //

    /** Invoking a service failed. */
    // SERVICE_INVOKE_ERROR(202, InvokeServiceError.class), //

    /* Broadcast */
    /** Broadcasts a message. */
    BROADCAST(210, BroadcastMsg.class);

    final int type;

    final Class<? extends AbstractTextMessage> cl;

    MessageType(int type, Class<? extends AbstractTextMessage> cl) {
        if (type < 1 || type > 255) {
            throw new IllegalArgumentException("type must be 1>= type <=255");
        }
        this.type = type;
        this.cl = requireNonNull(cl);
    }

}
