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
package dk.dma.enav.communication.service;

import java.util.concurrent.TimeUnit;

import test.stubs.HelloService;
import dk.dma.enav.communication.AbstractNetworkTest;
import dk.dma.enav.communication.MaritimeNetworkClient;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractServiceTest extends AbstractNetworkTest {

    public MaritimeNetworkClient registerService(MaritimeNetworkClient pnc, String reply) throws Exception {
        pnc.serviceRegister(HelloService.GET_NAME, HelloService.create(reply)).awaitRegistered(5, TimeUnit.SECONDS);
        return pnc;
    }

}
