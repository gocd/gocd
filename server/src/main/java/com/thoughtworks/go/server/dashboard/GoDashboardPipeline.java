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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;

import java.util.Optional;

/* Represents a pipeline on the dashboard. Cacheable, since the permissions are not specific to a user. */
public class GoDashboardPipeline {
    private final PipelineModel pipelineModel;
    private final Permissions permissions;
    private final String groupName;
    private final TrackingTool trackingTool;
    private final long lastUpdatedTimeStamp;
    private ConfigOrigin origin;
    private int displayOrderWeight;
    private PipelineConfig pipelineConfig;

    public GoDashboardPipeline(PipelineModel pipelineModel, Permissions permissions, String groupName, Counter timeStampBasedCounter, PipelineConfig pipelineConfig) {
        this.pipelineModel = pipelineModel;
        this.permissions = permissions;
        this.groupName = groupName;
        this.trackingTool = pipelineConfig.getTrackingTool();
        this.lastUpdatedTimeStamp = timeStampBasedCounter.getNext();
        this.origin = pipelineConfig.getOrigin();
        this.displayOrderWeight = pipelineConfig.getDisplayOrderWeight();
        this.pipelineConfig = pipelineConfig;
    }

    public String groupName() {
        return groupName;
    }

    public PipelineModel model() {
        return pipelineModel;
    }

    public Permissions permissions() {
        return permissions;
    }

    public Optional<TrackingTool> getTrackingTool() {
        return Optional.ofNullable(trackingTool);
    }

    public PipelineConfig pipelineConfig() {
        return pipelineConfig;
    }

    public StageInstanceModel getLatestStage() {
        PipelineInstanceModel latestPipelineInstance = pipelineModel.getLatestPipelineInstance();
        if (latestPipelineInstance == null) {
            return null;
        }
        return latestPipelineInstance.latestStage();
    }

    public CaseInsensitiveString name() {
        return new CaseInsensitiveString(pipelineModel.getName());
    }

    public boolean canBeViewedBy(String userName) {
        return permissions.viewers().contains(userName);
    }

    public boolean canBeOperatedBy(String userName) {
        return permissions.operators().contains(userName);
    }

    public boolean canBeAdministeredBy(String userName) {
        return permissions.admins().contains(userName);
    }

    public boolean isPipelineOperator(String userName) {
        return permissions.pipelineOperators().contains(userName);
    }

    public boolean isStageOperator(String stageName, String userName) {
        return permissions.stageOperators(stageName).contains(userName);
    }

    public long getLastUpdatedTimeStamp() {
        return lastUpdatedTimeStamp;
    }

    @Override
    public String toString() {
        return String.format("GoDashboardPipeline{name='%s',groupName='%s'}", name(), groupName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GoDashboardPipeline that = (GoDashboardPipeline) o;

        if (lastUpdatedTimeStamp != that.lastUpdatedTimeStamp) return false;
        if (pipelineModel != null ? !pipelineModel.equals(that.pipelineModel) : that.pipelineModel != null)
            return false;
        if (permissions != null ? !permissions.equals(that.permissions) : that.permissions != null) return false;
        return groupName != null ? groupName.equals(that.groupName) : that.groupName == null;
    }

    @Override
    public int hashCode() {
        int result = pipelineModel != null ? pipelineModel.hashCode() : 0;
        result = 31 * result + (permissions != null ? permissions.hashCode() : 0);
        result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
        result = 31 * result + (int) (lastUpdatedTimeStamp ^ (lastUpdatedTimeStamp >>> 32));
        return result;
    }

    String cacheSegment() {
        return name().toString() + ":" + getLastUpdatedTimeStamp();
    }

    public boolean isLocal() {
        return origin == null || origin.isLocal();
    }

    public Integer getdisplayOrderWeight() {
        return displayOrderWeight;
    }

    public ConfigOrigin getOrigin() {
        return origin;
    }

    public boolean isAllowOnlyOnSuccessOfPreviousStage(String stageName) {
        StageConfig stage = this.pipelineConfig.getStage(stageName);
        return stage != null && stage.getApproval().isAllowOnlyOnSuccess();
    }

    public boolean isUsingTemplate() {
        return this.pipelineConfig.hasTemplate();
    }

    public String getTemplateName() {
        if (this.pipelineConfig.hasTemplate()) {
            return this.pipelineConfig.getTemplateName().toString();
        }
        return null;
    }
}
