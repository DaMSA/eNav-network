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
package test.util;

import static java.util.Objects.requireNonNull;
import dk.dma.enav.communication.service.spi.Service;
import dk.dma.enav.communication.service.spi.ServiceInitiationPoint;
import dk.dma.enav.communication.service.spi.ServiceMessage;
import dk.dma.enav.model.MaritimeId;

/**
 * 
 * @author Kasper Nielsen
 */
public class TestService extends Service {

    /** An initiation point */
    public static final ServiceInitiationPoint<TestInit> TEST_INIT = new ServiceInitiationPoint<>(TestInit.class);

    public static class TestInit extends ServiceMessage<TestReply> {

        private final long id;

        private final MaritimeId source;

        private final MaritimeId target;

        final long timestamp = System.nanoTime();

        public TestInit(long id, MaritimeId source, MaritimeId target) {
            this.id = id;
            this.source = requireNonNull(source);
            this.target = requireNonNull(target);
        }

        public long getId() {
            return id;
        }

        public MaritimeId getSource() {
            return source;
        }

        public MaritimeId getTarget() {
            return target;
        }

        /**
         * @return the timestamp
         */
        protected long getTimestamp() {
            return timestamp;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "TestInit [id=" + id + ", source=" + source + ", target=" + target + "]";
        }

        public TestReply reply() {
            return new TestReply(this);
        }
    }

    public static class TestReply extends ServiceMessage<Void> {

        private final TestInit testInit;
        final long timestamp = System.nanoTime();

        TestReply(TestInit testInit) {
            this.testInit = requireNonNull(testInit);
        }

        /**
         * @return the name
         */
        public TestInit getInit() {
            return testInit;
        }

        /**
         * @return the timestamp
         */
        protected long getTimestamp() {
            return timestamp;
        }

        public String toString() {
            return "reply-" + testInit.toString();
        }
    }
}
