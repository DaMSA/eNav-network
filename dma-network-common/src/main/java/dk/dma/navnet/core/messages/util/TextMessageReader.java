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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * 
 * @author Kasper Nielsen
 */
public class TextMessageReader {

    private final JsonParser jp;

    public TextMessageReader(String message) throws IOException {
        requireNonNull(message);
        JsonFactory jsonF = new JsonFactory();
        jp = jsonF.createJsonParser(message);
        if (jp.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Expected the start of a JSON array, but was '" + jp.getText() + "'");
        }
    }

    public int takeInt() throws IOException {
        if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) {
            throw new IOException("Expected an integer, but was '" + jp.getText() + "'");
        }
        return jp.getIntValue();
    }

    public long takeLong() throws IOException {
        if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) {
            throw new IOException("Expected an long, but was '" + jp.getText() + "'");
        }
        return jp.getLongValue();
    }

    public double takeDouble() throws IOException {
        if (jp.nextToken() != JsonToken.VALUE_NUMBER_FLOAT) {
            throw new IOException("Expected an integer, but was '" + jp.getText() + "'");
        }
        return jp.getDoubleValue();
    }

    public String takeString() throws IOException {
        if (jp.nextToken() != JsonToken.VALUE_STRING) {
            throw new IOException("Expected an String, but was '" + jp.getText() + "'");
        }
        return jp.getText();
    }

    public String[] takeStringArray() throws IOException {
        if (jp.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Expected an String, but was '" + jp.getText() + "'");
        }
        ArrayList<String> result = new ArrayList<>();
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            result.add(jp.getText());
        }
        return result.toArray(new String[result.size()]);
    }
}
