/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security;

import java.io.File;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArtifactSizeEnforcementFilterTest {

    private ArtifactsDirHolder mockArtifactsDirHolder;
    private SystemEnvironment mockSysEnv;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ArtifactSizeEnforcementFilter artifactSizeEnforcementFilter;
    private FilterChain filterChain;

    @Before
    public void setUp(){
        mockArtifactsDirHolder = mock(ArtifactsDirHolder.class);
        mockSysEnv = mock(SystemEnvironment.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        artifactSizeEnforcementFilter = new ArtifactSizeEnforcementFilter(mockArtifactsDirHolder, mockSysEnv);
        filterChain = mock(FilterChain.class);
        File mockArtifactsDir = mock(File.class);
        when(mockArtifactsDirHolder.getArtifactsDir()).thenReturn(mockArtifactsDir);
        when(mockArtifactsDir.getUsableSpace()).thenReturn(1050000L);
        when(mockSysEnv.getArtifactReposiotryFullLimit()).thenReturn(1L); // This value is in MB
        when(mockSysEnv.getDiskSpaceCacheRefresherInterval()).thenReturn(5000L); // This value is in MB
    }

    @Test
    public void shouldRejectRequestBasedOnArtifactSize() throws IOException, ServletException {
        ArtifactSizeEnforcementFilter artifactSizeEnforcementFilter = new ArtifactSizeEnforcementFilter(mockArtifactsDirHolder, mockSysEnv);

        request.addHeader(HttpService.GO_ARTIFACT_PAYLOAD_SIZE, "713");
        artifactSizeEnforcementFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE));

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void shouldCacheDiskSpaceFor5Seconds() throws Exception {
        File artifactsDir = mock(File.class);
        ArtifactSizeEnforcementFilter artifactSizeEnforcementFilter = new ArtifactSizeEnforcementFilter(mockArtifactsDirHolder, mockSysEnv);
        when(mockArtifactsDirHolder.getArtifactsDir()).thenReturn(artifactsDir);
        when(artifactsDir.getUsableSpace()).thenReturn(30000000L);
        request.addHeader(HttpService.GO_ARTIFACT_PAYLOAD_SIZE, "713");
        artifactSizeEnforcementFilter.doFilter(request, response, filterChain);
        artifactSizeEnforcementFilter.doFilter(request, response, filterChain); //another request within 5seconds
        verify(filterChain, times(2)).doFilter(request, response);
        verify(mockArtifactsDirHolder, times(1)).getArtifactsDir();
    }

    @Test
    public void shouldLetTheRequestPassIfItDoesNotHavePayloadSize() throws IOException, ServletException {
        artifactSizeEnforcementFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
