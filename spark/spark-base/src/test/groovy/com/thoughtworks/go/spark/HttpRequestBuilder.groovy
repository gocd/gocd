/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.spark

import com.google.gson.Gson
import com.thoughtworks.go.spark.mocks.MockHttpServletRequest
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils

import javax.servlet.http.Cookie
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class HttpRequestBuilder {

  private MockHttpServletRequest request
  private static final Gson GSON = new Gson()
  private static final CONTEXT_PATH = "/go"

  HttpRequestBuilder() {
    this.request = new MockHttpServletRequest()
  }

  HttpRequestBuilder withPath(String path) {
    URIBuilder uri = new URIBuilder(CONTEXT_PATH + path)
    request.serverName = 'test.host'
    request.contextPath = CONTEXT_PATH
    request.parameters = splitQuery(uri)
    request.requestURI = uri.getPath()
    request.queryString = URLEncodedUtils.format(uri.getQueryParams(), StandardCharsets.UTF_8)
    return this
  }

  MockHttpServletRequest build() {
    return request
  }

  static HttpRequestBuilder GET(String path = '/') {
    def builder = new HttpRequestBuilder().withMethod('GET').withPath(path)
    return builder
  }

  static HttpRequestBuilder HEAD(String path = '/') {
    return new HttpRequestBuilder().withMethod('HEAD').withPath(path)
  }

  static HttpRequestBuilder PUT(String path = '/') {
    return new HttpRequestBuilder().withMethod('PUT').withPath(path)
  }

  static HttpRequestBuilder POST(String path = '/') {
    return new HttpRequestBuilder().withMethod('POST').withPath(path)
  }

  static HttpRequestBuilder PATCH(String path = '/') {
    return new HttpRequestBuilder().withMethod('PATCH').withPath(path)
  }

  static HttpRequestBuilder DELETE(String path = '/') {
    return new HttpRequestBuilder().withMethod('DELETE').withPath(path)
  }

  HttpRequestBuilder withCookies(Cookie... cookies) {
    def existingCookies = request.getCookies() ? Arrays.asList(request.getCookies()) : []
    existingCookies += cookies.toList()
    request.setCookies(existingCookies as Cookie[])
    return this
  }

  HttpRequestBuilder withHeaders(Map<String, String> map) {
    map.forEach { k, v ->
      request.addHeader(k, v)
    }
    return this
  }

  HttpRequestBuilder withBody(String body, Charset charset = StandardCharsets.UTF_8) {
    request.content = body.getBytes(charset)
    return this
  }

  HttpRequestBuilder withChunkedBody(String body, Charset charset = StandardCharsets.UTF_8) {
    request.addHeader("transfer-encoding", "chunked")
    request.contentChunked = body.getBytes(charset)
    return this
  }

  HttpRequestBuilder withJsonBody(String body, Charset charset = StandardCharsets.UTF_8) {
    return withBody(body, charset)
  }

  HttpRequestBuilder withJsonBody(Object body) {
    return withJsonBody(GSON.toJson(body))
  }

  private static Map<String, List<String>> splitQuery(URIBuilder builder) throws UnsupportedEncodingException {
    Map<String, List<String>> params = new LinkedHashMap<String, List<String>>()
    builder.queryParams.each { NameValuePair nvp ->
      if (!params.containsKey(nvp.name)) {
        params.put(nvp.name, new ArrayList<String>())
      }
      params.get(nvp.name).add(nvp.value)
    }
    return params
  }

  HttpRequestBuilder withMethod(String method) {
    request.method = method.toUpperCase()
    this
  }

  HttpRequestBuilder withSessionAttr(String name, Object value) {
    request.getSession().setAttribute(name, value)
    this
  }


}
