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
package dk.dma.navnet.core.transport.websocket;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import dk.dma.navnet.core.transport.ClientTransportFactory;
import dk.dma.navnet.core.transport.ServerTransportFactory;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractTransportSuite {

    static ClientTransportFactory ctf;
    static ServerTransportFactory stf;

    public static TestSuite create(ClientTransportFactory ctf, ServerTransportFactory stf) {
        AbstractTransportSuite.ctf = ctf;
        AbstractTransportSuite.stf = stf;
        TestSuite ts = new TestSuite();
        ts.addTest(new JUnit4TestAdapter(ClosingTest.class));
        return ts;
    }
}
