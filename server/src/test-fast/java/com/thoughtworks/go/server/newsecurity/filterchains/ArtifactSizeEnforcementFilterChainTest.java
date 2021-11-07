/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import ch.qos.logback.core.util.FileSize;
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.server.newsecurity.filters.ArtifactSizeEnforcementFilter;
import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert.assertThat;
import static com.thoughtworks.go.server.newsecurity.filterchains.DenyGoCDAccessForArtifactsFilterChainTest.wrap;
import static org.mockito.Mockito.*;

class ArtifactSizeEnforcementFilterChainTest {
    private MockHttpServletResponse response = new MockHttpServletResponse();
    private ArtifactSizeEnforcementFilterChain filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        File artifactsDir = mock(File.class);
        when(artifactsDir.getUsableSpace()).thenReturn(FileSize.valueOf("1GB").getSize());
        ArtifactsDirHolder artifactsDirHolder = mock(ArtifactsDirHolder.class);
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(artifactsDir);
        filter = new ArtifactSizeEnforcementFilterChain(new ArtifactSizeEnforcementFilter(artifactsDirHolder, new SystemEnvironment()));
        filterChain = mock(FilterChain.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/files/bar/foo.zip", "/remoting/files/bar/foo.zip"})
    void shouldDoNothingWhenNoArtifactSizeHeaderPresent(String path) throws IOException, ServletException {
        MockHttpServletRequest request = HttpRequestBuilder.POST(path).build();
        filter.doFilter(request, response, filterChain);

        assertThat(response)
                .isOk();

        verify(filterChain).doFilter(wrap(request), wrap(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/files/bar/foo.zip", "/remoting/files/bar/foo.zip"})
    void shouldAllowIfEnoughDiskSpaceIsAvailable(String path) throws IOException, ServletException {
        MockHttpServletRequest request = HttpRequestBuilder.POST(path).withHeader("X-GO-ARTIFACT-SIZE", FileSize.valueOf("100MB").getSize()).build();

        filter.doFilter(request, response, filterChain);

        assertThat(response)
                .isOk();
        verify(filterChain).doFilter(wrap(request), wrap(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/files/bar/foo.zip", "/remoting/files/bar/foo.zip"})
    void shouldDisallowIfNotEnoughDiskSpaceIsAvailable(String path) throws IOException, ServletException {
        MockHttpServletRequest request = HttpRequestBuilder.POST(path).withHeader("X-GO-ARTIFACT-SIZE", FileSize.valueOf("600MB").getSize()).build();

        filter.doFilter(request, response, filterChain);

        assertThat(response)
                .isEntityTooLarge();

        verify(filterChain, never()).doFilter(request, response);
    }
}
