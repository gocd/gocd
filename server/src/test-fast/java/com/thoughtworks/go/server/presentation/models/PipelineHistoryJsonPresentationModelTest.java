/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineHistoryMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.presentation.PipelineHistoryGroupingUtil;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.GoConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfigWithStages;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PipelineHistoryJsonPresentationModelTest {
    private PipelineConfig pipelineConfig;
    private PipelineHistoryJsonPresentationModel presenter;
    private static final int COUNT = 1;
    private static final int START = 1;
    private static final int PER_PAGE = 10;
    private static final boolean CAN_FORCE = false;
    private PipelinePauseInfo pipelinePauseInfo;
    private boolean hasForceBuildCause = false;
    private Date modificationDate = new Date();
    private boolean hasModification = false;

    @BeforeEach
    public void setUp() throws Exception {
        pipelineConfig = PipelineConfigMother.pipelineConfig("mingle", StageConfigMother.custom("dev", "defaultJob"),
            StageConfigMother.manualStage("ft"));
        pipelinePauseInfo = PipelinePauseInfo.notPaused();
        presenter = new PipelineHistoryJsonPresentationModel(pipelinePauseInfo,
            preparePipelineHistoryGroups(pipelineConfig),
            pipelineConfig,
            pagination(), CAN_FORCE, hasForceBuildCause, hasModification, true);
    }

    private Pagination pagination() {
        return Pagination.pageStartingAt(START, COUNT, PER_PAGE);
    }

    private PipelineHistoryGroups preparePipelineHistoryGroups(PipelineConfig pipelineConfig) {
        PipelineInstanceModels pipelineHistory = PipelineHistoryMother.pipelineHistory(pipelineConfig,
            modificationDate);
        return new PipelineHistoryGroupingUtil().createGroups(pipelineHistory);
    }

    private PipelineHistoryGroups preparePipelineHistoryGroupsWithErrorMessage(PipelineConfig pipelineConfig) {
        PipelineInstanceModels pipelineHistory = PipelineHistoryMother.pipelineHistoryWithErrorMessage(pipelineConfig,
            modificationDate);
        return new PipelineHistoryGroupingUtil().createGroups(pipelineHistory);
    }

    @AfterEach
    public void teardown() throws Exception {
        pipelinePauseInfo.setPaused(false);
    }

    @Test
    public void shouldContainPipelineConfig() {
        assertThatJson(new Gson().toJson(presenter.toJson())).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "groups": [
                    {
                      "config": {
                        "stages": [
                          {
                            "name": "dev",
                            "isAutoApproved": "true"
                          },
                          {
                            "name": "ft",
                            "isAutoApproved": "false"
                          }
                        ]
                      }
                    }
                  ]
                }""");
    }

    @Test
    public void shouldContainPipelineHistory() {
        Map json = presenter.toJson();
        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "groups": [
                    {
                      "history": [
                        {
                          "pipelineId": 1,
                          "stages": [
                            {
                              "stageStatus": "Passed",
                              "stageName": "dev",
                              "stageId": 0,
                              "approvedBy": "changes",
                              "getCanRun": "false",
                              "scheduled": "true"
                            },
                            {
                              "stageStatus": "Passed",
                              "stageName": "ft",
                              "stageId": 0,
                              "approvedBy": "lgao",
                              "getCanRun": "false",
                              "getCanCancel": "false",
                              "scheduled": "true"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }""");
    }

    @Test
    public void shouldContainPipelineHistoryWithErrorMessage() {
        presenter = new PipelineHistoryJsonPresentationModel(pipelinePauseInfo,
            preparePipelineHistoryGroupsWithErrorMessage(pipelineConfig),
            pipelineConfig,
            pagination(), CAN_FORCE, hasForceBuildCause, hasModification, true);
        Map json = presenter.toJson();
        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "groups": [
                    {
                      "history": [
                        {
                          "pipelineId": 1,
                          "stages": [
                            {
                              "stageStatus": "Cancelled",
                              "stageName": "dev",
                              "stageId": 0,
                              "approvedBy": "changes",
                              "getCanRun": "true",
                              "scheduled": "true"
                            },
                            {
                              "stageStatus": "Unknown",
                              "stageName": "ft",
                              "stageId": 0,
                              "approvedBy": "",
                              "getCanRun": "false",
                              "errorMessage":"Cannot schedule ft as the previous stage dev has Cancelled!",
                              "getCanCancel": "false",
                              "scheduled": "false"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }""");
    }

    @Test
    public void shouldContainPipelineHistoryComments() {
        Map json = presenter.toJson();
        assertThatJson(new Gson().toJson(json))
            .when(IGNORING_EXTRA_FIELDS)
            .isEqualTo("""
                    {
                      "groups": [
                        {
                          "history": [
                            {
                              "pipelineId": 1,
                              "comment": "build comment"
                            }
                          ]
                        }
                      ]
                    }""");

    }

    @Test
    public void shouldContainPipelinePageInfo() {
        assertThatJson(new Gson().toJson(presenter.toJson())).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "pipelineName": "mingle",
                  "count": 1,
                  "canForce": "false",
                  "start": 1,
                  "perPage": 10
                }""");
    }

    @Test
    public void needsApprovalInJsonShouldBeFalseWhenPipelineIsPaused() {
        pipelinePauseInfo.setPaused(true);
        HashMap<String, Object> map = new HashMap(presenter.toJson());

        assertThat(map.get("paused"), is("true"));
        assertThat(map, not(hasKey("needsApproval")));
    }

    @Test
    public void shouldShowFirstStageApproverNameInBuildCauseBy() {
        assertThatJson(presenter.toJson())
            .node("groups[0].history[0].buildCauseBy")
            .isEqualTo("Triggered by " + GoConstants.DEFAULT_APPROVED_BY);
    }

    @Test
    public void shouldContainMaterialRevisions() {
        assertThatJson(presenter.toJson())
            .node("groups[0].history[0].materialRevisions[0].revision").isEqualTo("svn.100")
            .node("groups[0].history[0].materialRevisions[0].user").isEqualTo("user")
            .node("groups[0].history[0].materialRevisions[0].date").isEqualTo(DateUtils.formatISO8601(modificationDate));
    }

    @Test
    public void shouldContainPipelineCounterOrLabel() {
        assertThatJson(presenter.toJson())
            .node("groups[0].history[0].counterOrLabel").isStringEqualTo("1");
    }

    @Test
    public void shouldContainPipelinePauseInfo() {
        pipelinePauseInfo.setPaused(true);
        pipelinePauseInfo.setPauseCause("pauseCause");
        pipelinePauseInfo.setPauseBy("pauseBy");

        assertThatJson(new Gson().toJson(presenter.toJson())).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "paused": "true"
                }""");
        assertThatJson(new Gson().toJson(presenter.toJson())).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "pauseCause": "pauseCause"
                }""");
        assertThatJson(new Gson().toJson(presenter.toJson())).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "pauseBy": "pauseBy"
                }""");
    }

    @Test
    public void shouldCreateGroupForCurrentConfigIfItHasChanged() {
        PipelineHistoryGroups historyGroups = preparePipelineHistoryGroups(pipelineConfig);
        PipelineConfig newConfig = createPipelineConfigWithStages("mingle", "stage1", "stage2");
        presenter = new PipelineHistoryJsonPresentationModel(pipelinePauseInfo,
                historyGroups,
                newConfig,
                pagination(), CAN_FORCE, hasForceBuildCause, hasModification, true);
        assertThatJson(new Gson().toJson(presenter.toJson())).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
                {
                  "groups": [
                    {
                      "config": {
                        "stages": [
                          {
                            "name": "stage1",
                            "isAutoApproved": "true"
                          },
                          {
                            "name": "stage2",
                            "isAutoApproved": "true"
                          }
                        ]
                      }
                    },
                    {
                      "config": {
                        "stages": [
                          {
                            "name": "dev",
                            "isAutoApproved": "true"
                          },
                          {
                            "name": "ft",
                            "isAutoApproved": "false"
                          }
                        ]
                      }
                    }
                  ]
                }""");
    }

    @Test
    public void shouldEncodeStageLocator() {
        PipelineConfig pipelineConfig1 = createPipelineConfigWithStages("mingle-%", "stage1-%",
                "stage2");
        PipelineHistoryJsonPresentationModel presenter = new PipelineHistoryJsonPresentationModel(pipelinePauseInfo,
                preparePipelineHistoryGroups(pipelineConfig1),
                pipelineConfig1,
                pagination(), CAN_FORCE, hasForceBuildCause, hasModification, true);
        assertThatJson(presenter.toJson())
            .node("groups[0].history[0].stages[0].stageLocator").isEqualTo("mingle-%25/1/stage1-%25/1");
    }

    @Test
    public void shouldGetScheduleTimestamp() {
        PipelineConfig pipelineConfig1 = createPipelineConfigWithStages("mingle-%", "stage1-%",
                "stage2");
        PipelineHistoryJsonPresentationModel presenter = new PipelineHistoryJsonPresentationModel(pipelinePauseInfo,
                preparePipelineHistoryGroups(pipelineConfig1),
                pipelineConfig1,
                pagination(), CAN_FORCE, hasForceBuildCause, hasModification, true);
        assertThatJson(presenter.toJson())
            .node("groups[0].history[0].scheduled_timestamp").isEqualTo(modificationDate.getTime());
    }

}
