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
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.NullAgent;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.server.web.JsonView;
import com.thoughtworks.go.util.TimeConverter;
import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.JobState.*;
import static java.lang.String.valueOf;


public class JobStatusJsonPresentationModel {
    private AgentConfig agentConfig;
    private final JobInstance instance;
    private TimeConverter timeConverter = new TimeConverter();
    private DurationBean durationBean;

    public JobStatusJsonPresentationModel(JobInstance instance, AgentConfig agentConfig, DurationBean durationBean) {
        this.instance = instance;
        this.agentConfig = agentConfig == null ? NullAgent.createNullAgent() : agentConfig;
        this.durationBean = durationBean;
    }

    public JobStatusJsonPresentationModel(JobInstance instance, AgentConfig agentConfig) {
        this(instance, agentConfig, new DurationBean(instance.getId()));
    }

    public JobStatusJsonPresentationModel(JobInstance instance) {
        this(instance, null, new DurationBean(instance.getId()));
    }

    public Map toJsonHash() {
        Map<String, Object> jsonParams = new LinkedHashMap<>();
        jsonParams.put("agent", agentConfig.getHostNameForDispaly());
        jsonParams.put("agent_ip", agentConfig.getIpAddress());
        jsonParams.put("agent_uuid", agentConfig.getUuid());
        jsonParams.put("build_scheduled_date", getPreciseScheduledDate());
        jsonParams.put("build_assigned_date", getPreciseDateFor(Assigned));
        jsonParams.put("build_preparing_date", getPreciseDateFor(Preparing));
        jsonParams.put("build_building_date", getPreciseDateFor(Building));
        jsonParams.put("build_completing_date", getPreciseDateFor(Completing));
        jsonParams.put("build_completed_date", getPreciseDateFor(Completed));
        jsonParams.put("current_status", instance.displayStatusWithResult());
        jsonParams.put("current_build_duration", instance.getCurrentBuildDuration());
        jsonParams.put("last_build_duration", Long.toString(this.durationBean.getDuration()));
        jsonParams.put("id", Long.toString(getBuildInstanceId()));
        jsonParams.put("is_completed", valueOf(instance.isCompleted()));
        jsonParams.put("name", getName());
        jsonParams.put("result", getResult().toString());
        jsonParams.put("buildLocator", instance.buildLocator());
        jsonParams.put("buildLocatorForDisplay", instance.buildLocatorForDisplay());
        return jsonParams;
    }

    public String toJsonString() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("building_info", this.toJsonHash());
        return new JsonView().renderJson(info);
    }

    public String getName() {
        return instance.getName();
    }

    public long getPipelineId() {
        return instance.getStageId();
    }

    public JobResult getResult() {
        return this.instance.getResult();
    }

    public String getCurrentStatus() {
        return instance.currentStatus().toString();
    }

    public String getStatus() {
        JobState currentstatus = instance.currentStatus();
        return (currentstatus == JobState.Scheduled || currentstatus == JobState.Waiting)
                ? getResult().toString() : currentstatus.toString().toLowerCase();
    }

    public String getBuildInstanceCompletedTimestamp() {
        return TimeConverter.getHumanReadableDate(new DateTime(instance.getStartedDateFor(JobState.Completed)));
    }

    public long getBuildInstanceId() {
        return instance.getId();
    }

    public String getUrl() {
        return String.valueOf(instance.getId());
    }

    public boolean isSame(long buildInstanceId) {
        return instance.getId() == buildInstanceId;
    }

    public String getTabToShow() {
        String result = "";
        if (instance.isPassed()) {
            result = "#tab-artifacts";
        } else if (instance.isFailed()) {
            result = "#tab-failures";
        }
        return result;
    }

    public String getBuildLocator() {
        return instance.buildLocator();
    }

    public String getBuildLocatorForDisplay() {
        return instance.buildLocatorForDisplay();
    }

    public String getPreciseScheduledDate() {
        return timeConverter.nullSafeDate(instance.getScheduledDate());
    }

    public String getPreciseCompletedDate() {
        return getPreciseDateFor(JobState.Completed);
    }

    public String getPreciseDateFor(JobState state) {
        return timeConverter.nullSafeDate(instance.getStartedDateFor(state));
    }

    public String getHumanReadableScheduledDate() {
        return timeConverter.getConvertedTime(instance.getScheduledDate()).toString();
    }

    public boolean isCopy() {
        return instance.isCopy();
    }
}
