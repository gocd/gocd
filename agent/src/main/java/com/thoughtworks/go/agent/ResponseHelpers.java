/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class ResponseHelpers {
    private ResponseHelpers() {
    }

    public static String readBodyAsString(HttpResponse response) throws IOException {
        try (InputStream responseBody = bodyStream(response)) {
            return IOUtils.toString(responseBody, StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static String readBodyAsStringOrElse(HttpResponse response, String defaultValue) {
        try {
            return readBodyAsString(response);
        } catch (IOException ignored) {
            return defaultValue;
        }
    }

    public static InputStream bodyStream(HttpResponse response) throws IOException {
        final Header encodingHeader = response.getFirstHeader("Content-Encoding");
        final boolean isCompressed = encodingHeader != null &&
                encodingHeader.getValue() != null &&
                encodingHeader.getValue().toLowerCase().contains("gzip");

        return isCompressed ? new GZIPInputStream(response.getEntity().getContent()) :
                response.getEntity().getContent();
    }
}
