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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.cache.ZipArtifactCache;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConsoleActivityMonitor;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.web.ArtifactFolderViewFactory;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;

import static com.thoughtworks.go.util.GoConstants.CHECKSUM_MULTIPART_FILENAME;
import static com.thoughtworks.go.util.GoConstants.REGULAR_MULTIPART_FILENAME;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ArtifactsControllerTest {

    private ArtifactsController artifactsController;

    private MockHttpServletRequest request;
    private ConsoleActivityMonitor consoleActivityMonitor;
    private RestfulService restfulService;
    private ArtifactsService artifactService;
    private ConsoleService consoleService;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() {
        consoleActivityMonitor = mock(ConsoleActivityMonitor.class);

        restfulService = mock(RestfulService.class);
        artifactService = mock(ArtifactsService.class);
        consoleService = mock(ConsoleService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        artifactsController = new ArtifactsController(artifactService, restfulService, mock(ZipArtifactCache.class), consoleActivityMonitor, consoleService, systemEnvironment);

        request = new MockHttpServletRequest();
    }

    @Test
    public void shouldUpdateLastConsoleActivityOnConsoleLogPut() throws Exception {
        String content = "Testing:";
        request.setContent(content.getBytes());
        JobIdentifier jobIdentifier = new JobIdentifier("pipeline", 10, "label-10", "stage", "2", "build", 103l);
        when(restfulService.findJob("pipeline", "10", "stage", "2", "build", 103l)).thenReturn(jobIdentifier);
        String path = "cruise-output/console.log";
        File artifactFile = new File("junk");
        when(consoleService.consoleLogFile(jobIdentifier)).thenReturn(artifactFile);
        when(consoleService.updateConsoleLog(eq(artifactFile), any(InputStream.class))).thenReturn(true);
        assertThat(((ResponseCodeView) artifactsController.putArtifact("pipeline", "10", "stage", "2", "build", 103l, path, "agent-id", request).getView()).getStatusCode(), is(HttpServletResponse.SC_OK));
        verify(consoleActivityMonitor).consoleUpdatedFor(jobIdentifier);
    }

    @Test
    public void shouldReturnHttpErrorCodeWhenChecksumFileSaveFails() throws Exception {
        File artifactFile = new File("junk");
        when(artifactService.findArtifact(any(JobIdentifier.class), eq("some-path"))).thenReturn(artifactFile);
        when(artifactService.saveFile(any(File.class), any(InputStream.class), eq(false), eq(1))).thenReturn(true);
        when(artifactService.saveOrAppendFile(any(File.class), any(InputStream.class))).thenReturn(false);

        MockMultipartHttpServletRequest mockMultipartHttpServletRequest = new MockMultipartHttpServletRequest();
        mockMultipartHttpServletRequest.addFile(new MockMultipartFile(REGULAR_MULTIPART_FILENAME, "content".getBytes()));
        mockMultipartHttpServletRequest.addFile(new MockMultipartFile(CHECKSUM_MULTIPART_FILENAME, "checksum-content".getBytes()));

        ModelAndView modelAndView = artifactsController.postArtifact("pipeline-1", "1", "stage-1", "2", "job-1", 122L, "some-path", 1, mockMultipartHttpServletRequest);


        ResponseCodeView view = (ResponseCodeView) modelAndView.getView();
        assertThat(view.getStatusCode(), is(SC_INTERNAL_SERVER_ERROR));
        assertThat(view.getContent(), is("Error saving checksum file for the artifact at path 'some-path'"));
    }

    @Test
    public void shouldFunnelAll_GET_calls() throws Exception {
        final ModelAndView returnVal = new ModelAndView();
        ArtifactsController controller = new ArtifactsController(artifactService, restfulService, mock(ZipArtifactCache.class), consoleActivityMonitor, consoleService, systemEnvironment) {
            @Override ModelAndView getArtifact(String filePath, ArtifactFolderViewFactory folderViewFactory, String pipelineName, String counterOrLabel, String stageName, String stageCounter,
                                               String buildName, String sha, String serverAlias) throws Exception {
                return returnVal;
            }
        };

        assertThat(controller.getArtifactAsHtml("pipeline", "counter", "stage", "2", "job", "file_name", "sha1", null), sameInstance(returnVal));
        assertThat(controller.getArtifactAsZip("pipeline", "counter", "stage", "2", "job", "file_name", "sha1"), sameInstance(returnVal));
        assertThat(controller.getArtifactAsJson("pipeline", "counter", "stage", "2", "job", "file_name", "sha1"), sameInstance(returnVal));
    }

    @Test
    public void shouldReturnBadRequestIfRequiredHeadersAreMissingOnACreateArtifactRequest() throws Exception {
        MultipartHttpServletRequest multipartHttpServletRequest = new MockMultipartHttpServletRequest();

        when(systemEnvironment.isApiSafeModeEnabled()).thenReturn(true);
        ModelAndView modelAndView = artifactsController.postArtifact("pipeline", "invalid-label", "stage", "stage-counter", "job-name", 3L, "file-path", 3, multipartHttpServletRequest);
        ResponseCodeView codeView = (ResponseCodeView) modelAndView.getView();

        assertThat(codeView.getStatusCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(codeView.getContent(), is("Missing required header 'Confirm'"));

    }
}
