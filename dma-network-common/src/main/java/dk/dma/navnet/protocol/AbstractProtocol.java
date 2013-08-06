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
package dk.dma.navnet.protocol;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractProtocol {

    /** When the latest message was received. */
    volatile long latestReceivedMessage;

    /** When the latest message was send. */
    volatile long latestSendMessage;

    /** A read lock. */
    private final ReentrantLock readLock = new ReentrantLock();

    /** A write lock. */
    private final ReentrantLock writeLock = new ReentrantLock();

    protected final void fullyLock() {
        readLock.lock();
        writeLock.lock();
    }

    protected final void fullyUnlock() {
        writeLock.unlock();
        readLock.unlock();
    }

    protected void messageReceived() {
        latestReceivedMessage = System.nanoTime();
    }

    protected void messageSend() {
        latestSendMessage = System.nanoTime();
    }

    protected final void readLock() {
        readLock.lock();
    }

    protected final void readUnlock() {
        readLock.unlock();
    }

    protected final void writeLock() {
        writeLock.lock();
    }

    protected final void writeUnlock() {
        writeLock.unlock();
    }
}
