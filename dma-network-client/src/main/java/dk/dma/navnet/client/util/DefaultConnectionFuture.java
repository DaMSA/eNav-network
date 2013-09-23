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

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jsr166e.CompletableFuture;
import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.util.function.BiConsumer;

/**
 * 
 * @author Kasper Nielsen
 */
public class DefaultConnectionFuture<T> extends CompletableFuture<T> implements ConnectionFuture<T> {
    final ScheduledExecutorService ses;

    final String requestId;

    public DefaultConnectionFuture(ScheduledExecutorService ses) {
        this.ses = ses;
        this.requestId = "fixme";
    }

    public DefaultConnectionFuture<T> timeout(final long timeout, final TimeUnit unit) {
        // timeout parameters checked by ses.schedule
        final DefaultConnectionFuture<T> cf = new DefaultConnectionFuture<>(requireNonNull(ses, "executor is null"));
        final Future<?> f;
        try {
            f = ses.schedule(new Runnable() {
                public void run() {
                    if (!isDone()) {
                        cf.completeExceptionally(new TimeoutException("Timed out after " + timeout + " "
                                + unit.toString().toLowerCase()));
                    }
                }
            }, timeout, unit);
        } catch (RejectedExecutionException e) {
            // Unfortunately TimeoutException does not allow exceptions in its constructor
            cf.completeExceptionally(new RuntimeException("Could not scedule task, ", e));
            return cf;
        }
        if (f.isCancelled()) {
            cf.completeExceptionally(new RuntimeException("Could not scedule task"));
        }
        // Check if scheduler is shutdown
        // do it after cf.f is set (reversed in shutdown code)
        handle(new BiFun<T, Throwable, Void>() {
            public Void apply(T t, Throwable throwable) {
                // Users must manually purge if many outstanding tasks
                f.cancel(false);
                if (throwable != null) {
                    cf.completeExceptionally(throwable);
                } else {
                    cf.complete(t);
                }
                return null;
            }
        });
        return cf;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(final BiConsumer<T, Throwable> consumer) {
        requireNonNull(consumer, "consumer is null");
        handle(new BiFun<T, Throwable, Void>() {
            public Void apply(T a, Throwable b) {
                consumer.accept(a, b);
                return null;
            }
        });
    }

}
