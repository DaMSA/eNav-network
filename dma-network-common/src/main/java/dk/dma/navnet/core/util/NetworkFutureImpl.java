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
package dk.dma.navnet.core.util;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jsr166e.CompletableFuture;
import dk.dma.enav.net.NetworkFuture;

/**
 * 
 * @author Kasper Nielsen
 */
public class NetworkFutureImpl<T> extends CompletableFuture<T> implements NetworkFuture<T> {

    final Class<T> type;

    public NetworkFutureImpl(Class<T> type) {
        this.type = requireNonNull(type);
    }

    /**
     * @return the type
     */
    public Class<T> getType() {
        return type;
    }

    public CompletableFuture<T> timeout(ScheduledExecutorService ses, final TimeUnit unit, final long timeout) {
        final Future<?> f = ses.schedule(new Runnable() {
            public void run() {
                if (!isDone()) {
                    completeExceptionally(new TimeoutException("Timed out after " + timeout + " "
                            + unit.toString().toLowerCase()));
                }
            }
        }, timeout, unit);

        // peek(new BiConsumer<T, Throwable>() {
        // public void apply(T t, Throwable throwable) {
        // f.cancel(false);
        // }
        // });
        // The peek method could also just take a Runnable. But I see no reason not to take the 2 parameters.
        final CompletableFuture<T> cf = new CompletableFuture<>();
        handle(new BiFun<T, Throwable, Void>() {
            public Void apply(T t, Throwable throwable) {
                if (t != null) {
                    cf.complete(t);
                } else {
                    cf.completeExceptionally(throwable);
                }
                // Users must manually purge if many outstanding tasks
                f.cancel(false);
                return null;
            }
        });
        return cf;

        // // Eller bare schedulere den til hvert sekund?
        // if (ThreadLocalRandom.current().nextInt(10000) == 0) {
        // // purge
        // // cf.runAsync(null)
        // // purge
        // }
        //
        //
    }

    public static <T> NetworkFuture<T> wrap(CompletableFuture<T> future) {
        return new Wrapper<>(future);
    }

    static class Wrapper<T> implements NetworkFuture<T> {
        final CompletableFuture<T> future;

        Wrapper(CompletableFuture<T> future) {
            this.future = requireNonNull(future);
        }

        /** {@inheritDoc} */
        @Override
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        /** {@inheritDoc} */
        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }

        /** {@inheritDoc} */
        @Override
        public T getNow(T valueIfAbsent) {
            return future.getNow(valueIfAbsent);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isDone() {
            return future.isDone();
        }
    }
}
