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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.update.SecretConfigCreateCommand;
import com.thoughtworks.go.config.update.SecretConfigDeleteCommand;
import com.thoughtworks.go.config.update.SecretConfigUpdateCommand;
import com.thoughtworks.go.domain.SecretConfigUsage;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SecretConfigService extends NewPluginProfilesService<SecretConfig> {
    private final SecretsExtension secretsExtension;

    @Autowired
    public SecretConfigService(GoConfigService goConfigService, EntityHashingService hashingService, SecretsExtension secretsExtension) {
        super(goConfigService, hashingService);
        this.secretsExtension = secretsExtension;
    }

    @Override
    protected NewPluginProfiles<SecretConfig> getPluginProfiles() {
        return goConfigService.cruiseConfig().getSecretConfigs();
    }

    public SecretConfigs getAllSecretConfigs() {
        return (SecretConfigs) getPluginProfiles();
    }

    public void create(Username currentUser, SecretConfig secretConfig, LocalizedOperationResult result) {
        SecretConfigCreateCommand command = new SecretConfigCreateCommand(goConfigService, secretConfig, secretsExtension, currentUser, result);
        update(currentUser, secretConfig, result, command);
    }

    public void update(Username currentUser, String md5, SecretConfig secretConfig, LocalizedOperationResult result) {
        SecretConfigUpdateCommand command = new SecretConfigUpdateCommand(goConfigService, secretConfig, secretsExtension, currentUser, result, hashingService, md5);
        update(currentUser, secretConfig, result, command);
    }

    public void delete(Username currentUser, SecretConfig secretConfig, LocalizedOperationResult result) {
        SecretConfigDeleteCommand command = new SecretConfigDeleteCommand(goConfigService, secretConfig, getUsageInformation(secretConfig.getId()), secretsExtension, currentUser, result);
        update(currentUser, secretConfig, result, command);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.SecretConfig.deleteSuccessful(secretConfig.getId()));
        }
    }

    public Set<SecretConfigUsage> getUsageInformation(String configId) {
        if (findProfile(configId) == null) {
            throw new RecordNotFoundException(EntityType.SecretConfig, configId);
        }

        final Set<SecretConfigUsage> usages = new HashSet<>();

        for (PipelineConfig pipelineConfig : goConfigService.getAllPipelineConfigs()) {
            getPipelineUsage(pipelineConfig, configId).ifPresent(usage -> usages.add(usage));

            for (StageConfig stage : pipelineConfig.getStages()) {
                getStageUSage(pipelineConfig, stage, configId).ifPresent(usage -> usages.add(usage));

                for (JobConfig job : stage.getJobs()) {
                    getJobUSage(pipelineConfig, stage, job, configId).ifPresent(usage -> usages.add(usage));
                }
            }
        }
        return usages;
    }

    private Optional<SecretConfigUsage> getJobUSage(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig, String configId) {
        EnvironmentVariablesConfig secureVariables = jobConfig.getVariables().getSecureVariables();

        if (isUsingSecretConfig(configId, secureVariables)) {
            return Optional.of(createSecretConfigUsage(pipelineConfig, stageConfig, jobConfig));
        }
        return Optional.empty();
    }

    private SecretConfigUsage createSecretConfigUsage(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig) {
        return new SecretConfigUsage(
                pipelineConfig.getName().toString(),
                stageConfig == null? null : stageConfig.name().toString(),
                jobConfig == null ? null : jobConfig.name().toString(),
                getTemplateName(pipelineConfig),
                getOrigin(pipelineConfig)
        );
    }

    private Optional<SecretConfigUsage> getStageUSage(PipelineConfig pipelineConfig, StageConfig stageConfig, String configId) {
        EnvironmentVariablesConfig secureVariables = stageConfig.getVariables().getSecureVariables();

        if (isUsingSecretConfig(configId, secureVariables)) {
            return Optional.of(createSecretConfigUsage(pipelineConfig, stageConfig, null));
        }
        return Optional.empty();
    }

    private Optional<SecretConfigUsage> getPipelineUsage(PipelineConfig pipelineConfig, String configId) {
        EnvironmentVariablesConfig secureVariables = pipelineConfig.getVariables().getSecureVariables();

        if (isUsingSecretConfig(configId, secureVariables)) {
            return Optional.of(createSecretConfigUsage(pipelineConfig, null, null));
        }
        return Optional.empty();
    }

    private boolean isUsingSecretConfig(String configId, EnvironmentVariablesConfig secureVariables) {
        return secureVariables
                .stream()
                .filter(var -> var.getSecretParams().hasSecretParams())
                .anyMatch(var -> var.getSecretParams().findFirstByConfigId(configId).isPresent());
    }

    private String getTemplateName(PipelineConfig pipelineConfig) {
        String templateName = null;
        if (pipelineConfig.getTemplateName() != null) {
            templateName = pipelineConfig.getTemplateName().toString();
        }
        return templateName;
    }

    private String getOrigin(PipelineConfig pipelineConfig) {
        return pipelineConfig.getOrigin() instanceof FileConfigOrigin ? "gocd" : "config_repo";
    }

}
