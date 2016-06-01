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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StageStatusPluginNotifier implements StageStatusListener {
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private NotificationPluginRegistry notificationPluginRegistry;
    private GoConfigService goConfigService;
    private PipelineSqlMapDao pipelineSqlMapDao;
    private PluginNotificationQueue pluginNotificationQueue;

    @Autowired
    public StageStatusPluginNotifier(NotificationPluginRegistry notificationPluginRegistry, GoConfigService goConfigService, PipelineSqlMapDao pipelineSqlMapDao, PluginNotificationQueue pluginNotificationQueue) {
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.goConfigService = goConfigService;
        this.pipelineSqlMapDao = pipelineSqlMapDao;
        this.pluginNotificationQueue = pluginNotificationQueue;
    }

    @Override
    public void stageStatusChanged(final Stage stage) {
        if (isAnyPluginInterestedInStageStatus() && isStageStateScheduledOrCompleted(stage)) {
            Map data = createRequestDataMap(stage);

            pluginNotificationQueue.post(new PluginNotificationMessage(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION, data));
        }
    }

    private boolean isAnyPluginInterestedInStageStatus() {
        return notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION);
    }

    private boolean isStageStateScheduledOrCompleted(Stage stage) {
        return stage.isScheduled() || stage.isReRun() || stage.getState().completed();
    }

    Map createRequestDataMap(Stage stage) {
        Map<String, Object> data = new LinkedHashMap<>();
        String pipelineName = stage.getIdentifier().getPipelineName();
        Integer pipelineCounter = new Integer(stage.getIdentifier().getPipelineCounter());

        Map<String, Object> pipelineMap = createPipelineDataMap(pipelineName, pipelineCounter, stage);
        data.put("pipeline", pipelineMap);

        return data;
    }

    private Map<String, Object> createPipelineDataMap(String pipelineName, Integer pipelineCounter, Stage stage) {
        Map<String, Object> pipelineMap = new LinkedHashMap<>();
        pipelineMap.put("name", pipelineName);
        pipelineMap.put("counter", pipelineCounter.toString());

        String pipelineGroup = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
        pipelineMap.put("group", pipelineGroup);

        BuildCause buildCause = pipelineSqlMapDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, pipelineCounter);

        List<Map> materialRevisionList = createBuildCauseDataMap(buildCause.getMaterialRevisions());
        pipelineMap.put("build-cause", materialRevisionList);

        Map<String, Object> stageMap = createStageDataMap(stage);
        pipelineMap.put("stage", stageMap);

        return pipelineMap;
    }

    private List<Map> createBuildCauseDataMap(MaterialRevisions materialRevisions) {
        List<Map> materialRevisionList = new ArrayList<>();
        for (MaterialRevision currentRevision : materialRevisions) {
            Map<String, Object> materialRevisionMap = new LinkedHashMap<>();

            materialRevisionMap.put("material", currentRevision.getMaterial().getAttributes(false));
            materialRevisionMap.put("changed", currentRevision.isChanged());

            List<Map> modificationList = new ArrayList<>();
            for (Modification modification : currentRevision.getModifications()) {
                Map<String, Object> modificationMap = createModificationDataMap(modification);
                modificationList.add(modificationMap);
            }
            materialRevisionMap.put("modifications", modificationList);

            materialRevisionList.add(materialRevisionMap);
        }
        return materialRevisionList;
    }

    private Map<String, Object> createModificationDataMap(Modification modification) {
        Map<String, Object> modificationMap = new LinkedHashMap<>();
        modificationMap.put("revision", modification.getRevision());
        modificationMap.put("modified-time", dateToString(modification.getModifiedTime()));
        modificationMap.put("data", modification.getAdditionalDataMap());
        return modificationMap;
    }

    private Map<String, Object> createStageDataMap(Stage stage) {
        Map<String, Object> stageMap = new LinkedHashMap<>();
        stageMap.put("name", stage.getName());
        stageMap.put("counter", new Integer(stage.getCounter()).toString());
        stageMap.put("approval-type", stage.getApprovalType());
        stageMap.put("approved-by", stage.getApprovedBy());
        stageMap.put("state", stage.getState().toString());
        stageMap.put("result", stage.getResult().toString());
        stageMap.put("create-time", timestampToString(stage.getCreatedTime()));
        stageMap.put("last-transition-time", timestampToString(stage.getLastTransitionedTime()));

        List<Map> jobsList = new ArrayList<>();
        for (JobInstance currentJob : stage.getJobInstances()) {
            Map<String, Object> jobMap = createJobDataMap(currentJob);
            jobsList.add(jobMap);
        }
        stageMap.put("jobs", jobsList);

        return stageMap;
    }

    private Map<String, Object> createJobDataMap(JobInstance job) {
        Map<String, Object> jobMap = new LinkedHashMap<>();
        jobMap.put("name", job.getName());
        jobMap.put("schedule-time", dateToString(job.getScheduledDate()));
        jobMap.put("complete-time", dateToString(job.getCompletedDate()));
        jobMap.put("state", job.getState().toString());
        jobMap.put("result", job.getResult().toString());
        jobMap.put("agent-uuid", job.getAgentUuid());
        return jobMap;
    }

    private String timestampToString(Timestamp timestamp) {
        return timestamp == null ? "" : new SimpleDateFormat(DATE_PATTERN).format(timestamp);
    }

    private String dateToString(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_PATTERN).format(date);
    }
}
