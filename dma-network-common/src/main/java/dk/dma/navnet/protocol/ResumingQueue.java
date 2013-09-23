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
package dk.dma.navnet.protocol;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.TransportMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class ResumingQueue {

    /** A lock held, every time a message is written. */
    private final ReentrantLock lock = new ReentrantLock();

    /** A queue of messages that not yet been acked. */
    private final LinkedBlockingQueue<ConnectionMessage> unacked = new LinkedBlockingQueue<>();

    volatile long nextId = 1;

    void ackUpToIncluding(long id) {
        lock.lock();
        try {
            for (;;) {
                ConnectionMessage m = unacked.peek();
                if (m == null || m.getMessageId() > id) {
                    return;
                }
                unacked.poll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void write(Transport t, TransportMessage message) {
        String msg = message.toJSON();
        lock.lock();
        try {
            if (message instanceof ConnectionMessage) {
                ConnectionMessage m = (ConnectionMessage) message;
                m.setMessageId(nextId++);
                System.out.println("setting nextid= " + nextId);
                unacked.add(m);
            }
            t.doSendTextAsync(msg);
        } finally {
            lock.unlock();
        }
    }

    public void resume(Transport t, long id) {
        lock.lock();
        try {
            ackUpToIncluding(id);
            for (ConnectionMessage m : unacked) {
                String msg = m.toJSON();
                t.doSendTextAsync(msg);
            }
        } finally {
            lock.unlock();
        }
    }
}
