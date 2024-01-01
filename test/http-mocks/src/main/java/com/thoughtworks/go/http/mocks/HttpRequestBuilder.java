/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.http.mocks;

import com.google.gson.Gson;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.nio.charset.Charset;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpRequestBuilder {

    private static final Gson GSON = new Gson();
    private static final String CONTEXT_PATH = "/go";

    private MockHttpServletRequest request;

    public HttpRequestBuilder() {
        this(new MockHttpServletRequest());
    }

    private HttpRequestBuilder(MockHttpServletRequest request) {
        this.request = request;
    }

    public HttpRequestBuilder withPath(String path) {
        try {
            URIBuilder uri = new URIBuilder(path);
            request.setServerName("test.host");
            request.setContextPath(CONTEXT_PATH);
            request.setParameters(splitQuery(uri));
            request.setRequestURI(CONTEXT_PATH + uri.getPath());
            request.setServletPath(uri.getPath());
            if (!uri.getQueryParams().isEmpty()) {
                request.setQueryString(URLEncodedUtils.format(uri.getQueryParams(), UTF_8));
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpRequestBuilder withPathSafe(String path) {
        return withPath(StringUtils.isBlank(path) ? "/" : path);
    }


    public MockHttpServletRequest build() {
        return request;
    }

    private static HttpRequestBuilder create() {
        return new HttpRequestBuilder();
    }

    private static HttpRequestBuilder createChunked() {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public int getContentLength() {
                return 0;
            }
        };
        request.addHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
        return new HttpRequestBuilder(request);
    }

    public static HttpRequestBuilder GET(String path) {
        return create().withMethod("GET").withPathSafe(path);
    }

    public static HttpRequestBuilder HEAD(String path) {
        return create().withMethod("HEAD").withPathSafe(path);
    }

    public static HttpRequestBuilder DELETE(String path) {
        return create().withMethod("DELETE").withPathSafe(path);
    }

    public static HttpRequestBuilder PUT(String path) {
        return create().withMethod("PUT").withPathSafe(path);
    }

    public static HttpRequestBuilder PUTChunked(String path) {
        return createChunked().withMethod("PUT").withPathSafe(path);
    }

    public static HttpRequestBuilder POST(String path) {
        return create().withMethod("POST").withPathSafe(path);
    }

    public static HttpRequestBuilder POSTChunked(String path) {
        return createChunked().withMethod("POST").withPathSafe(path);
    }

    public static HttpRequestBuilder PATCH(String path) {
        return create().withMethod("PATCH").withPathSafe(path);
    }

    public static HttpRequestBuilder PATCHChunked(String path) {
        return createChunked().withMethod("PATCH").withPathSafe(path);
    }

    public HttpRequestBuilder withCookies(Cookie... cookies) {
        List<Cookie> existingCookies = new ArrayList<>();
        final Cookie[] requestCookies = request.getCookies();

        if (requestCookies != null) {
            existingCookies.addAll(Arrays.asList(requestCookies));
        }

        existingCookies.addAll(Arrays.asList(cookies));
        request.setCookies(existingCookies.toArray(new Cookie[0]));
        return this;
    }

    public HttpRequestBuilder withHeaders(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            withHeader(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public HttpRequestBuilder withHeader(String name, Object value) {
        request.addHeader(name, value.toString());
        return this;
    }

    public HttpRequestBuilder withBody(String body, Charset charset) {
        request.setContent(body.getBytes(charset));
        return this;
    }

    public HttpRequestBuilder withBody(String body) {
        return withBody(body, UTF_8);
    }

    public HttpRequestBuilder withJsonBody(Object body) {
        return withBody(GSON.toJson(body));
    }

    private static Map<String, String[]> splitQuery(URIBuilder builder) {
        Map<String, String[]> params = new LinkedHashMap<>();

        for (NameValuePair nameValuePair : builder.getQueryParams()) {
            params.compute(nameValuePair.getName(),
                (key, oldValue) -> oldValue == null ? new String[] {nameValuePair.getValue()} : ArrayUtils.add(oldValue, nameValuePair.getValue()));
        }

        return params;
    }

    public HttpRequestBuilder withMethod(String method) {
        request.setMethod(method.toUpperCase());
        return this;
    }

    public HttpRequestBuilder withSessionAttr(String name, Object value) {
        request.getSession().setAttribute(name, value);
        return this;
    }

    public HttpRequestBuilder withBasicAuth(String username, String password) {
        return withHeader(HttpHeaders.AUTHORIZATION, "basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8)));
    }

    public HttpRequestBuilder withBearerAuth(String token) {
        return withHeader(HttpHeaders.AUTHORIZATION, "bearer " + token);
    }

    public HttpRequestBuilder withSession(HttpSession session) {
        request.setSession(session);
        return this;
    }

    public HttpRequestBuilder withRequestedSessionId(String requestedSessionId) {
        request.setRequestedSessionId(requestedSessionId);
        return this;
    }

    public HttpRequestBuilder withRequestedSessionIdFromSession() {
        return withRequestedSessionId(request.getSession(true).getId());
    }

    public HttpRequestBuilder usingAjax() {
        return withHeader("X-Requested-With", "XMLHttpRequest");
    }
}
