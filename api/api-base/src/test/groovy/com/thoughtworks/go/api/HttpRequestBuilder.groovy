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

package com.thoughtworks.go.api

import com.google.gson.Gson
import com.thoughtworks.go.api.mocks.MockHttpServletRequest
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class HttpRequestBuilder {

  public static final String APPLICATION_JSON = 'application/json'

  private MockHttpServletRequest request
  private static final Gson GSON = new Gson()

  private HttpRequestBuilder() {
    this.request = new MockHttpServletRequest()
  }

  MockHttpServletRequest build() {
    return request
  }

  static HttpRequestBuilder GET(String path = '/') {
    def builder = new HttpRequestBuilder().withMethod('GET')
    URIBuilder uri = new URIBuilder(path)
    builder.request.serverName = 'test.host'
    builder.request.parameters = splitQuery(uri)
    builder.request.requestURI = uri.getPath()
    builder.request.queryString = URLEncodedUtils.format(uri.getQueryParams(), StandardCharsets.UTF_8)
    return builder
  }

  static HttpRequestBuilder HEAD(String path = '/') {
    return GET(path).withMethod('HEAD')
  }

  static HttpRequestBuilder PUT(String path = '/') {
    return GET(path).withMethod('PUT')
  }

  static HttpRequestBuilder POST(String path = '/') {
    return GET(path).withMethod('POST')
  }

  static HttpRequestBuilder PATCH(String path = '/') {
    return GET(path).withMethod('PATCH')
  }

  static HttpRequestBuilder DELETE(String path = '/') {
    return GET(path).withMethod('DELETE')
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

  private HttpRequestBuilder withMethod(String method) {
    request.method = method.toUpperCase()
    this
  }

}
