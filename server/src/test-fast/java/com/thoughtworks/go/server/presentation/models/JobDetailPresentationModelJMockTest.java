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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.service.ArtifactsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobDetailPresentationModelJMockTest {
    private JobInstance stubJobInstance;
    private JobDetailPresentationModel jobDetailPresenter;
    private ArtifactsService artifactService;
    private JobIdentifier jobIdentifier;

    @BeforeEach
    public void setUp() {
        stubJobInstance = mock(JobInstance.class);
        artifactService = mock(ArtifactsService.class);
        jobIdentifier = new JobIdentifier("pipeline", -1, "1", "stageName", "0", "build", 1L);
        when(stubJobInstance.getName()).thenReturn("build");
        when(stubJobInstance.getId()).thenReturn(1L);
        when(stubJobInstance.getIdentifier()).thenReturn(jobIdentifier);
        Stage stage = StageMother.custom("stageName", stubJobInstance);
        Pipeline pipeline = new Pipeline("pipeline", null, stage);
        pipeline.setId(1L);

        TrackingTool trackingTool = new TrackingTool();
        jobDetailPresenter = new JobDetailPresentationModel(stubJobInstance,
                null, null, pipeline, new Tabs(), trackingTool, artifactService, null);
    }

    @Test
    public void shouldReturnBuildMessageFromPipeline() throws Exception {
        String message = jobDetailPresenter.getBuildCauseMessage();
        assertThat(message, is("Unknown"));
    }
}
