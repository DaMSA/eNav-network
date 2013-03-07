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
import dk.dma.navnet.core.messages.c2c.Broadcast;
import dk.dma.navnet.core.messages.c2c.InvokeService;
import dk.dma.navnet.core.messages.s2c.AckMessage;
import dk.dma.navnet.core.messages.s2c.FindServices;
import dk.dma.navnet.core.messages.s2c.PositionReportMessage;
import dk.dma.navnet.core.messages.s2c.RegisterService;
import dk.dma.navnet.core.messages.s2c.connection.ConnectedMessage;
import dk.dma.navnet.core.messages.s2c.connection.HelloMessage;
import dk.dma.navnet.core.messages.s2c.connection.WelcomeMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public enum MessageType {

    // 0 - 9 : lifecycle, connect/reconnect/disconnect..
    WELCOME(1, WelcomeMessage.class), // 1. message from server 2 client
    HELLO(2, HelloMessage.class), // 1. message from client 2 server
    CONNECTED(3, ConnectedMessage.class), // 2. message from server 2 client

    // TimedOut???, eller er det en close besked

    POSITION_REPORT(20, PositionReportMessage.class), //

    /* Communication client<->server */

    REGISTER_SERVICE(100, RegisterService.class), //
    UNREGISTER_SERVICE(101, RegisterService.class), //
    FIND_SERVICE(102, FindServices.class), //

    SUBSCRIBE_CHANNEL(110, RegisterService.class), //
    UNSUBSCRIBE_CHANNEL(111, RegisterService.class), //
    ACK(199, AckMessage.class), //

    /* Communication client<->client */
    SERVICE_INVOKE(200, InvokeService.class), //
    BROADCAST(201, Broadcast.class), //

    // BROADCAST(11) {
    // // Har man et topic/channal man broadcaster paa.
    // // f.eks. warning (channel)
    // // SearchAndRescue channel
    // // imo.xxx channels are reserved channels.
    // @Override
    // AbstractTextMessage read(ProtocolReader pr) throws IOException {
    // return new PositionReportMessage(pr.takeDouble(), pr.takeDouble(), pr.takeLong());
    // }
    // },
    //
    // REGISTER_SERVICE(20) {
    // @Override
    // AbstractTextMessage read(ProtocolReader pr) throws IOException {
    // return new ConnectedMessage(pr.takeString());
    // }
    // },
    // ACK(29) {
    // @Override
    // AbstractTextMessage read(ProtocolReader pr) throws IOException {
    // return new ConnectedMessage(pr.takeString());
    // }
    // }
    //
    ;

    final int type;

    final Class<? extends AbstractMessage> cl;

    MessageType(int type, Class<? extends AbstractMessage> cl) {
        this.type = type;
        this.cl = requireNonNull(cl);
    }

}
