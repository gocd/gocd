/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpRequestBuilder {

    private MockHttpServletRequest request;
    private static final Gson GSON = new Gson();
    private static final String CONTEXT_PATH = "/go";

    public HttpRequestBuilder() {
        this.request = new MockHttpServletRequest();
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

    public MockHttpServletRequest build() {
        return request;
    }

    public static HttpRequestBuilder GET(String path) {
        return new HttpRequestBuilder().withMethod("GET").withPath(StringUtils.isBlank(path) ? "/" : path);
    }

    public static HttpRequestBuilder HEAD(String path) {
        return new HttpRequestBuilder().withMethod("HEAD").withPath(StringUtils.isBlank(path) ? "/" : path);
    }

    public static HttpRequestBuilder PUT(String path) {
        return new HttpRequestBuilder().withMethod("PUT").withPath(StringUtils.isBlank(path) ? "/" : path);
    }

    public static HttpRequestBuilder POST(String path) {
        return new HttpRequestBuilder().withMethod("POST").withPath(StringUtils.isBlank(path) ? "/" : path);
    }

    public static HttpRequestBuilder PATCH(String path) {
        return new HttpRequestBuilder().withMethod("PATCH").withPath(StringUtils.isBlank(path) ? "/" : path);
    }

    public static HttpRequestBuilder DELETE(String path) {
        return new HttpRequestBuilder().withMethod("DELETE").withPath(StringUtils.isBlank(path) ? "/" : path);
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

    public HttpRequestBuilder withChunkedBody(String body, Charset charset) {
        request.addHeader("transfer-encoding", "chunked");
        request.setContentChunked(body.getBytes(charset));
        return this;
    }

    public HttpRequestBuilder withChunkedBody(String body) {
        return withChunkedBody(body, UTF_8);
    }

    public HttpRequestBuilder withJsonBody(String body, Charset charset) {
        return withBody(body, charset);
    }

    public HttpRequestBuilder withJsonBody(String body) {
        return withBody(body, UTF_8);
    }

    public HttpRequestBuilder withJsonBody(Object body) {
        return withJsonBody(GSON.toJson(body));
    }

    private static Map<String, List<String>> splitQuery(URIBuilder builder) {
        Map<String, List<String>> params = new LinkedHashMap<>();

        for (NameValuePair nameValuePair : builder.getQueryParams()) {
            if (!params.containsKey(nameValuePair.getName())) {
                params.put(nameValuePair.getName(), new ArrayList<>());
            }
            params.get(nameValuePair.getName()).add(nameValuePair.getValue());
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

    public HttpRequestBuilder withAttribute(String name, Object value) {
        request.setAttribute(name, value);
        return this;
    }

    public HttpRequestBuilder withBasicAuth(String username, String password) {
        return withHeader("Authorization", "basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8)));
    }

    public HttpRequestBuilder withBearerAuth(String token) {
        return withHeader("Authorization", "bearer " + token);
    }

    public HttpRequestBuilder withFormData(String name, String value) {
        final String existingContentType = request.getHeader("content-type");
        if (existingContentType == null) {
            withHeader("Content-Type", "application/x-www-form-urlencoded");
        }

        if (!request.getHeader("content-type").equals("application/x-www-form-urlencoded")) {
            throw new IllegalStateException("Content-type has already been set to " + existingContentType);
        }

        request.setParameter(name, value);
        return this;
    }

    public HttpRequestBuilder withSession(HttpSession session) {
        request.setSession(session);
        return this;
    }

    public HttpRequestBuilder withX509(X509Certificate[] chain) {
        return withAttribute("javax.servlet.request.X509Certificate", chain);
    }

    public HttpRequestBuilder withRequestedSessionId(String requestedSessionId) {
        request.setRequestedSessionId(requestedSessionId);
        return this;
    }

    public HttpRequestBuilder withRequestedSessionIdFromSession() {
        return withRequestedSessionId(request.getSession(true).getId());
    }

    public HttpRequestBuilder usingAjax(boolean isAjax) {
        return !isAjax ? this : usingAjax();
    }

    public HttpRequestBuilder usingAjax() {
        return withHeader("X-Requested-With", "XMLHttpRequest");
    }

    public HttpRequestBuilder withQueryString(String queryString) {
        request.setQueryString(queryString);
        return this;
    }
}
