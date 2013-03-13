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
 * 
 * @author Kasper Nielsen
 */
public enum MessageType {

    // 0 - 9 : lifecycle, connect/reconnect/disconnect..
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

    POSITION_REPORT(20, PositionReportMessage.class), //

    /* Communication client<->server */
    // AREA, type of recipiants
    REGISTER_SERVICE(100, RegisterService.class), //
    // ok + service registration id
    // registration idet bruges baade til
    // at unregistrere
    REGISTER_SERVICE_ACK(104, RegisterServiceResult.class),

    // servicen der skal unregistreres
    UNREGISTER_SERVICE(101, RegisterService.class), //
    // 00 unregistreret, 01 unknown id
    UNREGISTER_SERVICE_ACK(105, RegisterService.class), //

    FIND_SERVICE(102, FindService.class), //

    FIND_SERVICE_ACK(106, FindServiceResult.class), //

    // id, status code (0 ok, 1

    // SUBSCRIBE_CHANNEL(110, RegisterService.class), //
    // UNSUBSCRIBE_CHANNEL(111, RegisterService.class), //

    /* Communication client<->client */
    SERVICE_INVOKE(200, InvokeService.class), //
    BROADCAST(201, BroadcastMsg.class), //
    SERVICE_INVOKE_ACK(202, InvokeServiceResult.class), //

    // BROADCAST(11) {
    // // Har man et topic/channal man broadcaster paa.
    // // f.eks. warning (channel)
    // // SearchAndRescue channel
    // // imo.xxx channels are reserved channels.

    ;

    final int type;

    final Class<? extends AbstractMessage> cl;

    MessageType(int type, Class<? extends AbstractMessage> cl) {
        this.type = type;
        this.cl = requireNonNull(cl);
    }

}
