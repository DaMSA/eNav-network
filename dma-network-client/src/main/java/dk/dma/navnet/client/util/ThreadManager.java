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
package dk.dma.navnet.client.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.picocontainer.Startable;

import dk.dma.commons.util.concurrent.CustomConcurrentHashMap;
import dk.dma.enav.communication.ConnectionClosedException;

/**
 * 
 * @author Kasper Nielsen
 */
public class ThreadManager implements Startable {

    /** An {@link ExecutorService} for running various tasks. */
    final ThreadPoolExecutor es = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());

    final Set<DefaultConnectionFuture<?>> futures = Collections
            .newSetFromMap(new CustomConcurrentHashMap<DefaultConnectionFuture<?>, Boolean>(
                    CustomConcurrentHashMap.WEAK, CustomConcurrentHashMap.EQUALS, CustomConcurrentHashMap.STRONG,
                    CustomConcurrentHashMap.EQUALS, 0));

    /** A {@link ScheduledExecutorService} for scheduling various tasks. */
    final ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(2);

    public <T> DefaultConnectionFuture<T> create() {
        DefaultConnectionFuture<T> t = new DefaultConnectionFuture<>(getSes());
        futures.add(t);
        return t;

    }

    public void execute(Runnable r) {
        es.execute(r);
    }

    /**
     * @return the ses
     */
    public ScheduledThreadPoolExecutor getSes() {
        return ses;
    }

    /**
     * @param command
     * @param initialDelay
     * @param period
     * @param unit
     * @return
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#scheduleAtFixedRate(java.lang.Runnable, long, long,
     *      java.util.concurrent.TimeUnit)
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return ses.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * @param command
     * @param initialDelay
     * @param delay
     * @param unit
     * @return
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#scheduleWithFixedDelay(java.lang.Runnable, long, long,
     *      java.util.concurrent.TimeUnit)
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return ses.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    /** {@inheritDoc} */
    @Override
    public void start() {}

    /** {@inheritDoc} */
    @Override
    public void stop() {
        es.shutdown();
        ses.shutdown();
        for (DefaultConnectionFuture<?> f : futures) {
            if (!f.isDone()) {
                f.completeExceptionally(new ConnectionClosedException("OOps"));
            }
        }

        for (Runnable r : ses.getQueue()) {
            ScheduledFuture<?> sf = (ScheduledFuture<?>) r;
            sf.cancel(false);
        }
        ses.purge(); // remove all the tasks we just cancelled
        try {
            ses.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            es.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

}
