/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import java.util.List;

public class PatchEnvironmentCommand extends EnvironmentCommand {
    private final List<String> pipelinesToAdd;
    private final List<String> pipelinesToRemove;
    private final List<EnvironmentVariableConfig> envVarsToAdd;
    private final List<String> envVarsToRemove;

    public PatchEnvironmentCommand(GoConfigService goConfigService, EnvironmentConfig environmentConfig, List<String> pipelinesToAdd, List<String> pipelinesToRemove, List<EnvironmentVariableConfig> envVarsToAdd, List<String> envVarsToRemove, Username username, String actionFailed, HttpLocalizedOperationResult result) {
        super(actionFailed, environmentConfig, result, goConfigService, username);
        this.pipelinesToAdd = pipelinesToAdd;
        this.pipelinesToRemove = pipelinesToRemove;
        this.envVarsToAdd = envVarsToAdd;
        this.envVarsToRemove = envVarsToRemove;
    }

    @Override
    public void update(CruiseConfig configForEdit) throws Exception {
        EnvironmentConfig environmentConfig = configForEdit.getEnvironments().named(this.environmentConfig.name());

        for (String pipelineName : pipelinesToAdd) {
            environmentConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        }

        for (String pipelineName : pipelinesToRemove) {
            environmentConfig.removePipeline(new CaseInsensitiveString(pipelineName));
        }

        for (EnvironmentVariableConfig variableConfig : envVarsToAdd) {
            environmentConfig.addEnvironmentVariable(variableConfig);
        }

        for (String variableName : envVarsToRemove) {
            environmentConfig.getVariables().removeIfExists(variableName);
        }
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        EnvironmentConfig preprocessedEnvironmentConfig = preprocessedConfig.getEnvironments().find(environmentConfig.name());

        boolean isValid = validateRemovalOfInvalidEntities();

        if (preprocessedEnvironmentConfig instanceof MergeEnvironmentConfig) {
            isValid = validateRemovalOfRemoteEntities(preprocessedEnvironmentConfig);
        }

        return isValid && super.isValid(preprocessedConfig);
    }

    private boolean validateRemovalOfRemotePipelines(EnvironmentConfig preprocessedEnvironmentConfig) {
        for (String pipelineToRemove : pipelinesToRemove) {
            if (preprocessedEnvironmentConfig.containsPipelineRemotely(new CaseInsensitiveString(pipelineToRemove))) {
                String origin = ((MergeEnvironmentConfig) preprocessedEnvironmentConfig).getOriginForPipeline(new CaseInsensitiveString(pipelineToRemove)).displayName();
                String message = String.format("Pipeline '%s' cannot be removed from environment '%s' as the association has been defined remotely in [%s]",
                        pipelineToRemove, environmentConfig.name(), origin);
                result.unprocessableEntity(LocalizedMessage.composite(actionFailed, message));
                return false;
            }
        }
        return true;
    }

    private boolean validateRemovalOfEnvironmentVariables(EnvironmentConfig preprocessedEnvironmentConfig) {
        for (String variableName : envVarsToRemove) {
            if (preprocessedEnvironmentConfig.containsEnvironmentVariableRemotely(variableName)) {
                String origin = ((MergeEnvironmentConfig) preprocessedEnvironmentConfig).getOriginForEnvironmentVariable(variableName).displayName();
                String message = String.format("Environment variable with name '%s' cannot be removed from environment '%s' as the association has been defined remotely in [%s]",
                        variableName, environmentConfig.name(), origin);
                result.unprocessableEntity(LocalizedMessage.composite(actionFailed, message));
                return false;
            }
        }

        return true;
    }

    private boolean validateRemovalOfRemoteEntities(EnvironmentConfig preprocessedEnvironmentConfig) {
        boolean isValid = validateRemovalOfRemotePipelines(preprocessedEnvironmentConfig);
        isValid = isValid && validateRemovalOfEnvironmentVariables(preprocessedEnvironmentConfig);
        return isValid;
    }

    private boolean validateRemovalOfInvalidPipelines() {
        EnvironmentConfig environmentConfig = this.environmentConfig;
        for (String pipelineToRemove : pipelinesToRemove) {
            if (!environmentConfig.containsPipeline(new CaseInsensitiveString(pipelineToRemove))) {
                String message = String.format("Pipeline '%s' does not exist in environment '%s'", pipelineToRemove, environmentConfig.name());
                result.unprocessableEntity(LocalizedMessage.composite(actionFailed, message));
                return false;
            }
        }

        return true;
    }

    private boolean validateRemovalOfInvalidEnvironmentVariable() {
        EnvironmentConfig environmentConfig = this.environmentConfig;
        for (String variableName : envVarsToRemove) {
            if (!environmentConfig.getVariables().hasVariable(variableName)) {
                String message = String.format("Environment variable with name '%s' does not exist in environment '%s'", variableName, environmentConfig.name());
                result.unprocessableEntity(LocalizedMessage.composite(actionFailed, message));
                return false;
            }
        }
        return true;
    }

    private boolean validateRemovalOfInvalidEntities() {
        boolean isValid = validateRemovalOfInvalidPipelines();
        isValid = isValid && validateRemovalOfInvalidEnvironmentVariable();
        return isValid;
    }
}
