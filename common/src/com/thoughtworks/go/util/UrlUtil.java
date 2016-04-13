/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class UrlUtil {

    private static final String UTF_8 = "UTF-8";

    public static String encodeInUtf8(String url) {
        String[] parts = url.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            try {
                builder.append(URLEncoder.encode(part, UTF_8));
            } catch (UnsupportedEncodingException e) {
                bomb(e);
            }
            if (i < parts.length - 1) {
                builder.append('/');
            }
        }
        if (url.endsWith("/")) {
            builder.append('/');
        }
        return builder.toString();
    }

    public static String urlWithQuery(String oldUrl, String paramName, String paramValue) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(oldUrl);
        uriBuilder.addParameter(paramName, paramValue);
        return uriBuilder.toString();
    }

    private static String encode(String query) {
        if (query == null) {
            return null;
        }
        try {
            return URLEncoder.encode(query, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String decode(String query) {
        if (query == null) {
            return null;
        }
        try {
            return URLDecoder.decode(query, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getQueryParamFromUrl(String url, String paramName) {
        try {
            List<NameValuePair> queryParams = new URIBuilder(url).getQueryParams();
            for (NameValuePair pair : queryParams) {
                if (pair.getName().equals(paramName)) {
                    return pair.getValue();
                }
            }
            return StringUtils.EMPTY;
        } catch (URISyntaxException e) {
            return StringUtils.EMPTY;
        }
    }

    public static String concatPath(String baseUrl, String path) {
        StringBuilder builder = new StringBuilder(baseUrl);
        if(!baseUrl.endsWith("/")) {
            builder.append('/');
        }
        builder.append(path);
        return builder.toString();
    }

    private static class QueryTuple {
        private static final String QUERY_SEPERATOR = "&";
        private static final String QUERY_KEY_VAL_SEPERATOR = "=";
        final String key;
        final String val;

        QueryTuple(String key, String val) {
            this.key = key;
            this.val = val;
        }

        static List<QueryTuple> parse(String query) {
            List<QueryTuple> parsed = new ArrayList<>();
            if (StringUtils.isEmpty(query)) {
                return parsed;
            }

            String[] split = query.split(QUERY_SEPERATOR);
            for (String queryFrag : split) {
                String[] queryFragmentSplit = queryFrag.split(QUERY_KEY_VAL_SEPERATOR);
                parsed.add(new QueryTuple(queryFragmentSplit[0], decode(queryFragmentSplit[1])));
            }
            return parsed;
        }

        static String toString(List<QueryTuple> queryTuples) {
            StringBuilder builder = new StringBuilder();
            for (QueryTuple queryTuple : queryTuples) {
                if (builder.length() > 0) {
                    builder.append(QUERY_SEPERATOR);
                }
                builder.append(queryTuple.key).append(QUERY_KEY_VAL_SEPERATOR).append(encode(queryTuple.val));
            }
            return builder.toString();
        }
    }
}
