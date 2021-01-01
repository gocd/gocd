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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class StageAuthorizationChecker implements SchedulingChecker {
    private String pipelineName;
    private String stageName;
    private String username;
    private SecurityService securityService;

    public StageAuthorizationChecker(String pipelineName, String stageName, String username,
                                      SecurityService securityService) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.username = username;
        this.securityService = securityService;
    }

    @Override
    public void check(OperationResult result) {
        HealthStateType id = HealthStateType.general(HealthStateScope.forPipeline(pipelineName));
        if (!securityService.hasOperatePermissionForStage(pipelineName, stageName, username)) {
            String message = String.format("Failed to trigger pipeline [%s]", pipelineName);
            result.forbidden(message,
                    "User " + username + " does not have permission to schedule " + pipelineName + "/" + stageName,
                    id);
        } else {
            result.success(id);
        }
    }
}
