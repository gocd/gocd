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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.domain.ZippedArtifact;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileModelAndViewTest {
    private MockHttpServletResponse response;
    private File existFile;

    @BeforeEach
    public void setUp() throws Exception {
        response = new MockHttpServletResponse();
        existFile = TestFileUtil.createTempFile("a.log");
        existFile.createNewFile();
    }

    @Test
    public void shouldReturnFileViewWhenSha1IsEmpty() throws Exception {
        FileModelAndView.createFileView(existFile, null);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void shouldReturn304AsStatusCodeWhenSha1IsSameAsProvidedValue() throws Exception {
        ModelAndView modelAndView = FileModelAndView.createFileView(existFile, FileUtil.sha1Digest(existFile));
        modelAndView.getView().render(modelAndView.getModel(), new MockHttpServletRequest(), response);
        assertThat(response.getStatus(), is(304));
    }

    @Test
    public void shouldReturnModelWithZipFlagTurnedOnIfZipIsNeeded() throws Exception {
        ZippedArtifact zippedArtifact = new ZippedArtifact(existFile.getParentFile(), existFile.getName());
        ModelAndView modelAndView = FileModelAndView.createFileView(zippedArtifact, "");
        assertThat(modelAndView.getModel().containsKey(FileView.NEED_TO_ZIP), is(true));
    }

    @Test
    public void shouldReturnModelWithZipFlagTurnedOffIfZipIsNotNeeded() throws Exception {
        ModelAndView modelAndView = FileModelAndView.createFileView(existFile, "");
        assertThat(modelAndView.getModel().containsKey(FileView.NEED_TO_ZIP), is(false));
    }

    @Test
    public void shouldReturnAnErrorMessageForConsoleLogNotFound() throws Exception {
        assertThat(((ResponseCodeView) FileModelAndView.fileNotFound("cruise-output/console.log").getView()).getContent(), is("Console log for this job is unavailable as it may have been purged by Go or "
                + "deleted externally."));
    }

    @Test
    public void shouldReturnAnErrorMessageForNormalFileNotFound() throws Exception {
        assertThat(((ResponseCodeView) FileModelAndView.fileNotFound("bring/sally/up/bring/sally/down").getView()).getContent(), is("Artifact 'bring/sally/up/bring/sally/down' is unavailable as "
                + "it may have been purged by Go or deleted externally."));
    }
}
