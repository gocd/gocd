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
package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
    @ValueSource(strings = {
        "/cctray.xml",
        "/api/foo",
        "/remoting/foo",
        "/auth/foo",
        "/plugin/foo/login",
        "/plugin/foo/authenticate",
        "/assets/images/logo.png",
        "/files/build-windows/8531/build-non-server/1/FastTests-runInstance-1/cruise-output/console.log?startLineNumber=0",
        "/console-websocket/build-linux/8839/build-non-server/1/jasmine-rspec?startLine=0",
        "/pipelines/build-windows/8531/build-non-server/1/stats_iframe",
        "/admin/config_change/between/4f0288d15e3855288102dda9ebd45e52/and/233cd2b905cd5fec7435d9fdb72f79b7",
        "/history/stage/Security-Checks/8812/Security-Checks/1?page=5&tab=overview",
    })
    void shouldNotSaveIncomingRequestFromUrls(String url) throws IOException, ServletException {
        assertNotRemembered(HttpRequestBuilder.GET(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/pipelines/Security-Checks/8812/Security-Checks/1/overview.json?stage-history-page=3",
        "/jobStatus.json?pipelineName=Security-Checks&stageName=Security-Checks&jobId=1887084"
    })
    void shouldNotSaveIncomingRequestForJSONUrls(String url) throws IOException, ServletException {
        assertNotRemembered(HttpRequestBuilder.GET(url));
    }

    private void assertNotRemembered(HttpRequestBuilder url) throws IOException, ServletException {
        request = url.build();
        assertThat(SessionUtils.savedRequest(request)).isNull();
        filter.doFilter(request, response, filterChain);
        assertThat(SessionUtils.savedRequest(request)).isNull();
    }

    @ParameterizedTest
    @MethodSource("rememberedUrls")
    void shouldSaveIncomingRequestForAllOtherCalls(String url) throws IOException, ServletException {
        request = HttpRequestBuilder.GET(url).build();
        assertThat(SessionUtils.savedRequest(request)).isNull();
        filter.doFilter(request, response, filterChain);
        assertThat(SessionUtils.savedRequest(request).getRedirectUrl()).isEqualTo("http://test.host/go" + url);
    }

    @ParameterizedTest
    @MethodSource("rememberedUrls")
    void shouldNotRememberPostUrls(String url) throws IOException, ServletException {
        assertNotRemembered(HttpRequestBuilder.POST(url));
    }

    private static Stream<String> rememberedUrls() {
        return Stream.of("/", "/home", "/dashboard", "/foobar");
    }
}
