/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class DeletePipelineConfigsCommand extends PipelineConfigsCommand {
    private final PipelineConfigs group;

    public DeletePipelineConfigsCommand(PipelineConfigs group, LocalizedOperationResult result, Username currentUser,
                                        SecurityService securityService) {
        super(result, currentUser, securityService);
        this.group = group;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedPipelineConfigs = group;
        preprocessedConfig.deletePipelineGroup(group.getGroup());
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return true;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!group.isEmpty()) {
            result.unprocessableEntity("Failed to delete group " + group.getGroup() + " because it was not empty.");
            return false;
        }
        return isUserAdminOfGroup(group.getGroup());
    }
}
