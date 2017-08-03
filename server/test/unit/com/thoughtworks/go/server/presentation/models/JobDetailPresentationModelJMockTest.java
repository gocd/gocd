/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

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
    public void shouldReturnBuildMessageFromPipeline() throws Exception {
        String message = jobDetailPresenter.getBuildCauseMessage();
        assertThat(message, is("Unknown"));
    }
}