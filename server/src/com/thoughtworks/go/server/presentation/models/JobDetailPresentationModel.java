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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.DirectoryReader;
import com.thoughtworks.go.util.TimeConverter;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.TestArtifactPlan.TEST_OUTPUT_FOLDER;
import static com.thoughtworks.go.domain.TestReportGenerator.TEST_RESULTS_FILE;
import static com.thoughtworks.go.server.web.JsonRenderer.render;
import static com.thoughtworks.go.util.ArtifactLogUtil.*;
import static com.thoughtworks.go.util.FileUtil.normalizePath;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.math.NumberUtils.toInt;

public class JobDetailPresentationModel {
    static final String BASE_FILE_URL = "files/";
    protected final JobInstance job;
    protected TimeConverter converter;
    protected JobInstances recent25;
    private final TrackingTool trackingTool;
    private final ArtifactsService artifactsService;
    private final Properties properties;
    protected final AgentConfig buildingAgentConfig;
    private JobIdentifier jobIdentifier;
    private Pipeline pipeline;
    private final Tabs customizedTabs;
    private StageIdentifier stageIdentifier;
    private Stage stage;

    public JobDetailPresentationModel(JobInstance job, JobInstances recent25,
                                      AgentConfig buildingAgentConfig, Pipeline pipeline, Tabs customizedTabs,
                                      TrackingTool trackingTool, ArtifactsService artifactsService,
                                      Properties properties, Stage stage) {
        this.pipeline = pipeline;
        this.customizedTabs = customizedTabs;
        this.job = job;
        this.recent25 = recent25;
        this.trackingTool = trackingTool;
        this.artifactsService = artifactsService;
        this.properties = properties;
        this.stage = stage;
        this.converter = new TimeConverter();
        this.buildingAgentConfig = buildingAgentConfig;
        jobIdentifier = this.job.getIdentifier();
        stageIdentifier = jobIdentifier.getStageIdentifier();
    }

    public String getConsoleoutLocator() {
        return jobIdentifier.artifactLocator("cruise-output/console.log");
    }

    public String getBuildLocator() {
        return jobIdentifier.buildLocator();
    }

    public String getBuildLocatorForDisplay() {
        return jobIdentifier.buildLocatorForDisplay();
    }

    public String getStageLocator() {
        return stageIdentifier.stageLocator();
    }

    public Integer getPipelineCounter() {
        return jobIdentifier.getPipelineCounter();
    }

    public String getPipelineLabel() {
        return jobIdentifier.getPipelineLabel();
    }

    public long getId() {
        return job.getId();
    }

    public String getPipelineName() {
        return jobIdentifier.getPipelineName();
    }

    public String getStageName() {
        return jobIdentifier.getStageName();
    }

    public String getStageCounter() {
        return jobIdentifier.getStageCounter();
    }

    public String getBuildName() {
        return job.getName();
    }

    public String renderArtifactFiles(String requestContext) throws IllegalArtifactLocationException {
        HtmlRenderer renderer = new HtmlRenderer(requestContext);
        getArtifactFiles(new DirectoryReader(jobIdentifier)).render(renderer);
        return renderer.asString();
    }

    public DirectoryEntries getArtifactFiles(final DirectoryReader directoryReader) throws IllegalArtifactLocationException {
        return new DirectoryEntries() {{
            if (!job.isCompleted()) {
                addFolder(CRUISE_OUTPUT_FOLDER).addFile(CONSOLE_LOG_FILE_NAME,
                        artifactsService.findArtifactUrl(jobIdentifier,
                                getConsoleOutputFolderAndFileName()));
            }
            addAll(directoryReader.listEntries(artifactsService.findArtifact(jobIdentifier, ""), ""));
            setIsArtifactsDeleted(stage.isArtifactsDeleted());
        }};
    }

    public ModificationVisitor getModificationSummaries() {
        return new ModificationSummaries(pipeline.getMaterialRevisions());
    }

    public String getMaterialRevisionsJson() {
        MaterialRevisionsJsonBuilder jsonVisitor = new MaterialRevisionsJsonBuilder(trackingTool);
        pipeline.getMaterialRevisions().accept(jsonVisitor);
        return render(jsonVisitor.json());
    }

    public String getModificationTime() {
        return converter.nullSafeDate(pipeline.getModifiedDate());
    }

    public Properties getProperties() {
        return properties;
    }

    public List<JobStatusJsonPresentationModel> getRecent25() {
        List<JobStatusJsonPresentationModel> recent25StatusJson =
                new ArrayList<>();
        for (JobInstance jobInstance : this.recent25) {
            recent25StatusJson.add(new JobStatusJsonPresentationModel(jobInstance));
        }
        return recent25StatusJson;
    }

    public String getStacktrace() {
        return job.getStacktrace();
    }

    public String getBuildError() {
        return job.getBuildError();
    }

    public boolean hasBuildError() {
        return !isEmpty(getBuildError());
    }

    public boolean hasFailedTests() {
        return hasTests() && toInt(getProperties().getValue(TestReportGenerator.FAILED_TEST_COUNT)) > 0;
    }

    public boolean hasServerFailure() {
        return job.getServerFailurePage() != null;
    }

    public boolean hasStacktrace() {
        return !isEmpty(getStacktrace());
    }

    public boolean hasTests() {
        return StringUtils.isNotEmpty(getIndexPageURL());
    }

    //TODO: Fix the places where we return empty strings or nulls
    public String getServerFailurePageURL() {
        File serverFailurePage = job.getServerFailurePage();
        if (serverFailurePage != null) {
            String fullPath = serverFailurePage.getPath();
            return getRestfulUrl(fullPath.substring(fullPath.indexOf(ArtifactLogUtil.CRUISE_OUTPUT_FOLDER)));
        }
        return "";
    }

    public String getRestfulUrl(String path) {
        return BASE_FILE_URL + jobIdentifier.buildLocator() + "/" + normalizePath(path);
    }


    public String getIndexPageURL() {
        File testIndexPage = job.getTestIndexPage();
        if (testIndexPage != null && testIndexPage.getName().equals(TEST_RESULTS_FILE)) {
            return getRestfulUrl(
                    testIndexPage.getPath().substring(testIndexPage.getPath().indexOf(TEST_OUTPUT_FOLDER)));
        }
        return "";
    }

    public boolean isCompleted() {
        return job.getState().isCompleted();
    }

    public boolean isBuilding() {
        return job.getState().isBuilding();
    }

    public boolean isPassed() {
        return job.getResult().isPassed();
    }

    public boolean isFailed() {
        return job.getResult().isFailed();
    }

    public String getResult() {
        return job.getResult().toString().toLowerCase();
    }

    public String getBuildCauseMessage() {
        return pipeline.getBuildCauseMessage();
    }

    public Tabs getCustomizedTabs() {
        return customizedTabs;
    }

    public Stage getStage() {
        return stage;
    }
}
