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
package dk.dma.navnet.server;

import static java.util.Objects.requireNonNull;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.spi.AbstractConnection;
import dk.dma.navnet.core.spi.AbstractHandler;

/**
 * 
 * @author Kasper Nielsen
 */
class ServerHandler extends AbstractHandler {

    final ConnectionManager cm;

    volatile Client holder;

    long nextReplyId;

    State state = State.CREATED;

    final S2CConnection con;

    ServerHandler(ConnectionManager cm) {
        super(cm.ses);
        this.cm = requireNonNull(cm);
        con = new S2CConnection(cm, this);
    }

    /** {@inheritDoc} */
    @Override
    public void connected() {
        sendMessage(new WelcomeMessage(1, cm.server.id, "enavServer/1.0"));
    }

    /** {@inheritDoc} */
    @Override
    protected void closed(int statusCode, String reason) {
        cm.server.tracker.remove(this.holder);
        cm.server.at.disconnected(this);
        super.closed(statusCode, reason);
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable cause) {
        cm.server.tracker.remove(this.holder);
        cm.server.at.disconnected(this);
    }

    enum State {
        CONNECTED, CREATED, DISCONNECTED
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractConnection client() {
        return con;
    }
}
