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

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@UtilityClass
public class UriEncodingUtil {
    /**
     * Encodes a string in paranoid mode; avoiding reserved characters regardless of which part of the URI that char
     * is reserved for. It will encode delimiters like `/`, `?`, `=` etc and ambiguously reserved chars like parentheses.
     * Use this when you don't know which part of the URI a string will go in; and don't mind potentially unnecessary
     * encoding.
     * - This seems most similar to encodeURIComponent within ECMAScript, but is more conservative again, encoding the extra chars from RFC3986
     * - It is probably NOT SAFE to rely on this to encode things which might end up in the URI authority/host etc.
     */
    public static @NotNull String encodePartParanoid(String value) {
        return URLEncoder.encode(value, UTF_8);
    }

    /**
     * Encodes a query parameter as necessary.
     * - It will encode important delimiters like `=`; `+`, '&' etc but will not encode `?`, for example.
     * @see org.springframework.web.util.HierarchicalUriComponents.Type#QUERY_PARAM
     */
    @SuppressWarnings("JavadocReference")
    @SneakyThrows
    public static @NotNull String encodeQueryParam(String value) {
        return UriUtils.encodeQueryParam(value, "UTF-8");
    }

    /**
     * Encodes query parameters as necessary to construct a query string; without the `?` delimiter.
     * @see URLEncodedUtils#format(Iterable, Charset)
     */
    public static @NotNull String encodeQueryParams(Map<String, String> params) {
        List<BasicNameValuePair> queryParams = params.entrySet().stream()
            .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        return URLEncodedUtils.format(queryParams, UTF_8);
    }

    /**
     * Encodes a path segment or part of a path segment as necessary.
     * - It will encode important delimiters like `/`; `?` etc but will not encode `=`, for example.
     * @see org.springframework.web.util.HierarchicalUriComponents.Type#PATH_SEGMENT
     */
    @SuppressWarnings("JavadocReference")
    @SneakyThrows
    public static @NotNull String encodePathSegment(String value) {
        return UriUtils.encodePathSegment(value, "UTF-8");
    }

    /**
     * Encodes a "raw" path, but does not expect it to be a full path; so does not ensure it starts with a `/` nor
     * alter the endings. Generally this is a bad idea, and you should fully encode a path.
     * - It will NOT encode `/`,assuming these are path separators.
     * - It WILL encode `?` so passing a query string to this will lead to bad encoding
     */
    @SneakyThrows
    public static String encodePathPartial(String value) {
        return UriUtils.encodePath(value, "UTF-8");
    }
}
