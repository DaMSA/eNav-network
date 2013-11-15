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
package dk.dma.navnet.messages.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import jsr166e.CompletableFuture;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class ResumingClientQueue {

    /** A queue of messages that not yet been acked. */
    private final LinkedBlockingQueue<OutstandingMessage> unacked = new LinkedBlockingQueue<>(10000);

    volatile long nextId = 1;

    volatile long latestReceived;

    public void messageIn(ConnectionMessage m) {
        latestReceived = m.getMessageId();
        ackUpToIncluding(m.getLatestReceivedId());
    }

    public void ackUpToIncluding(long id) {
        for (;;) {
            OutstandingMessage m = unacked.peek();
            if (m == null || m.id > id) {
                return;
            }
            m.complete(null);
            unacked.poll();
        }
    }

    public List<OutstandingMessage> reConnected(ConnectedMessage m) {
        // System.out.println(unacked2.size());
        ackUpToIncluding(m.getLastReceivedMessageId());
        return new ArrayList<>(unacked);
    }

    public OutstandingMessage write(ConnectionMessage message) {
        long nextId = this.nextId;
        message.setMessageId(nextId); // we need to set the nextId before we can generate the final message text
        message.setLatestReceivedId(latestReceived);

        String text = message.toJSON();
        this.nextId++;// we successfully generated the message now increment

        OutstandingMessage om2 = new OutstandingMessage(message, text);
        unacked.add(om2);
        // System.out.println("SIZE " + unacked2.size());
        return om2;
    }

    // Det er jo naar man har modtaget beskeden.
    // men ikke proceserede den
    public static class OutstandingMessage extends CompletableFuture<Void> {
        ConnectionMessage cm;

        public String msg;

        long id;

        OutstandingMessage(ConnectionMessage cm, String msg) {
            this.cm = cm;
            this.msg = msg;
            id = cm.getMessageId();
        }
    }
}

//
// public void write(Consumer<String> t, ConnectionMessage message) {
// String msg = message.toJSON();
// lock.lock();
// try {
// ConnectionMessage m = message;
// m.setMessageId(nextId++);
// System.out.println("setting nextid= " + nextId);
// t.accept(msg);
// } finally {
// lock.unlock();
// }
// }
