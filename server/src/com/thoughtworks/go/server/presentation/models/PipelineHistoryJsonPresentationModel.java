/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.presentation.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.CommentRenderer;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.presentation.PipelineHistoryGroupingUtil;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.TimeConverter;
import com.thoughtworks.go.util.UrlUtil;

public class PipelineHistoryJsonPresentationModel implements JsonAware {
    private final PipelinePauseInfo pipelinePauseInfo;
    private final PipelineConfig pipelineConfig;
    private final Pagination pagination;
    private TimeConverter timeConverter = new TimeConverter();
    private boolean canForce;
    private final boolean hasForceBuildCause;
    private PipelineHistoryGroups pipelineHistoryGroups;
    private final boolean hasBuildCauseInBuffer;
    private final boolean canPause;

    public PipelineHistoryJsonPresentationModel(PipelinePauseInfo pipelinePauseInfo,
                                                PipelineInstanceModels pipelineHistory,
                                                PipelineConfig pipelineConfig,
                                                Pagination pagination, boolean canForce,
                                                boolean hasForcedBuildCause, boolean hasBuildCauseInBuffer,
                                                boolean canPause) {
        this(pipelinePauseInfo,
                new PipelineHistoryGroupingUtil().createGroups(pipelineHistory),
                pipelineConfig,
                pagination,
                canForce,
                hasForcedBuildCause, hasBuildCauseInBuffer, canPause);
    }

    PipelineHistoryJsonPresentationModel(PipelinePauseInfo pipelinePauseInfo,
                                         PipelineHistoryGroups pipelineHistoryGroups,
                                         PipelineConfig pipelineConfig,
                                         Pagination pagination, boolean canForce,
                                         boolean hasForceBuildCause, boolean hasBuildCauseInBuffer, boolean canPause) {
        this.pipelinePauseInfo = pipelinePauseInfo;
        this.pipelineHistoryGroups = pipelineHistoryGroups;
        this.pipelineConfig = pipelineConfig;
        this.pagination = pagination;
        this.canForce = canForce;
        this.hasForceBuildCause = hasForceBuildCause;
        this.hasBuildCauseInBuffer = hasBuildCauseInBuffer;
        this.canPause = canPause;
        createGroupForCurrentConfigIfItHasChanged(new HashMap<String, StageIdentifier>());
    }

    private void createGroupForCurrentConfigIfItHasChanged(Map<String, StageIdentifier> latest) {
        if (pipelineHistoryGroups.isEmpty()) {
            if (hasBuildCauseInBuffer || pipelineConfig.isFirstStageManualApproval()) {
                createGroupForCurrentConfig(latest);
            }
            return;
        }
        if (hasPipelineConfigChanged()) {
            createGroupForCurrentConfig(latest);
        }
    }

    private boolean hasPipelineConfigChanged() {
        return !pipelineHistoryGroups.first().match(pipelineConfig);
    }

    private void createGroupForCurrentConfig(Map<String, StageIdentifier> latest) {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel(
                new StageConfigurationModels(pipelineConfig, latest));
        pipelineHistoryGroups.add(0, group);
    }

    public JsonMap toJson() {
        JsonMap json = new JsonMap();
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        json.put("pipelineName", pipelineName);
        json.put("paused", String.valueOf(pipelinePauseInfo.isPaused()));
        json.put("pauseCause", pipelinePauseInfo.getPauseCause());
        json.put("pauseBy", pipelinePauseInfo.getPauseBy());
        json.put("canForce", String.valueOf(canForce));
        json.put("nextLabel", "");
        json.put("groups", groupAsJson());
        json.put("forcedBuild", String.valueOf(hasForceBuildCause));
        json.put("showForceBuildButton", String.valueOf(showForceBuildButton()));
        json.put("canPause", String.valueOf(canPause));
        json.putAll(pagination.toJsonMap());
        return json;
    }

    private boolean showForceBuildButton() {
        return hasBuildCauseInBuffer || pipelineConfig.isFirstStageManualApproval();
    }

    private Json groupAsJson() {
        JsonList jsonList = new JsonList();
        for (PipelineInstanceGroupModel group : pipelineHistoryGroups) {
            JsonMap jsonMap = new JsonMap();
            BaseCollection<StageConfigurationModel> groupConfig = group.getConfig();
            Json configJson = configAsJson(groupConfig);
            jsonMap.put("config", configJson);
            jsonMap.put("history", historyAsJson(group.getPipelineInstances()));
            jsonList.add(jsonMap);
        }
        return jsonList;
    }

