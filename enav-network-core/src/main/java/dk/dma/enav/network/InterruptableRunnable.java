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
package dk.dma.enav.network;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 
 * @author Kasper Nielsen
 */
abstract class InterruptableRunnable<T> extends AbstractQueuedSynchronizer implements Runnable {

    /** Per-thread task counter */
    volatile long completedTasks;

    volatile boolean isStopped;

    private final CountDownLatch latch = new CountDownLatch(1);

    private final String name;

    volatile Thread thread;

    InterruptableRunnable(String name) {
        this.name = requireNonNull(name);
    }

    void awaitTermination() throws InterruptedException {
        latch.await();
    }

    protected void finished() {

    }

    void interruptIfStarted() {
        Thread t;
        if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
            try {
                t.interrupt();
            } catch (SecurityException ignore) {}
        }
    }

    protected boolean isHeldExclusively() {
        return getState() != 0;
    }

    boolean isLocked() {
        return isHeldExclusively();
    }

    // Lock methods
    //
    // The value 0 represents the unlocked state.
    // The value 1 represents the locked state.

    boolean isStopped() {
        return isStopped;
    }

    void lock() {
        acquire(1);
    }

    protected abstract void repeat() throws InterruptedException;

    /** {@inheritDoc} */
    @Override
    public final void run() {
        Thread t = thread = Thread.currentThread();
        String tname = t.getName();
        try {
            t.setName(name);
        } catch (SecurityException ignore) {}
        try {
            while (!isStopped) {
                try {
                    repeat();
                } catch (InterruptedException ignore) {}
            }
        } finally {
            try {
                t.setName(tname);
            } catch (SecurityException ignore) {}
            latch.countDown();
            finished();
        }
    }

    synchronized void shutdown() {
        if (!isStopped) {
            isStopped = true;
            Thread t = thread;
            if (t != null && !t.isInterrupted() && tryLock()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {} finally {
                    unlock();
                }
            }
        }
    }

    protected boolean tryAcquire(int unused) {
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }

    boolean tryLock() {
        return tryAcquire(1);
    }

    protected boolean tryRelease(int unused) {
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
    }

    void unlock() {
        release(1);
    }
}
