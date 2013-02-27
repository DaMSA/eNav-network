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

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * 
 * @author Kasper Nielsen
 */
public class ProtocolReader {

    private final JsonParser jp;

    ProtocolReader(String message) throws IOException {
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
}
