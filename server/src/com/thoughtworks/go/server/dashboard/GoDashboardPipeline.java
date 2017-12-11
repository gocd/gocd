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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;

/* Represents a pipeline on the dashboard. Cacheable, since the permissions are not specific to a user. */
public class GoDashboardPipeline {
    private final PipelineModel pipelineModel;
    private final Permissions permissions;
    private final String groupName;
    private final long lastUpdatedTimeStamp;

    public GoDashboardPipeline(PipelineModel pipelineModel, Permissions permissions, String groupName, Counter timeStampBasedCounter) {
        this.pipelineModel = pipelineModel;
        this.permissions = permissions;
        this.groupName = groupName;
        this.lastUpdatedTimeStamp = timeStampBasedCounter.getNext();
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
}
