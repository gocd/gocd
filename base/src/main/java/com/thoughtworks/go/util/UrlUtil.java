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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.function.Function;

public class UrlUtil {
    private UrlUtil() {}

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

    public static String normalizeUrlString(@NotNull String encodedUrlString) {
        return encodedUrlString.endsWith("/") ? encodedUrlString.substring(0, encodedUrlString.length() - 1) : encodedUrlString;
    }

    public static String joinPathPartsPreEncoded(String left, String right) {
        if (left.endsWith("/")) {
            return left + (right.startsWith("/") ? right.substring(1) : right);
        } else if (!right.startsWith("/")) {
            return left + "/" + right;
        } else {
            return left + right;
        }
    }
}
