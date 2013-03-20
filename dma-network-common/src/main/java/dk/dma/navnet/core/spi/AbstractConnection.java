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
package dk.dma.navnet.core.spi;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractConnection {
    protected final ReentrantLock lock = new ReentrantLock();

    volatile AbstractMessageTransport transport;

    final ScheduledExecutorService ses;

    protected AbstractConnection(ScheduledExecutorService ses) {
        this.ses = requireNonNull(ses);
    }

    protected void setTransport(AbstractMessageTransport transport) {
        lock.lock();
        try {
            this.transport = transport;
        } finally {
            lock.unlock();
        }
    }

    protected abstract void handleText(AbstractTextMessage m) throws Exception;

    protected abstract void handleTextReply(AbstractTextMessage m, NetworkFutureImpl<?> f) throws Exception;

}
