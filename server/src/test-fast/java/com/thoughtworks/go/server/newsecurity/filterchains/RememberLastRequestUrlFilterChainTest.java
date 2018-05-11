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

package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

class RememberLastRequestUrlFilterChainTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Filter filter;
    private FilterChain filterChain;


    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        filter = new RememberLastRequestUrlFilterChain();
        filterChain = mock(FilterChain.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cctray.xml", "/api/foo", "/remoting/foo", "/agent-websocket/foo", "/auth/foo", "/plugin/foo/login", "/plugin/foo/authenticate", "/assets/images/logo.png", "/server/messages.json"})
    void shouldNotSaveIncomingRequestFromUrls(String url) throws IOException, ServletException {
        request = HttpRequestBuilder.GET(url)
                .build();

        Assertions.assertThat(SessionUtils.savedRequest(request))
                .isNull();
        filter.doFilter(request, response, filterChain);
        Assertions.assertThat(SessionUtils.savedRequest(request))
                .isNull();
    }

    @ParameterizedTest
    @MethodSource("rememberedUrls")
    void shouldSaveIncomingRequestForAllOtherCalls(String url) throws IOException, ServletException {
        request = HttpRequestBuilder.GET(url)
                .build();

        Assertions.assertThat(SessionUtils.savedRequest(request))
                .isNull();
        filter.doFilter(request, response, filterChain);
        Assertions.assertThat(SessionUtils.savedRequest(request).getRedirectUrl())
                .isEqualTo("http://test.host/go" + url);
    }

    @ParameterizedTest
    @MethodSource("rememberedUrls")
    void shouldNotRememberPostUrls(String url) throws IOException, ServletException {
        request = HttpRequestBuilder.POST(url)
                .build();

        Assertions.assertThat(SessionUtils.savedRequest(request))
                .isNull();

        filter.doFilter(request, response, filterChain);


        Assertions.assertThat(SessionUtils.savedRequest(request))
                .isNull();
    }

    private static Stream<String> rememberedUrls() {
        return Stream.of("/", "/home", "/dashboard", "/foobar");
    }
}
