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
package dk.dma.navnet.core.messages;

/**
 * 
 * @author Kasper Nielsen
 */
public class ProtocolWriter {
    final StringBuilder sb = new StringBuilder();
    boolean notFirst;

    ProtocolWriter() {
        sb.append("[");
    }

    void checkFirst() {
        if (notFirst) {
            sb.append(", ");
        }
        notFirst = true;
    }

    public ProtocolWriter writeInt(int i) {
        checkFirst();
        sb.append(i);
        return this;
    }

    public ProtocolWriter writeLong(long l) {
        checkFirst();
        sb.append(l);
        return this;
    }

    public ProtocolWriter writeDouble(double d) {
        checkFirst();
        // TODO quote string
        sb.append(d);
        return this;
    }

    public ProtocolWriter writeString(String s) {
        checkFirst();
        // TODO escape string
        if (s == null) {
            sb.append("\\\"\"");
        } else {
            sb.append('"').append(s).append('"');
        }

        return this;
    }

}
