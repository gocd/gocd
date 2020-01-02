/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.notification.v1;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.plugin.access.notification.DataConverter;

import java.util.ArrayList;
import java.util.Map;

public class StageConverter extends DataConverter<StageNotificationDTO> {
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private Stage stage;
    private final String pipelineGroup;
    private final BuildCause buildCause;

    public StageConverter(StageNotificationData stageNotificationData) {
        this.stage = stageNotificationData.getStage();
        this.pipelineGroup = stageNotificationData.getPipelineGroup();
        this.buildCause = stageNotificationData.getBuildCause();
    }

    @Override
    public StageNotificationDTO transformData() {
        String pipelineName = stage.getIdentifier().getPipelineName();
        Integer pipelineCounter = stage.getIdentifier().getPipelineCounter();
        StageNotificationDTO.PipelineDTO pipeline = new StageNotificationDTO.PipelineDTO(pipelineName, pipelineCounter, pipelineGroup, createBuildCause(buildCause), createStageDTO());
        return new StageNotificationDTO(pipeline);
    }

    private StageNotificationDTO.StageDTO createStageDTO() {
        ArrayList<StageNotificationDTO.JobDTO> jobs = new ArrayList<>();
        for (JobInstance job : stage.getJobInstances()) {
            StageNotificationDTO.JobDTO jobDTO = new StageNotificationDTO.JobDTO(job.getName(), job.getScheduledDate(), job.getCompletedDate(), job.getState(), job.getResult(), job.getAgentUuid());
            jobs.add(jobDTO);
        }

        return new StageNotificationDTO.StageDTO(stage.getName(), stage.getCounter(), stage.getApprovalType(), stage.getApprovedBy(), stage.getState(), stage.getResult(), stage.getCreatedTime(), stage.getLastTransitionedTime(), jobs);
    }

    private ArrayList<StageNotificationDTO.MaterialRevisionDTO> createBuildCause(BuildCause buildCause) {
        ArrayList<StageNotificationDTO.MaterialRevisionDTO> revisions = new ArrayList<>();
        for (MaterialRevision currentRevision : buildCause.getMaterialRevisions()) {
            Map<String, Object> attributes = currentRevision.getMaterial().getAttributes(false);
            ArrayList<StageNotificationDTO.ModificationDTO> modifications = new ArrayList<>();
            for (Modification modification : currentRevision.getModifications()) {
                modifications.add(new StageNotificationDTO.ModificationDTO(modification.getRevision(), modification.getModifiedTime(), modification.getAdditionalDataMap()));
            }
            revisions.add(new StageNotificationDTO.MaterialRevisionDTO(attributes, currentRevision.isChanged(), modifications));
        }
        return revisions;
    }
}
