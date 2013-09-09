//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common.frames;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.jetty.websocket.api.ProtocolException;
import org.eclipse.jetty.websocket.common.WebSocketFrame;

public abstract class ControlFrame2 extends WebSocketFrame {
    /** Maximum size of Control frame, per RFC 6455 */
    public static final int MAX_CONTROL_PAYLOAD = 125;

    public ControlFrame2(byte opcode) {
        super(opcode);
    }

    public void assertValid() {
        if (isControlFrame()) {
            if (getPayloadLength() > ControlFrame2.MAX_CONTROL_PAYLOAD) {
                throw new ProtocolException("Desired payload length [" + getPayloadLength()
                        + "] exceeds maximum control payload length [" + MAX_CONTROL_PAYLOAD + "]");
            }

            if ((finRsvOp & 0x80) == 0) {
                throw new ProtocolException("Cannot have FIN==false on Control frames");
            }

            if ((finRsvOp & 0x40) != 0) {
                throw new ProtocolException("Cannot have RSV1==true on Control frames");
            }

            if ((finRsvOp & 0x20) != 0) {
                throw new ProtocolException("Cannot have RSV2==true on Control frames");
            }

            if ((finRsvOp & 0x10) != 0) {
                throw new ProtocolException("Cannot have RSV3==true on Control frames");
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ControlFrame2 other = (ControlFrame2) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (finRsvOp != other.finRsvOp) {
            return false;
        }
        if (!Arrays.equals(mask, other.mask)) {
            return false;
        }
        if (masked != other.masked) {
            return false;
        }
        return true;
    }

    public boolean isControlFrame() {
        return true;
    }

    @Override
    public boolean isDataFrame() {
        return false;
    }

    @Override
    public WebSocketFrame setPayload(ByteBuffer buf) {
        if (buf == null) {
            data = null;
            return this;
        }
        System.out.println(buf.remaining());
        byte[] bytearr = new byte[buf.remaining()];
        buf.get(bytearr);
        System.out.println(buf.remaining());
        System.out.println(bytesToHex(bytearr));
        if (buf.remaining() > ControlFrame2.MAX_CONTROL_PAYLOAD) {
            throw new ProtocolException("Control Payloads can not exceed 125 bytes in length.");
        }
        return super.setPayload(buf);
    }

    final protected static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F' };

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
