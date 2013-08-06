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
package dk.dma.navnet.core.messages.util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * 
 * @author Kasper Nielsen
 */
public class TextMessageWriter {
    public final StringBuilder sb = new StringBuilder();
    boolean notFirst;

    public TextMessageWriter() {
        sb.append("[");
    }

    void checkFirst() {
        if (notFirst) {
            sb.append(", ");
        }
        notFirst = true;
    }

    public TextMessageWriter writeInt(int i) {
        checkFirst();
        sb.append(i);
        return this;
    }

    public TextMessageWriter writeLong(long l) {
        checkFirst();
        sb.append(l);
        return this;
    }

    public TextMessageWriter writeStringArray(String... s) {
        checkFirst();
        sb.append("[");
        for (Iterator<String> iterator = Arrays.asList(s).iterator(); iterator.hasNext();) {
            w(iterator.next());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return this;
    }

    public TextMessageWriter writeDouble(double d) {
        checkFirst();
        // TODO quote string
        sb.append(d);
        return this;
    }

    public TextMessageWriter writeString(String s) {
        checkFirst();
        // TODO escape string
        w(s);
        return this;
    }

    private void w(String s) {
        if (s == null) {
            sb.append("\\\"\"");
        } else {
            sb.append('"').append(s).append('"');
        }
    }
}
