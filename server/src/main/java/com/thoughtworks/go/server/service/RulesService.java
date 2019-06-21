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
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.server.exceptions.RulesViolationException.throwCannotRefer;
import static com.thoughtworks.go.server.exceptions.RulesViolationException.throwSecretConfigNotFound;
import static java.lang.String.format;

@Service
public class RulesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RulesService.class);
    private GoConfigService goConfigService;

    @Autowired
    public RulesService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean validateSecretConfigReferences(ScmMaterial scmMaterial) {
        List<CaseInsensitiveString> pipelines = goConfigService.pipelinesWithMaterial(scmMaterial.getFingerprint());

        HashMap<CaseInsensitiveString, StringBuilder> pipelinesWithErrors = new HashMap<>();
        pipelines.forEach(pipelineName -> {
            MaterialConfig materialConfig = goConfigService
                    .findPipelineByName(pipelineName)
                    .materialConfigs()
                    .getByMaterialFingerPrint(scmMaterial.getFingerprint());
            PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineName);
            ScmMaterialConfig scmMaterialConfig = (ScmMaterialConfig) materialConfig;
            SecretParams secretParams = SecretParams.parse(scmMaterialConfig.getPassword());
            secretParams.forEach(secretParam -> {
                String secretConfigId = secretParam.getSecretConfigId();
                SecretConfig secretConfig = goConfigService.getSecretConfigById(secretConfigId);
                if (secretConfig == null) {
                    addError(pipelinesWithErrors, pipelineName, format("Pipeline '%s' is referring to none-existent secret config '%s'.", pipelineName, secretConfigId));
                } else if (!secretConfig.canRefer(group.getClass(), group.getGroup())) {
                    addError(pipelinesWithErrors, pipelineName, format("Pipeline '%s' does not have permission to refer to secrets using secret config '%s'", pipelineName, secretConfigId));
                }
            });
        });
        StringBuilder errorMessage = new StringBuilder();
        if (!pipelinesWithErrors.isEmpty()) {
            errorMessage.append(StringUtils.join(pipelinesWithErrors.values(), '\n').trim());
            LOGGER.error("[Material Update] Failure: {}", errorMessage.toString());
        }
        if (pipelines.size() == pipelinesWithErrors.size()) {
            throw new RulesViolationException(errorMessage.toString());
        }
        return true;
    }

    private void addError(HashMap<CaseInsensitiveString, StringBuilder> pipelinesWithErrors, CaseInsensitiveString pipelineName, String message) {
        if (pipelinesWithErrors == null) {
            pipelinesWithErrors = new HashMap<>();
        }
        if (!pipelinesWithErrors.containsKey(pipelineName)) {
            pipelinesWithErrors.put(pipelineName, new StringBuilder());
        }
        StringBuilder stringBuilder = pipelinesWithErrors.get(pipelineName).append(message);
        pipelinesWithErrors.put(pipelineName, stringBuilder);
    }

    public boolean validateSecretConfigReferences(EnvironmentConfig environmentConfig) {
        SecretParams secretParams = environmentConfig.getSecretParams();
        validateSecretConfigReferences(secretParams, environmentConfig.getClass(), environmentConfig.name().toString(), "Environment");
        return true;
    }

    public void validateSecretConfigReferences(BuildAssignment buildAssignment) {
        SecretParams secretParams = buildAssignment.getSecretParams();
        if (secretParams.isEmpty()) {
            LOGGER.debug("No secret params available in build assignment {}.", buildAssignment.getJobIdentifier());
            return;
        }

        JobIdentifier jobIdentifier = buildAssignment.getJobIdentifier();
        PipelineConfigs group = goConfigService.findGroupByPipeline(new CaseInsensitiveString(jobIdentifier.getPipelineName()));
        String errorMessagePrefix = format("Job: '%s' in Pipeline: '%s' and Pipeline Group:", jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        validateSecretConfigReferences(secretParams, group.getClass(), group.getGroup(), errorMessagePrefix);
    }

    protected void validateSecretConfigReferences(SecretParams secretParams, Class<? extends Validatable> entityClass, String entityName, String entityNameOrErrorMessagePrefix) {
        secretParams.forEach(secretParam -> {
            SecretConfig secretConfig = goConfigService.cruiseConfig().getSecretConfigs().find(secretParam.getSecretConfigId());

            if (secretConfig == null) {
                throwSecretConfigNotFound(entityNameOrErrorMessagePrefix, entityName, secretParam.getSecretConfigId());
            }

            if (!secretConfig.canRefer(entityClass, entityName)) {
                throwCannotRefer(entityNameOrErrorMessagePrefix, entityName, secretParam.getSecretConfigId());
            }
        });
    }
}
