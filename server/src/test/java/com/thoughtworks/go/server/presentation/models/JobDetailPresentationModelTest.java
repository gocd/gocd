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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.service.ArtifactsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static com.thoughtworks.go.helper.JobInstanceMother.building;
import static com.thoughtworks.go.helper.StageMother.custom;
import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobDetailPresentationModelTest {

    private DirectoryReader directoryReader;
    private ArtifactsService artifactsService;

    @BeforeEach
    public void setUp() {
        directoryReader = mock(DirectoryReader.class);
        artifactsService = mock(ArtifactsService.class);
    }

    @Test
    public void shouldShowMessage() throws IllegalArtifactLocationException {
        JobInstance jobInstance = JobInstanceMother.completed("job-1");
        Stage stage = new Stage();
        stage.setArtifactsDeleted(true);

        File file = new File(UUID.randomUUID().toString());
        when(artifactsService.findArtifact(jobInstance.getIdentifier(), "")).thenReturn(file);

        DirectoryEntries directoryEntries = new DirectoryEntries();
        when(directoryReader.listEntries(eq(file), any(String.class))).thenReturn(directoryEntries);

        JobDetailPresentationModel jobDetailPresentationModel = new JobDetailPresentationModel(jobInstance, null,
            null, null, null, artifactsService, stage);
        DirectoryEntries artifactFiles = jobDetailPresentationModel.getArtifactFiles(directoryReader);

        assertThat(artifactFiles.isArtifactsDeleted()).isTrue();
    }

    @Test
    public void shouldAddFakeConsoleOutputEntryIfJobIsNotCompleted() throws Exception {
        JobInstance job = building("job");
        JobDetailPresentationModel model = new JobDetailPresentationModel(job, null,
            null, null, null, artifactsService, custom("stage"));

        when(artifactsService.findArtifact(job.getIdentifier(), "")).thenReturn(mock(File.class));
        when(artifactsService.findArtifactUrl(job.getIdentifier(), getConsoleOutputFolderAndFileName())).thenReturn("path/to/console");
        when(directoryReader.listEntries(any(File.class), eq(""))).thenReturn(new DirectoryEntries());

        DirectoryEntries expected = new DirectoryEntries();
        expected.addFolder("cruise-output").addFile("console.log", "path/to/console");

        assertThat(model.getArtifactFiles(directoryReader)).isEqualTo(expected);
    }


    @Test
    public void shouldReturnBuildMessageFromPipeline() {
        JobInstance stubJobInstance = mock(JobInstance.class);
        ArtifactsService artifactService = mock(ArtifactsService.class);
        JobIdentifier jobIdentifier = new JobIdentifier("pipeline", -1, "1", "stageName", "0", "build", 1L);
        when(stubJobInstance.getName()).thenReturn("build");
        when(stubJobInstance.getId()).thenReturn(1L);
        when(stubJobInstance.getIdentifier()).thenReturn(jobIdentifier);
        Stage stage = StageMother.custom("stageName", stubJobInstance);
        Pipeline pipeline = new Pipeline("pipeline", null, stage);
        pipeline.setId(1L);

        TrackingTool trackingTool = new TrackingTool();
        JobDetailPresentationModel jobDetailPresenter = new JobDetailPresentationModel(stubJobInstance,
            null, pipeline, new Tabs(), trackingTool, artifactService, null);
        String message = jobDetailPresenter.getBuildCauseMessage();
        assertThat(message).isEqualTo("Unknown");
    }
}
