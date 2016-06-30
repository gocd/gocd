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

import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;

/* Represents a pipeline on the dashboard. Cacheable, since the permissions are not specific to a user. */
public class GoDashboardPipeline {
    private final PipelineModel pipelineModel;
    private final Permissions permissions;
    private final String groupName;

    public GoDashboardPipeline(PipelineModel pipelineModel, Permissions permissions, String groupName) {
        this.pipelineModel = pipelineModel;
        this.permissions = permissions;
        this.groupName = groupName;
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
}
