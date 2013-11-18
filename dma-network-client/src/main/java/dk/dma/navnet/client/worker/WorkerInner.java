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
package dk.dma.navnet.client.worker;

import static java.util.Objects.requireNonNull;

import java.util.LinkedList;

import dk.dma.navnet.client.connection.ClientTransport;
import dk.dma.navnet.messages.ConnectionMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class WorkerInner {

    private boolean nextIsReceived = false;

    private final LinkedList<OutstandingMessage> unwritten = new LinkedList<>();

    private final LinkedList<OutstandingMessage> written = new LinkedList<>();

    private final LinkedList<ConnectionMessage> received = new LinkedList<>();

    final Worker worker;

    long latestAck = 0;

    long nextSendId;

    long latestReceivedMessageId;

    WorkerInner(Worker worker) {
        this.worker = requireNonNull(worker);
    }

    public void onConnect(long id, boolean isReconnected) {
        while (processNext()) {}
    }

    public void fromQueue(Object o) {
        if (o instanceof OutstandingMessage) {
            unwritten.add((OutstandingMessage) o);
        } else {
            received.add((ConnectionMessage) o);
        }
    }

    /**
     * @return false if there are no messages to process
     */
    boolean processNext() {
        boolean nextIsReceived = this.nextIsReceived;
        this.nextIsReceived = !nextIsReceived;
        if (nextIsReceived) {
            if (received.size() > 0) {
                processReceived();
            } else if (unwritten.size() > 0) {
                processWritten();
            } else {
                return false;
            }
        } else {
            if (unwritten.size() > 0) {
                processWritten();
            } else if (received.size() > 0) {
                processReceived();
            } else {
                return false;
            }
        }
        return true;
    }


    private void processReceived() {
        ConnectionMessage cm = received.poll();
        System.out.println("GOT MSG with " + cm.getLatestReceivedId() + " " + cm.toJSON());
        latestReceivedMessageId = cm.getMessageId();
        latestAck = cm.getLatestReceivedId();
        worker.connection.getBus().onMsg(cm);
        for (;;) {
            OutstandingMessage m = written.peek();
            if (m == null || m.id > latestAck) {
                return;
            }
            m.acked().complete(null);
            written.poll();
        }
    }

    private void processWritten() {
        ClientTransport transport = worker.connection.getTransport();
        if (transport != null) {
            OutstandingMessage om = unwritten.poll();
            ConnectionMessage cm = om.cm;
            om.id = ++nextSendId;
            cm.setMessageId(om.id);
            cm.setLatestReceivedId(latestReceivedMessageId);
            String message = cm.toJSON();
            written.add(om);
            transport.sendText(message);
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignore) {}
        }
    }
}
