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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.MingleConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands mingle configuration for pipelines
 */
@Service
public class MingleConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;

    @Autowired
    public MingleConfigService(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public MingleConfig mingleConfigForPipelineNamed(String pipelineName, Username user, HttpLocalizedOperationResult result) {
        if (!securityService.hasViewPermissionForPipeline(user, pipelineName)) {
            result.forbidden(EntityType.Pipeline.forbiddenToView(pipelineName, user.getUsername()), HealthStateType.forbiddenForPipeline(pipelineName));
            return null;
        }
        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        MingleConfig mingleConfig = pipelineConfig.getMingleConfig();
        return mingleConfig.equals(new MingleConfig()) ? null : mingleConfig;
    }

}
