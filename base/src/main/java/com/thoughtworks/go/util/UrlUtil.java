/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

public class UrlUtil {
    private UrlUtil() {}

    // TODO This encoding implementation is likely not URL compliant - is it a problem?
    public static String encodeInUtf8(String url) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(url.split("/")).forEachOrdered(part -> {
            builder.append(URLEncoder.encode(part, StandardCharsets.UTF_8));
            builder.append('/');
        });

        if (!url.endsWith("/")) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static URL fromString(String url) {
        return fromString(url, RuntimeException::new);
    }

    public static URL fromString(String url, Function<Exception, RuntimeException> exceptionSupplier) {
        try {
            return new URI(url).toURL();
        } catch (Exception e) {
            throw exceptionSupplier.apply(e);
        }
    }

    public static URL fromFile(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param url a fully encoded url string
     * @return Removes all oath/query/fragment/user information from the URL; returning only the root URL with scheme, host and port.
     */
    public static String rootUrlFrom(String url) {
        try {
            final URI uri = new URI(url);
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null).toString();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Url [%s] does not appear to be a valid URL", url), e);
        }
    }
}
