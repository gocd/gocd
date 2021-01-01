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

import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.permissions.EveryonePermission;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.util.SystemTimeClock;

import static com.thoughtworks.go.domain.PipelinePauseInfo.notPaused;

public class GoDashboardPipelineMother {
    public static GoDashboardPipeline pipeline(String pipelineName) {
        return pipeline(pipelineName, "group1");
    }

    public static GoDashboardPipeline pipeline(String pipelineName, String groupName) {
        Permissions permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE);
        return pipeline(pipelineName, groupName, permissions);
    }

    public static GoDashboardPipeline pipeline(String pipelineName, String groupName, Permissions permissions) {
        return new GoDashboardPipeline(new PipelineModel(pipelineName, false, false, notPaused()),
                permissions, groupName, new TimeStampBasedCounter(new SystemTimeClock()), PipelineConfigMother.pipelineConfig(pipelineName));
    }
}