    private Json configAsJson(BaseCollection<StageConfigurationModel> config) {
        JsonList jsonList = new JsonList();
        for (StageConfigurationModel stageInfo : config) {
            JsonMap jsonMap = new JsonMap();
            jsonMap.put("name", stageInfo.getName());
            jsonMap.put("isAutoApproved", String.valueOf(stageInfo.isAutoApproved()));
            jsonList.add(jsonMap);
        }
        JsonMap jsonMap = new JsonMap();
        jsonMap.put("stages", jsonList);
        return jsonMap;
    }

    private Json historyAsJson(BaseCollection<PipelineInstanceModel> pipelineHistory) {
        JsonList json = new JsonList();
        for (PipelineInstanceModel item : pipelineHistory) {
            JsonMap jsonMap = new JsonMap();
            jsonMap.put("pipelineId", item.getId());
            jsonMap.put("label", item.getLabel());
            jsonMap.put("counterOrLabel", item.getPipelineIdentifier().instanceIdentifier());
            jsonMap.put("scheduled_date", timeConverter.getConvertedTime(item.getScheduledDate()));
            jsonMap.put("buildCauseBy", item.getApprovedByForDisplay());
            jsonMap.put("modification_date", getModificationDate(item));
            jsonMap.put("materialRevisions", materialRevisionsJson(item));
            jsonMap.put("stages", stageHistoryAsJson(item, item.getStageHistory()));
            jsonMap.put("revision", item.getRevisionOfLatestModification());
            json.add(jsonMap);
        }
        return json;
    }

    private JsonList materialRevisionsJson(PipelineInstanceModel item) {
        CommentRenderer commentRenderer = pipelineConfig.getCommentRenderer();
        MaterialRevisionsJsonBuilder jsonVisitor = new MaterialRevisionsJsonBuilder(commentRenderer);
        jsonVisitor.setIncludeModifiedFiles(false);
        item.getBuildCause().getMaterialRevisions().accept(jsonVisitor);
        return jsonVisitor.json();
    }

    // TODO #1234 - should not get latest modified date
    private TimeConverter.ConvertedTime getModificationDate(PipelineInstanceModel item) {
        Date mostRecentModificationDate = item.getBuildCause().getMaterialRevisions().getDateOfLatestModification();
        return timeConverter.getConvertedTime(mostRecentModificationDate);
    }

    private Json stageHistoryAsJson(PipelineInstanceModel pipelineInstanceModel, StageInstanceModels stageHistory) {
        JsonList json = new JsonList();
        for (StageInstanceModel stageHistoryItem : stageHistory) {
            JsonMap jsonMap = new JsonMap();
            jsonMap.put("stageName", stageHistoryItem.getName());
            jsonMap.put("stageId", stageHistoryItem.getId());
            jsonMap.put("stageStatus", stageHistoryItem.getState().toString());
            StageIdentifier stageIdentifier = new StageIdentifier(pipelineInstanceModel.getPipelineIdentifier(),
                    stageHistoryItem.getName(), stageHistoryItem.getCounter());
            jsonMap.put("stageLocator", UrlUtil.encodeInUtf8(stageIdentifier.stageLocator()));
            jsonMap.put("getCanRun", Boolean.toString(stageHistoryItem.getCanRun()));
            jsonMap.put("getCanCancel", Boolean.toString(stageHistoryItem.getCanCancel()));
            jsonMap.put("scheduled", Boolean.toString(stageHistoryItem.isScheduled()));
            jsonMap.put("stageCounter", stageHistoryItem.getCounter());
            handleApproval(stageHistoryItem, jsonMap);
            json.add(jsonMap);
        }
        return json;
    }

    private void handleApproval(StageInstanceModel stageHistoryItem, JsonMap jsonMap) {
        if (stageHistoryItem.needsApproval() && !pipelinePauseInfo.isPaused()) {
            jsonMap.put("needsApproval", String.valueOf(true));
        } else if (stageHistoryItem.getApprovedBy() != null) {
            jsonMap.put("approvedBy", stageHistoryItem.getApprovedBy());
        }
    }

}
