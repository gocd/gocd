/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.util.LinkedHashMap;
import java.util.Map;

import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.DirectoryEntries;
import com.thoughtworks.go.domain.FolderDirectoryEntry;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import static com.thoughtworks.go.config.TestArtifactPlan.TEST_OUTPUT_FOLDER;
import static com.thoughtworks.go.server.presentation.models.JobDetailPresentationModel.BASE_FILE_URL;
import static com.thoughtworks.go.server.web.JsonRenderer.render;

import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.util.DirectoryReader;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TimeConverter;

public class StageDetailPresentationModel {
    private final Pipeline pipeline;
    private final Stage stage;
    private final TrackingTool trackingTool;
    private final ArtifactsService artifactsService;
    private final int failedTestCount;
    private TimeConverter converter;

    public StageDetailPresentationModel(Pipeline pipeline, Stage stage, TrackingTool trackingTool, ArtifactsService artifactsService, int count) {
        this.pipeline = pipeline;
        this.stage = stage;
        this.trackingTool = trackingTool;
        this.artifactsService = artifactsService;
        failedTestCount = count;
        this.converter = new TimeConverter();
    }

    public String getStageLocatorForDisplay() {
        return new StageIdentifier(pipeline, stage).stageLocatorForDisplay();
    }

    public long getStageId() {
        return stage.getId();
    }

    public long getStageCounter() {
        return stage.getCounter();
    }

    public String getStageName() {
        return stage.getName();
    }

    public String getPipelineName() {
        return pipeline.getName();
    }

    public long getPipelineId() {
        return pipeline.getId();
    }

    public String getStatus() {
        return stage.stageState().toString().toLowerCase();
    }

    public TimeConverter.ConvertedTime getModificationTime() {
        return this.converter.getConvertedTime(pipeline.getBuildCause().getMaterialRevisions().getDateOfLatestModification());
    }

    public JobInstances getStacktraces() {
        return stage.getJobInstances().withNonEmptyStacktrace();
    }

    public JobInstances getBuildErrors() {
        return stage.getJobInstances().withNonEmptyBuildErrors();
    }

    public Map<JobInstance, String> getIndexPages() {
        JobInstances nonEmptyIndexPages = stage.getJobInstances().withNonEmptyIndexPages();
        Map<JobInstance, String> aggregate = new LinkedHashMap<>();
        for (JobInstance job : nonEmptyIndexPages) {
            JobIdentifier jobIdentifier = new JobIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter()), job.getName(), job.getId());
            String filePath = job.getTestIndexPage().getPath();
            String path = FileUtil.normalizePath(filePath.substring(filePath.indexOf(TEST_OUTPUT_FOLDER)));
            aggregate.put(job, baseArtifactUrl(jobIdentifier, path));
        }
        return aggregate;
    }

    private String baseArtifactUrl(JobIdentifier jobIdentifier, String rest) {
        return BASE_FILE_URL + jobIdentifier.buildLocator() + "/" + rest;
    }

    public DirectoryEntries getArtifactFiles() throws IllegalArtifactLocationException {
        JobInstances withNonEmptyArtifacts = stage.getJobInstances();
        DirectoryEntries artifacts = new DirectoryEntries();
        for (JobInstance instance : withNonEmptyArtifacts) {
            DirectoryReader directoryReader = new DirectoryReader(instance.getIdentifier());
            DirectoryEntries subDirectories =
                    directoryReader.listEntries(artifactsService.findArtifact(instance.getIdentifier(), ""), "");
            artifacts.add(new FolderDirectoryEntry(instance.getName(), "", subDirectories));
        }
        return artifacts;
    }

    public String renderArtifactFiles(String requestContext) throws IllegalArtifactLocationException {
        HtmlRenderer renderer = new HtmlRenderer(requestContext);
        getArtifactFiles().render(renderer);
        return renderer.asString();
    }

    public boolean hasAnyBuildError() {
        return !getBuildErrors().isEmpty();
    }

    public boolean hasAnyStacktrace() {
        return !getStacktraces().isEmpty();
    }

    public boolean hasAnyFailedTest() {
        return failedTestCount>0;
    }

    public Integer getPipelineCounter() {
        return pipeline.getCounter();
    }

    public boolean hasTests() {
        final JobInstances jobInstances = stage.getJobInstances().withNonEmptyIndexPages();
        return jobInstances.size() > 0;
    }

}
