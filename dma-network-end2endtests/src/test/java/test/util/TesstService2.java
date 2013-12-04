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
import dk.dma.enav.maritimecloud.service.spi.Service;
import dk.dma.enav.maritimecloud.service.spi.ServiceInitiationPoint;
import dk.dma.enav.maritimecloud.service.spi.ServiceMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class TesstService2 extends Service {

    /** An initiation point */
    public static final ServiceInitiationPoint<TestInit> TEST_INIT = new ServiceInitiationPoint<>(TestInit.class);

    public static class TestInit extends ServiceMessage<TestReply> {

        private long id;

        private String source;

        private String target;

        final long timestamp = System.nanoTime();

        public TestInit() {}

        public TestInit(long id, String source, String target) {
            this.id = id;
            this.source = requireNonNull(source);
            this.target = requireNonNull(target);
        }

        public long getId() {
            return id;
        }

        public String getSource() {
            return source;
        }

        public String getTarget() {
            return target;
        }

        /**
         * @return the timestamp
         */
        protected long getTimestamp() {
            return timestamp;
        }

        public TestReply reply() {
            return new TestReply(this);
        }

        /**
         * @param id
         *            the id to set
         */
        public void setId(long id) {
            this.id = id;
        }

        /**
         * @param source
         *            the source to set
         */
        public void setSource(String source) {
            this.source = source;
        }

        /**
         * @param target
         *            the target to set
         */
        public void setTarget(String target) {
            this.target = target;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "TestInit [id=" + id + ", source=" + source + ", target=" + target + "]";
        }
    }

    public static class TestReply extends ServiceMessage<Void> {

        private TestInit testInit;

        final transient long timestamp = System.nanoTime();

        public TestReply() {}

        TestReply(TestInit testInit) {
            this.testInit = requireNonNull(testInit);
        }

        /**
         * @return the testInit
         */
        public TestInit getTestInit() {
            return testInit;
        }

        /**
         * @return the timestamp
         */
        protected long getTimestamp() {
            return timestamp;
        }

        /**
         * @param testInit
         *            the testInit to set
         */
        public void setTestInit(TestInit testInit) {
            this.testInit = testInit;
        }

        public String toString() {
            return "reply-" + testInit.toString();
        }
    }
}
