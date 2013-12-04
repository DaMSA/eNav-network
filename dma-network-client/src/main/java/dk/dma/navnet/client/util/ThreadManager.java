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
package dk.dma.navnet.client.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.picocontainer.Startable;

import dk.dma.enav.maritimecloud.ConnectionClosedException;

/**
 * 
 * @author Kasper Nielsen
 */
public class ThreadManager implements Startable {

    /** An {@link ExecutorService} for running various tasks. */
    final ThreadPoolExecutor es = new ThreadPoolExecutor(0, 3, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

    /** A list of all outstanding futures. Is used to cancel each future in case of shutdown. */
    final Set<DefaultConnectionFuture<?>> futures = Collections
            .newSetFromMap(new CustomConcurrentHashMap<DefaultConnectionFuture<?>, Boolean>(
                    CustomConcurrentHashMap.WEAK, CustomConcurrentHashMap.EQUALS, CustomConcurrentHashMap.STRONG,
                    CustomConcurrentHashMap.EQUALS, 0));

    /** A {@link ScheduledExecutorService} for scheduling various tasks. */
    final ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(2);

    public <T> DefaultConnectionFuture<T> create() {
        DefaultConnectionFuture<T> t = new DefaultConnectionFuture<>(getScheduler());

        futures.add(t);
        return t;
    }

    public void execute(Runnable r) {
        es.execute(r);
    }

    /**
     * Returns the scheduled executor.
     * 
     * @return the scheduled executor
     */
    public ScheduledThreadPoolExecutor getScheduler() {
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

    /**
     * @param task
     * @return
     * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
     */
    public <T> Future<T> submit(Callable<T> task) {
        return es.submit(task);
    }
}
