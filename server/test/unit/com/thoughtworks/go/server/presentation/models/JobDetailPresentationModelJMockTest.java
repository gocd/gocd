/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.presentation.models;

import java.io.File;

import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JobDetailPresentationModelJMockTest {
    private ClassMockery context = new ClassMockery();
    private JobInstance stubJobInstance;
    private JobDetailPresentationModel jobDetailPresenter;
    private File testFolder;
    private ArtifactsService artifactService;
    private JobIdentifier jobIdentifier;

    @Before
    public void setUp() {
        stubJobInstance = context.mock(JobInstance.class);
        TrackingTool trackingTool = context.mock(TrackingTool.class);
        artifactService = context.mock(ArtifactsService.class);
        jobIdentifier = new JobIdentifier("pipeline", -1, "1", "stageName", "0", "build", 1L);
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getName();
                will(returnValue("build"));
                allowing(stubJobInstance).getId();
                will(returnValue(1L));
                allowing(stubJobInstance).getIdentifier();
                will(returnValue(jobIdentifier));
            }
        });
        Stage stage = StageMother.custom("stageName", stubJobInstance);
        Pipeline pipeline = new Pipeline("pipeline", null, stage);
        pipeline.setId(1L);

        trackingTool = new TrackingTool();
        jobDetailPresenter = new JobDetailPresentationModel(stubJobInstance,
                null, null, pipeline, new Tabs(), trackingTool, artifactService, new Properties(), null);

        testFolder = TestFileUtil.createTempFolder("testFiles");
    }

    @After
    public void tearDown() throws Exception {
        FileUtil.deleteFolder(testFolder);
    }

    @Test
    public void hasBuildErrorShouldBeFalseWhenEmptyContent() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getBuildError();
                will(returnValue(""));
            }
        });
        assertThat(jobDetailPresenter.hasBuildError(), is(false));

    }

    @Test
    public void hasFailedTestsShouldBeFalseWhenIndexPageURLIsNull() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getTestIndexPage();
                will(returnValue(null));
            }
        });
        assertThat(jobDetailPresenter.hasFailedTests(), is(false));

    }

    @Test
    public void hasBuildErrorShouldBeTrueWhenNonEmptyContent() throws Exception {

        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getBuildError();
                will(returnValue("build cause"));
            }
        });
        assertThat(jobDetailPresenter.hasBuildError(), is(true));
    }

    @Test
    public void hasStackTraceShouldBeFalseWhenEmptyContent() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getStacktrace();
                will(returnValue(""));

            }
        });
        assertThat(jobDetailPresenter.hasStacktrace(), is(false));
    }

    @Test
    public void hasStackTraceShouldBeTrueWhenNonEmptyContent() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getStacktrace();
                will(returnValue("trace"));

            }
        });
        assertThat(jobDetailPresenter.hasStacktrace(), is(true));
    }

    @Test
    public void shouldIndexPage() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getTestIndexPage();
                will(returnValue(new File("home/user/testoutput/result/index.html")));
            }
        });
        String path = jobDetailPresenter.getIndexPageURL();
        assertThat(path, is("files/pipeline/1/stageName/0/build/testoutput/result/index.html"));
    }

    @Test
    public void shouldReturnEmptyStringForIndexPage() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getTestIndexPage();
                will(returnValue((File) null));
            }
        });
        String path = jobDetailPresenter.getIndexPageURL();
        assertThat(path, is(""));
    }


    @Test
    public void shouldGetServerFailurePage() throws Exception {
        final File file = new File(
                "home" + File.separator + "user" + File.separator + ArtifactLogUtil.CRUISE_OUTPUT_FOLDER
                        + File.separator + ArtifactLogUtil.SERVER_FAILURE_PAGE);
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getServerFailurePage();
                will(returnValue(file));
            }
        });
        String path = jobDetailPresenter.getServerFailurePageURL();
        assertThat(path, is("files/pipeline/1/stageName/0/build/" + ArtifactLogUtil.CRUISE_OUTPUT_FOLDER + "/" + ArtifactLogUtil.SERVER_FAILURE_PAGE));
    }


    @Test
    public void shouldReturnEmptyStringForServerFailurePage() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(stubJobInstance).getServerFailurePage();
                will(returnValue((File) null));
            }
        });
        String path = jobDetailPresenter.getServerFailurePageURL();
        assertThat(path, is(""));
    }

    @Test
    public void shouldReturnBuildMessageFromPipeline() throws Exception {
        String message = jobDetailPresenter.getBuildCauseMessage();
        assertThat(message, is("Unknown"));
    }
}