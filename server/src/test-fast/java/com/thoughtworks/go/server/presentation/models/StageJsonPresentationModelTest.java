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

import com.google.gson.Gson;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.dto.DurationBeans;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.domain.Agent;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.util.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static com.thoughtworks.go.domain.NullStage.createNullStage;
import static com.thoughtworks.go.domain.buildcause.BuildCause.createManualForced;
import static com.thoughtworks.go.helper.ModificationsMother.multipleModifications;
import static com.thoughtworks.go.helper.StageConfigMother.oneBuildPlanWithResourcesAndMaterials;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StageJsonPresentationModelTest {
    private Pipeline pipeline;
    private static final int PIPELINE_ID = 1111;
    private Stage stage;
    private BuildCause modifications;
    private AgentService agentService;

    @Before
    public void setUp() {
        MaterialRevisions materialRevisions = multipleModifications();

        stage = StageMother.withOneScheduledBuild("stage", "job-that-will-fail", "job-that-will-pass", 1);
        modifications = BuildCause.createWithModifications(materialRevisions, "");
        pipeline = new Pipeline("pipeline", PipelineLabel.COUNT_TEMPLATE, modifications,
                new EnvironmentVariables(), stage);
        stage.setIdentifier(new StageIdentifier(pipeline, stage));
        for (JobInstance job : stage.getJobInstances()) {
            job.setIdentifier(new JobIdentifier(pipeline, stage, job));
        }
        pipeline.setId(PIPELINE_ID);
        pipeline.updateCounter(9);
        agentService = mock(AgentService.class);
        when(agentService.findAgentObjectByUuid(any(String.class))).thenReturn(Agent.blankAgent("mocked"));
    }

    @Test
    public void shouldGetAPresenterWithLabelAndRelevantBuildPlansAndPipelineNameAndId() throws Exception {
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage, null, agentService);
        Map json = presenter.toJson();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"pipelineName\": \"pipeline\",\n" +
                "  \"stageName\": \"stage\",\n" +
                "  \"builds\": [\n" +
                "    {\n" +
                "      \"name\": \"job-that-will-fail\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"job-that-will-pass\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"scheduledBuild\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"current_label\": \"10\",\n" +
                "  \"pipelineCounter\": \"10\",\n" +
                "  \"pipelineCounterOrLabel\": \"10\",\n" +
                "  \"id\": \"1\"\n" +
                "}");

        assertFalse("JSON shouldn't contain last_successful_label",
                json.toString().contains("last_successful_label"));
    }

    @Test
    public void shouldGetAPresenterWithLabelAndRelevantBuildPlans() throws Exception {
        DurationBeans durations = new DurationBeans(
                new DurationBean(stage.getJobInstances().getByName("job-that-will-fail").getId(), 12L));

        StageJsonPresentationModel presenter =
                new StageJsonPresentationModel(pipeline, stage, null, agentService, durations, new TrackingTool());
        Map json = presenter.toJson();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"stageName\": \"stage\",\n" +
                "  \"builds\": [\n" +
                "    {\n" +
                "      \"name\": \"job-that-will-fail\",\n" +
                "      \"last_build_duration\": \"12\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"job-that-will-pass\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"scheduledBuild\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"current_label\": \"10\",\n" +
                "  \"id\": \"1\"\n" +
                "}");
        assertFalse("JSON shouldn't contain last_successful_label",
                json.toString().contains("last_successful_label"));
    }

    @Test
    public void shouldReturnBuildingStatusIfAnyBuildsAreScheduled() throws Exception {
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage, null, agentService);
        Map json = presenter.toJson();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"current_status\": \"failing\"\n" +
                "}");
    }

    @Test
    public void shouldReturnJsonWithModifications() throws Exception {
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage, null, agentService);
        Map json = presenter.toJson();

        JsonValue jsonValue = JsonUtils.from(json);
        JsonValue revision = jsonValue.getObject("materialRevisions", 0);
        //TODO: TRAINWRECK! WE should fix this when we re-do the JSON. We don't think this test will last long in the new UI
        String expected = modifications
                .getMaterialRevisions()
                .getMaterialRevision(0)
                .getModifications().get(0).getRevision();
        assertThat(revision.getString("revision"), is(expected));
        assertThat(revision.getString("user"), is(ModificationsMother.MOD_USER_WITH_HTML_CHAR));
        assertThat(revision.getString("date"), is(DateUtils.formatISO8601(ModificationsMother.TODAY_CHECKIN)));
    }

    @Test
    public void shouldReturnJsonForNullStage() throws Exception {
        final StageConfig config = oneBuildPlanWithResourcesAndMaterials("newStage");
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(
                pipeline, createNullStage(config), null, agentService);
        Map json = presenter.toJson();

        assertThatJson(new Gson().toJson(json.get("builds"))).when(IGNORING_EXTRA_FIELDS).isEqualTo("[\n" +
                "  {\n" +
                "    \"current_status\": \"unknown\"\n" +
                "  }\n" +
                "]");
    }

    @Test
    public void shouldReturnLastSuccesfulLabel() throws Exception {
        StageIdentifier successfulStage = new StageIdentifier(pipeline.getName(), 1, "LABEL:1", stage.getName(), "1");
        StageJsonPresentationModel presenter =
                new StageJsonPresentationModel(pipeline, stage, successfulStage, agentService);
        Map json = presenter.toJson();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"last_successful_label\": \"LABEL:1\",\n" +
                "  \"last_successful_stage_locator\": \"pipeline/1/stage/1\"\n" +
                "}");
    }

    @Test
    public void shouldGetAPresenterWithCanRunStatus() throws Exception {
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage, null, agentService);
        presenter.setCanRun(true);
        Map json = presenter.toJson();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"getCanRun\": \"true\"\n" +
                "}");
    }

    @Test
    public void shouldGetAPresenterWithCanCancelStatus() throws Exception {
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage, null, agentService);
        presenter.setCanCancel(true);
        Map json = presenter.toJson();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"getCanCancel\": \"true\"\n" +
                "}");
    }

    @Test
    public void shouldSetCanRunToFalseForANullStage() throws Exception {
        Stage unscheduled = new NullStage("unscheduled");
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, unscheduled, null,
                agentService);

        Map json = presenter.toJson();
        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"getCanRun\": \"false\"\n" +
                "}");
    }

    @Test
    public void shouldEscapeBuildCauseMessage() throws Exception {
        String userWithHtmlCharacters = "<user>";
        pipeline.setBuildCause(createManualForced(materialRevisions(userWithHtmlCharacters), new Username(new CaseInsensitiveString(userWithHtmlCharacters))));
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage, null, agentService);

        assertThatJson(new Gson().toJson(presenter.toJson())).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"buildCause\": \"Forced by \\u0026lt;user\\u0026gt;\"\n" +
                "}");
    }

    @Test
    public void shouldEncodeStageLocator() throws Exception {
        Stage stage1 = new Stage("stage-c%d", new JobInstances(), GoConstants.DEFAULT_APPROVED_BY, "manual", new TimeProvider());
        stage1.setIdentifier(new StageIdentifier("pipeline-a%b", 1, "label-1", "stage-c%d", "1"));
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage1, null, agentService);
        Map json = presenter.toJson();
        assertThat(JsonUtils.from(json).getString("stageLocator"), is("pipeline-a%25b/1/stage-c%25d/1"));
    }

    @Test
    public void shouldIncludeStageLocatorForDisplay() throws Exception {
        Stage stage1 = new Stage("stage-c%d", new JobInstances(), GoConstants.DEFAULT_APPROVED_BY, "manual", new TimeProvider());
        stage1.setIdentifier(new StageIdentifier("pipeline-a%b", 1, "label-1", "stage-c%d", "1"));
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage1, null, agentService);
        Map json = presenter.toJson();
        assertThat(JsonUtils.from(json).getString("stageLocatorForDisplay"), is("pipeline-a%b/label-1/stage-c%d/1"));
    }


    @Test
    public void shouldEncodeBuildLocator() throws Exception {
        JobInstance job = JobInstanceMother.assigned("job-%");
        Stage stage1 = new Stage("stage-c%d", new JobInstances(job), GoConstants.DEFAULT_APPROVED_BY, "manual", new TimeProvider());
        stage1.setIdentifier(new StageIdentifier("pipeline-a%b", 1, "label-1", "stage-c%d", "1"));
        job.setIdentifier(new JobIdentifier("pipeline-a%b", 1, "label-1", "stage-c%d", "1", "job-%", 0L));
        StageJsonPresentationModel presenter = new StageJsonPresentationModel(pipeline, stage1, null, agentService);
        Map json = presenter.toJson();
        assertThat(JsonUtils.from(json).getString("builds", 0, "buildLocator"),
                is("pipeline-a%25b/1/stage-c%25d/1/job-%25"));
    }

    private MaterialRevisions materialRevisions(String userWithHtmlCharacters) {
        MaterialRevisions revisions = new MaterialRevisions();
        revisions.addRevision(MaterialsMother.svnMaterial(),
                new Modification(userWithHtmlCharacters, "comment", "email", new Date(), "r1"));
        return revisions;
    }
}
