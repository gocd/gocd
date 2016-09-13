/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import java.util.ArrayList;
import java.util.List;

public class PatchEnvironmentCommand extends EnvironmentCommand implements EntityConfigUpdateCommand<EnvironmentConfig> {
    private final GoConfigService goConfigService;
    private final EnvironmentConfig environmentConfig;
    private final List<String> pipelinesToAdd;
    private final List<String> pipelinesToRemove;
    private final List<String> agentsToAdd;
    private final List<String> agentsToRemove;
    private final Username username;
    private final Localizable.CurryableLocalizable actionFailed;
    private final HttpLocalizedOperationResult result;

    public PatchEnvironmentCommand(GoConfigService goConfigService, EnvironmentConfig environmentConfig, List<String> pipelinesToAdd, List<String> pipelinesToRemove, List<String> agentsToAdd, List<String> agentsToRemove, Username username, Localizable.CurryableLocalizable actionFailed, HttpLocalizedOperationResult result) {
        super(actionFailed, environmentConfig, result);

        this.goConfigService = goConfigService;
        this.environmentConfig = environmentConfig;
        this.pipelinesToAdd = pipelinesToAdd;
        this.pipelinesToRemove = pipelinesToRemove;
        this.agentsToAdd = agentsToAdd;
        this.agentsToRemove = agentsToRemove;
        this.username = username;
        this.actionFailed = actionFailed;
        this.result = result;
    }

    private boolean validateAgents(List<String> uuids, LocalizedOperationResult result, Agents agents) {
        List<String> unknownAgents = new ArrayList<>();

        for (String uuid : uuids) {
            AgentConfig agent = agents.getAgentByUuid(uuid);
            if (agent.isNull()) {
                unknownAgents.add(uuid);
            }
        }

        if (!unknownAgents.isEmpty()) {
            result.badRequest(LocalizedMessage.string("AGENTS_WITH_UUIDS_NOT_FOUND", unknownAgents));
            return false;
        }
        return true;
    }

    private boolean validatePipelines(List<String> pipelines, HttpLocalizedOperationResult result, CruiseConfig config) {
        ArrayList<String> unknownPipelines = new ArrayList<>();

        for (String pipeline : pipelines) {
            try{
                config.pipelineConfigByName(new CaseInsensitiveString(pipeline));
            } catch(PipelineNotFoundException e){
                unknownPipelines.add(pipeline);
            }
        }

        if (!unknownPipelines.isEmpty()) {
            result.badRequest(LocalizedMessage.string("PIPELINES_WITH_NAMES_NOT_FOUND", unknownPipelines));
            return false;
        }
        return true;

    }


    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        EnvironmentsConfig environments = preprocessedConfig.getEnvironments();
        int index = environments.indexOf(environmentConfig);
        EnvironmentConfig preprocessedEnvironmentConfig = environments.get(index);

        if(isValidConfig(preprocessedConfig)){
            for (String uuid : agentsToAdd) {
                preprocessedEnvironmentConfig.addAgent(uuid);
            }

            for (String uuid : agentsToRemove) {
                preprocessedEnvironmentConfig.removeAgent(uuid);
            }

            for (String pipelineName : pipelinesToAdd) {
                preprocessedEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipelineName));
            }

            for (String pipelineName : pipelinesToRemove) {
                preprocessedEnvironmentConfig.removePipeline(new CaseInsensitiveString(pipelineName));
            }
        }
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(environmentConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!goConfigService.isAdministrator(username.getUsername())) {
            Localizable noPermission = LocalizedMessage.string("NO_PERMISSION_TO_UPDATE_ENVIRONMENT", environmentConfig.name().toString(), username.getDisplayName());
            result.unauthorized(noPermission, HealthStateType.unauthorised());
            return false;
        }
        return true;
    }

    public boolean isValidConfig(CruiseConfig preprocessedConfig) {
        boolean isValid = validateAgents(agentsToAdd, result, preprocessedConfig.agents());
        isValid = isValid && validateAgents(agentsToRemove, result, preprocessedConfig.agents());
        isValid = isValid && validatePipelines(pipelinesToAdd, result, preprocessedConfig);
        isValid = isValid && validatePipelines(pipelinesToRemove, result, preprocessedConfig);
        return isValid;
    }
}
