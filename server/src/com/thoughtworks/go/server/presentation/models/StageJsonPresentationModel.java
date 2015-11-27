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

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.dto.DurationBeans;
import com.thoughtworks.go.util.TimeConverter;
import com.thoughtworks.go.util.json.JsonAware;

import java.util.*;

import static com.thoughtworks.go.util.UrlUtil.encodeInUtf8;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/*
 *  Understands conversion of a Stage and its Builds to view representations.
 */
public class StageJsonPresentationModel implements JsonAware {
    private final Pipeline pipeline;
    private final Stage stage;
    private final StageIdentifier lastSuccessfulStage;
    private Agents agents;
    private DurationBeans durations;
    private final TrackingTool trackingTool;
    private TimeConverter timeConverter = new TimeConverter();
    private static final DurationBeans NO_DURATIONS = new DurationBeans();
    private boolean canRun;
    private ModificationSummaries summaries;
    private boolean canCancel;

    StageJsonPresentationModel(Pipeline pipeline, Stage stage, StageIdentifier lastSuccessfulStage,
                               Agents agents) {
        this(pipeline, stage, lastSuccessfulStage, agents, NO_DURATIONS, new TrackingTool());
    }

    public StageJsonPresentationModel(Pipeline pipeline, Stage stage, Agents agents, DurationBeans durations,
                                      TrackingTool trackingTool) {
        this(pipeline, stage, null, agents, durations, trackingTool);
    }

    public StageJsonPresentationModel(Pipeline pipeline, Stage stage, StageIdentifier lastSuccessfuleStage,
                                      Agents agents, DurationBeans durations, TrackingTool trackingTool) {
        this.pipeline = pipeline;
        this.stage = stage;
        this.lastSuccessfulStage = lastSuccessfuleStage;
        this.agents = agents;
        this.durations = durations;
        this.trackingTool = trackingTool;
        this.summaries = pipeline.toModificationSummaries();
    }

    public Map toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("pipelineId", pipeline.getId());
        json.put("pipelineName", pipeline.getName());
        json.put("uniqueStageId", uniqueStageId());
        json.put("buildCause", escapeHtml(pipeline.getBuildCauseMessage()));
        json.put("materialRevisions", materialRevisionsJson());
        json.put("stageName", stage.getName());
        json.put("stageCounter", stage.getCounter());
        json.put("current_label", pipeline.getLabel());
        json.put("pipelineCounterOrLabel", String.valueOf(pipeline.getIdentifier().instanceIdentifier()));
        if (pipeline.getCounter() != null) {
            json.put("pipelineCounter", String.valueOf(pipeline.getCounter()));
        }
        json.put("id", String.valueOf(stage.getId()));
        json.put("builds", jsonForBuildPlans());
        json.put("current_status", currentStatus());
        if (lastSuccessfulStage != null) {
            json.put("last_successful_label", lastSuccessfulStage.getPipelineLabel());
            json.put("last_successful_stage_locator", lastSuccessfulStage.getStageLocator());
        }
        if (stage.stageState().completed()) {
            json.put("stage_completed_date", getStageCompletedTime());
        }
        json.put("getCanRun", valueOf(canRun));
        json.put("getCanCancel", valueOf(canCancel));
        json.put("stageLocator", encodeInUtf8(stage.stageLocator()));
        json.put("stageLocatorForDisplay", stage.stageLocatorForDisplay());
        return json;
    }


    private List materialRevisionsJson() {
        MaterialRevisionsJsonBuilder jsonVisitor = new MaterialRevisionsJsonBuilder(trackingTool);
        jsonVisitor.setIncludeModifiedFiles(false);
        pipeline.getBuildCause().getMaterialRevisions().accept(jsonVisitor);
        return jsonVisitor.json();
    }

    public String uniqueStageId() {
        return pipeline.getName() + "-" + stage.getName() + "-" + stage.getId();
    }

    public String currentStatus() {
        return stage.stageState().toString().toLowerCase();
    }

    private TimeConverter.ConvertedTime getStageCompletedTime() {
        Date completedDate = stage.completedDate();
        if (completedDate == null) {
            return TimeConverter.ConvertedTime.NO_HISTORICAL_DATA;
        }
        return timeConverter.getConvertedTime(completedDate);
    }

    private List jsonForBuildPlans() {
        JobInstances builds = stage.getJobInstances();
        List plans = new ArrayList();
        for (JobInstance job : builds) {
            JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(job,
                    agents.getAgentByUuid(job.getAgentUuid()),
                    durations.byId(job.getId()));
            Map jsonMap = presenter.toJsonHash();
            jsonMap.put("buildLocator", job.buildLocator());
            plans.add(jsonMap);

        }
        return plans;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public String getName() {
        return stage.getName();
    }

    public void setCanRun(boolean canRun) {
        this.canRun = canRun;
    }

    public boolean stageHasHistory() {
        return !(stage instanceof NullStage);
    }

    public String getPipelineLabel() {
        return pipeline.getLabel();
    }

    public String latestRevision() {
        return summaries.latestRevision();
    }

    public int getModificationCount() {
        return summaries.getModificationCount();
    }

    public ModificationSummary getModification(int index) {
        return summaries.getModification(index);
    }

    public boolean getCanRun() {
        return canRun;
    }

    public boolean getCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }
}

