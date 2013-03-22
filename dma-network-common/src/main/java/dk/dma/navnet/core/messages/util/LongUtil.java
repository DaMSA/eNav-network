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

/**
 * Various {code int} utility methods.
 * 
 * @author Kasper Nielsen
 */
public class LongUtil {

    /**
     * Returns {@link Integer#MAX_VALUE} if the specified long is above {@link Integer#MAX_VALUE}, otherwise it cast the
     * specified long to an int.
     * <p>
     * No check of whether the specified long is positive is made.
     * 
     * @param value
     *            a positive long value
     * @return a saturated cast of the specified long
     */
    public static long saturatedAdd(long x, long y) {
        if (x == 0 || y == 0 || x > 0 ^ y > 0) {
            return x + y;
        } else if (x > 0) {
            return Long.MAX_VALUE - x < y ? Long.MAX_VALUE : x + y;
        } else {
            return Long.MIN_VALUE - x > y ? Long.MIN_VALUE : x + y;
        }
    }
}
