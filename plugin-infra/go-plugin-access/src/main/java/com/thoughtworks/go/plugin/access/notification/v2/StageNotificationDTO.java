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
package com.thoughtworks.go.plugin.access.notification.v2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StageNotificationDTO {
    @Expose
    @SerializedName("pipeline")
    private PipelineDTO pipeline;

    public StageNotificationDTO(PipelineDTO pipeline) {
        this.pipeline = pipeline;
    }

    public static class PipelineDTO {
        @Expose
        @SerializedName("name")
        private String name;
        @Expose
        @SerializedName("counter")
        private String counter;
        @Expose
        @SerializedName("label")
        private String pipelineLabel;
        @Expose
        @SerializedName("group")
        private String group;
        @Expose
        @SerializedName("build-cause")
        private List<MaterialRevisionDTO> buildCause;
        @Expose
        @SerializedName("stage")
        private StageDTO stage;

        public PipelineDTO(String name, Integer counter, String pipelineLabel, String group, List<MaterialRevisionDTO> buildCause, StageDTO stage) {
            this.name = name;
            this.counter = counter.toString();
            this.pipelineLabel = pipelineLabel;
            this.group = group;
            this.buildCause = buildCause;
            this.stage = stage;
        }
    }

    public static class MaterialRevisionDTO {
        @Expose
        @SerializedName("material")
        private Map<String, Object> material;
        @Expose
        @SerializedName("changed")
        private boolean changed;
        @Expose
        @SerializedName("modifications")
        private List<ModificationDTO> modifications;

        public MaterialRevisionDTO(Map<String, Object> material, boolean changed, List<ModificationDTO> modifications) {
            this.material = material;
            this.changed = changed;
            this.modifications = modifications;
        }
    }

    public static class ModificationDTO {
        @Expose
        @SerializedName("revision")
        private String revision;
        @Expose
        @SerializedName("modified-time")
        private String modifiedTime;
        @Expose
        @SerializedName("data")
        private HashMap<String, String> data;

        public ModificationDTO(String revision, Date modifiedTime, HashMap<String, String> data) {
            this.revision = revision;
            this.modifiedTime = dateToString(modifiedTime);
            this.data = data;
        }
    }

    public static class StageDTO {
        @Expose
        @SerializedName("name")
        private String name;
        @Expose
        @SerializedName("counter")
        private String counter;
        @Expose
        @SerializedName("approval-type")
        private String approvalType;
        @Expose
        @SerializedName("approved-by")
        private String approvedBy;
        @Expose
        @SerializedName("state")
        private String state;
        @Expose
        @SerializedName("result")
        private String result;
        @Expose
        @SerializedName("create-time")
        private String createTime;
        @Expose
        @SerializedName("last-transition-time")
        private String lastTransitionTime;
        @Expose
        @SerializedName("jobs")
        private List<JobDTO> jobs;

        public StageDTO(String name, int counter, String approvalType, String approvedBy, StageState state, StageResult result, Timestamp createTime, Timestamp lastTransitionTime, List<JobDTO> jobs) {
            this.name = name;
            this.counter = new Integer(counter).toString();
            this.approvalType = approvalType;
            this.approvedBy = approvedBy;
            this.state = state.toString();
            this.result = result.toString();
            this.createTime = timestampToString(createTime);
            this.lastTransitionTime = timestampToString(lastTransitionTime);
            this.jobs = jobs;
        }
    }

    public static class JobDTO {
        @Expose
        @SerializedName("name")
        private String name;
        @Expose
        @SerializedName("schedule-time")
        private String scheduleTime;
        @Expose
        @SerializedName("assign-time")
        private final String assignTime;
        @Expose
        @SerializedName("complete-time")
        private String completeTime;
        @Expose
        @SerializedName("state")
        private String state;
        @Expose
        @SerializedName("result")
        private String result;
        @Expose
        @SerializedName("agent-uuid")
        private String agentUuid;

        public JobDTO(String name, Date scheduleTime, Date assignTime, Date completeTime, JobState state, JobResult result, String agentUuid) {
            this.name = name;
            this.scheduleTime = dateToString(scheduleTime);
            this.assignTime = dateToString(assignTime);
            this.completeTime = dateToString(completeTime);
            this.state = state.toString();
            this.result = result.toString();
            this.agentUuid = agentUuid;
        }
    }

    private static String timestampToString(Timestamp timestamp) {
        return timestamp == null ? "" : new SimpleDateFormat(StageConverter.DATE_PATTERN).format(timestamp);
    }

    private static String dateToString(Date date) {
        return date == null ? "" : new SimpleDateFormat(StageConverter.DATE_PATTERN).format(date);
    }
}
