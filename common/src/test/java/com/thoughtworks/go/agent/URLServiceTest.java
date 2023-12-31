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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

public class URLServiceTest {
    private static final String BASE_URL = "http://localhost:9090/go";

    private URLService urlService;
    private JobIdentifier jobIdentifier;

    @BeforeEach
    public void setUp() {
        urlService = new URLService();
        jobIdentifier = new JobIdentifier("pipelineName", -2, "LATEST", "stageName", "LATEST", "buildName", 123L);
    }

    @AfterEach
    public void teardown() {
        new SystemEnvironment().clearProperty(SystemEnvironment.SERVICE_URL);
    }

    @Test
    public void shouldReturnProperDownloadUrl() {
        String downloadUrl1 = urlService.getRestfulArtifactUrl(jobIdentifier, "file");
        String downloadUrl2 = urlService.getRestfulArtifactUrl(jobIdentifier, "/file");
        assertThat(downloadUrl1, is("/files/pipelineName/LATEST/stageName/LATEST/buildName/file"));
        assertThat(downloadUrl1, is(downloadUrl2));
    }

    @Test
    public void shouldReturnProperRestfulUrlOfArtifact() {
        String downloadUrl1 = urlService.getUploadUrlOfAgent(jobIdentifier, "file");
        String downloadUrl2 = urlService.getUploadUrlOfAgent(jobIdentifier, "/file");
        assertThat(downloadUrl1,
                endsWith("/files/pipelineName/LATEST/stageName/LATEST/buildName/file?attempt=1&buildId=123"));
        assertThat(downloadUrl1, endsWith(downloadUrl2));
    }

    @Test
    public void shouldReturnRestfulUrlOfAgentWithAttemptCounter() {
        String uploadUrl1 = urlService.getUploadUrlOfAgent(jobIdentifier, "file", 1);
        assertThat(uploadUrl1,
                endsWith("/files/pipelineName/LATEST/stageName/LATEST/buildName/file?attempt=1&buildId=123"));
    }

    @Test
    public void shouldReturnServerUrlWithSubpath() {
        new SystemEnvironment().setProperty(SystemEnvironment.SERVICE_URL, BASE_URL + "/");
        assertThat(new URLService().serverUrlFor("someSubPath/xyz"), is(BASE_URL + "/someSubPath/xyz"));
    }

}
