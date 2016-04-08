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

package com.thoughtworks.go.util;

import com.thoughtworks.go.domain.JobIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertThat;

public class URLServiceTest {
    private static final String BASE_URL = "http://localhost:9090/go";

    private URLService urlService;
    private JobIdentifier jobIdentifier;

    @Before
    public void setUp() {
        urlService = new URLService();
        jobIdentifier = new JobIdentifier("pipelineName", -2, "LATEST", "stageName", "LATEST", "buildName", 123L);
    }

    @After
    public void teardown() {
        new SystemEnvironment().clearProperty("serviceUrl");
    }

    @Test
    public void shouldReturnRepositoryURLWhenBaseURLIsEndedCorrectly() throws Exception {
        new SystemEnvironment().setProperty("serviceUrl", BASE_URL);
        assertThat(new URLService().getBuildRepositoryURL(), is(BASE_URL + "/remoting/remoteBuildRepository"));
    }

    @Test
    public void shouldReturnRepositoryURLWhenBaseURLIsNotEndedCorrectly() throws Exception {
        new SystemEnvironment().setProperty("serviceUrl", BASE_URL + "/");
        assertThat(new URLService().getBuildRepositoryURL(), is(BASE_URL + "/remoting/remoteBuildRepository"));
    }

    @Test
    public void propertiesURLShouldGoThroughtSecurityCheck() {
        String url = urlService.getPropertiesUrl(jobIdentifier, "failedcount");
        assertThat(url, endsWith("/remoting/properties/pipelineName/LATEST/stageName/LATEST/buildName/failedcount"));
    }

    @Test public void shouldReturnProperDownloadUrl() throws Exception {
        String downloadUrl1 = urlService.getRestfulArtifactUrl(jobIdentifier, "file");
        String downloadUrl2 = urlService.getRestfulArtifactUrl(jobIdentifier, "/file");
        assertThat(downloadUrl1, is("/files/pipelineName/LATEST/stageName/LATEST/buildName/file"));
        assertThat(downloadUrl1, is(downloadUrl2));
    }

    @Test public void shouldReturnProperRestfulUrlOfArtifact() throws Exception {
        String downloadUrl1 = urlService.getUploadUrlOfAgent(jobIdentifier, "file");
        String downloadUrl2 = urlService.getUploadUrlOfAgent(jobIdentifier, "/file");
        assertThat(downloadUrl1,
                endsWith("/files/pipelineName/LATEST/stageName/LATEST/buildName/file?attempt=1&buildId=123"));
        assertThat(downloadUrl1, endsWith(downloadUrl2));
    }

    @Test
    public void shouldReturnRestfulUrlOfAgentWithAttemptCounter() throws Exception {
        String uploadUrl1 = urlService.getUploadUrlOfAgent(jobIdentifier, "file", 1);
        assertThat(uploadUrl1,
                endsWith("/files/pipelineName/LATEST/stageName/LATEST/buildName/file?attempt=1&buildId=123"));
    }

    @Test
    public void shouldReturnServerUrlWithSubpath() {
        new SystemEnvironment().setProperty("serviceUrl", BASE_URL + "/");
        assertThat(new URLService().serverUrlFor("someSubPath/xyz"), is(BASE_URL + "/someSubPath/xyz"));
    }

    @Test
    public void agentRemoteWebSocketUrl() {
        assertThat(urlService.getAgentRemoteWebSocketUrl(), is("wss://localhost:8443/go/agent-websocket"));
    }
}