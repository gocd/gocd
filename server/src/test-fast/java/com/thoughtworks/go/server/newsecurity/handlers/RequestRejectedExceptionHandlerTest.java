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
package com.thoughtworks.go.server.newsecurity.handlers;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert.assertThat;

class RequestRejectedExceptionHandlerTest {

    @ParameterizedTest
    @ValueSource(strings = {"/remoting/foo", "/add-on/bar/api/foo", "/api/foo", "/cctray.xml"})
    void shouldHandleUrlsAsApiRequest(String url) throws IOException {
        final MockHttpServletRequest request = HttpRequestBuilder.GET(url)
                .withHeader("Accept", "application/json")
                .build();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestRejectedExceptionHandler().handle(request, response, "Something went wrong", HttpStatus.BAD_REQUEST);

        assertThat(response)
                .isBadRequest()
                .hasBody("{\n" +
                        "  \"message\": \"Something went wrong\"\n" +
                        "}");
    }

    @Test
    void shouldReturn404WithHtmlResponse() throws IOException {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("foo/bar")
                .withHeader("Accept", "application/json")
                .build();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestRejectedExceptionHandler().handle(request, response, "Something went wrong", HttpStatus.BAD_REQUEST);

        assertThat(response)
                .isBadRequest()
                .hasNoBody();
    }
}
