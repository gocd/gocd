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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.cache.ZipArtifactCache;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConsoleActivityMonitor;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.web.ArtifactFolderViewFactory;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.net.HttpURLConnection;

import static com.thoughtworks.go.remote.StandardHeaders.REQUEST_CONFIRM_MODIFICATION;
import static com.thoughtworks.go.util.GoConstants.*;
import static java.net.HttpURLConnection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ArtifactsControllerTest {

    private ArtifactsController artifactsController;

    private MockHttpServletRequest request;
    private ConsoleActivityMonitor consoleActivityMonitor;
    private RestfulService restfulService;
    private ArtifactsService artifactService;
    private ConsoleService consoleService;
    private SystemEnvironment systemEnvironment;
    private JobInstanceDao jobInstanceDao;

    @BeforeEach
    public void setUp() {
        consoleActivityMonitor = mock(ConsoleActivityMonitor.class);

        restfulService = mock(RestfulService.class);
        artifactService = mock(ArtifactsService.class);
        consoleService = mock(ConsoleService.class);
        jobInstanceDao = mock(JobInstanceDao.class);
        systemEnvironment = mock(SystemEnvironment.class);
        artifactsController = new ArtifactsController(artifactService, restfulService, mock(ZipArtifactCache.class), jobInstanceDao, consoleActivityMonitor, consoleService, systemEnvironment);

        request = new MockHttpServletRequest();
    }

    @Test
    public void shouldUpdateLastConsoleActivityOnConsoleLogPut() throws Exception {
        String content = "Testing:";
        request.setContent(content.getBytes());
        JobIdentifier jobIdentifier = new JobIdentifier("pipeline", 10, "label-10", "stage", "2", "build", 103L);
        when(restfulService.findJob("pipeline", "10", "stage", "2", "build", 103L)).thenReturn(jobIdentifier);
        String path = "cruise-output/console.log";
        File artifactFile = new File("junk");
        when(consoleService.consoleLogFile(jobIdentifier)).thenReturn(artifactFile);
        when(consoleService.appendToConsoleLogIoSafe(eq(artifactFile), any())).thenReturn(true);
        assertThat(((ResponseCodeView) artifactsController.putArtifact("pipeline", "10", "stage", "2", "build", 103L, path, "agent-id", request).getView()).getStatusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        verify(consoleActivityMonitor).consoleUpdatedFor(jobIdentifier);
    }

    @Test
    public void testConsoleOutShouldReturnErrorWhenJobHasBeenCompletedAndLogsNotFound() {
        JobIdentifier jobIdentifier = new JobIdentifier("pipeline", 10, "label-10", "stage", "2", "build", 103L);
        when(restfulService.findJob("pipeline", "10", "stage", "2", "build")).thenReturn(jobIdentifier);

        when(jobInstanceDao.isJobCompleted(jobIdentifier)).thenReturn(true);
        when(consoleService.doesLogExist(jobIdentifier)).thenReturn(false);

        ModelAndView view = artifactsController.consoleout("pipeline", "10", "stage", "build", "2", 1L);

        assertThat(view.getView().getContentType()).isEqualTo(RESPONSE_CHARSET);
        assertThat(view.getView()).isInstanceOf(ResponseCodeView.class);
        assertThat(((ResponseCodeView) view.getView()).getContent()).contains("Console log for Job [pipeline/10/stage/2/build/103] is unavailable as it may have been purged by Go or deleted externally");
    }

    @Test
    public void shouldReturnHttpErrorCodeWhenChecksumFileSaveFails() throws Exception {
        File artifactFile = new File("junk");
        JobIdentifier jobIdentifier = new JobIdentifier("pipeline-1", 1, "1", "stage-1", "2", "job-1", 122L);
        when(restfulService.findJob("pipeline-1", "1", "stage-1", "2", "job-1", 122L)).thenReturn(jobIdentifier);
        when(artifactService.findArtifact(any(), eq("some-path"))).thenReturn(artifactFile);
        when(artifactService.saveFile(any(), any(), eq(false), eq(1))).thenReturn(true);
        when(artifactService.saveOrAppendFile(any(), any())).thenReturn(false);

        MockMultipartHttpServletRequest request = newMultiPartRequest();
        request.addFile(new MockMultipartFile(REGULAR_MULTIPART_FILENAME, "content".getBytes()));
        request.addFile(new MockMultipartFile(CHECKSUM_MULTIPART_FILENAME, "checksum-content".getBytes()));

        ModelAndView modelAndView = artifactsController.postArtifact("pipeline-1", "1", "stage-1", "2", "job-1", 122L, "some-path", 1, request);

        ResponseCodeView view = (ResponseCodeView) modelAndView.getView();
        assertThat(view.getStatusCode()).isEqualTo(HTTP_INTERNAL_ERROR);
        assertThat(view.getContent()).isEqualTo("Error saving checksum file for the artifact at path 'some-path'");
    }

    private static MockMultipartHttpServletRequest newMultiPartRequest() {
        MockMultipartHttpServletRequest mockMultipartHttpServletRequest = new MockMultipartHttpServletRequest();
        mockMultipartHttpServletRequest.addHeader(REQUEST_CONFIRM_MODIFICATION, true);
        return mockMultipartHttpServletRequest;
    }

    @Test
    void shouldFailToPostAndPutWhenStageCounterIsNotAPositiveInteger() throws Exception {
        MockMultipartHttpServletRequest request = newMultiPartRequest();

        ModelAndView modelAndView = artifactsController.postArtifact("pipeline-1", "1", "stage-1", "NOT_AN_INTEGER", "job-1", 122L, "some-path", 1, request);
        assertThat(((ResponseCodeView) modelAndView.getView()).getStatusCode()).isEqualTo(HTTP_NOT_FOUND);

        modelAndView = artifactsController.postArtifact("pipeline-1", "1", "stage-1", "-123", "job-1", 122L, "some-path", 1, request);
        assertThat(((ResponseCodeView) modelAndView.getView()).getStatusCode()).isEqualTo(HTTP_NOT_FOUND);

        modelAndView = artifactsController.putArtifact("pipeline-1", "1", "stage-1", "NOT_AN_INTEGER", "job-1", 122L, "some-path", "1", request);
        assertThat(((ResponseCodeView) modelAndView.getView()).getStatusCode()).isEqualTo(HTTP_NOT_FOUND);

        modelAndView = artifactsController.putArtifact("pipeline-1", "1", "stage-1", "-123", "job-1", 122L, "some-path", "1", request);
        assertThat(((ResponseCodeView) modelAndView.getView()).getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    }

    @Test
    void shouldFailToGetConsoleOutWhenStageCounterIsNotAPositiveInteger() {
        ModelAndView modelAndView = artifactsController.consoleout("pipeline-1", "1", "stage-1", "job-1", "NOT_AN_INTEGER", 122L);
        assertThat(((ResponseCodeView) modelAndView.getView()).getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    }

    @Test
    void shouldFailToGetArtifactWhenStageCounterIsNotAPositiveInteger() throws Exception {
        ModelAndView modelAndView = artifactsController.getArtifactAsJson("pipeline-1", "1", "stage-1",  "NOT_AN_INTEGER", "job-1", "some-path", "sha1");
        assertThat(((ResponseCodeView) modelAndView.getView()).getStatusCode()).isEqualTo(HTTP_NOT_FOUND);
    }

    @Test
    public void shouldFunnelAll_GET_calls() throws Exception {
        final ModelAndView returnVal = new ModelAndView();
        ArtifactsController controller = new ArtifactsController(artifactService, restfulService, mock(ZipArtifactCache.class), jobInstanceDao, consoleActivityMonitor, consoleService, systemEnvironment) {
            @Override
            ModelAndView getArtifact(String filePath, ArtifactFolderViewFactory folderViewFactory, String pipelineName, String counterOrLabel, String stageName, String stageCounter,
                                     String buildName, String sha) {
                return returnVal;
            }
        };

        assertThat(controller.getArtifactNonFolder("pipeline", "counter", "stage", "2", "job", "file_name", "sha1")).isSameAs(returnVal);
        assertThat(controller.getArtifactAsZip("pipeline", "counter", "stage", "2", "job", "file_name", "sha1")).isSameAs(returnVal);
        assertThat(controller.getArtifactAsJson("pipeline", "counter", "stage", "2", "job", "file_name", "sha1")).isSameAs(returnVal);
    }

    @Test
    public void shouldReturnBadRequestIfRequiredHeadersAreMissingOnACreateArtifactRequest() throws Exception {
        MultipartHttpServletRequest multipartHttpServletRequest = new MockMultipartHttpServletRequest();

        ModelAndView modelAndView = artifactsController.postArtifact("pipeline", "invalid-label", "stage", "stage-counter", "job-name", 3L, "file-path", 3, multipartHttpServletRequest);
        ResponseCodeView codeView = (ResponseCodeView) modelAndView.getView();

        assertThat(codeView.getStatusCode()).isEqualTo(HTTP_BAD_REQUEST);
        assertThat(codeView.getContent()).isEqualTo("Missing required header 'X-GoCD-Confirm'");

    }
}
