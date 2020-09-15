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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.server.exceptions.RulesViolationException.throwCannotRefer;
import static com.thoughtworks.go.server.exceptions.RulesViolationException.throwSecretConfigNotFound;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;

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

        Map<CaseInsensitiveString, StringBuilder> pipelinesWithErrors = new HashMap<>();
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
        if (!pipelinesWithErrors.isEmpty()) {
            LOGGER.debug("[Material Update] Failure: {}", errorString(pipelinesWithErrors));
        }
        if (pipelines.size() == pipelinesWithErrors.size()) {
            throw new RulesViolationException(errorString(pipelinesWithErrors));
        }

        return true;
    }

    public void validateSecretConfigReferences(PluggableSCMMaterial pluggableSCMMaterial) {
        SCM scmConfig = pluggableSCMMaterial.getScmConfig();
        validateSecretConfigReferences(scmConfig);
    }

    public void validateSecretConfigReferences(PackageMaterial packageMaterial) {
        PackageRepository pkgRepository = packageMaterial.getPackageDefinition().getRepository();
        Map<CaseInsensitiveString, StringBuilder> errors = validate(packageMaterial.getSecretParams(), pkgRepository.getClass(), pkgRepository.getName(), "Package Material");

        if (!errors.isEmpty()) {
            throw new RulesViolationException(errorString(errors));
        }
    }

    public boolean validateSecretConfigReferences(EnvironmentConfig environmentConfig) {
        SecretParams secretParams = environmentConfig.getSecretParams();
        validateSecretConfigReferences(secretParams, environmentConfig.getClass(), environmentConfig.name().toString(), "Environment");
        return true;
    }

    public void validateSecretConfigReferences(BuildAssignment buildAssignment) {
        SecretParams secretParams = buildAssignment.getSecretParams();
        JobIdentifier jobIdentifier = buildAssignment.getJobIdentifier();
        PipelineConfigs group = goConfigService.findGroupByPipeline(new CaseInsensitiveString(jobIdentifier.getPipelineName()));
        String errorMessagePrefix = format("Job: '%s' in Pipeline: '%s' and Pipeline Group:", jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        validateSecretConfigReferences(secretParams, group.getClass(), group.getGroup(), errorMessagePrefix);
    }

    public void validateSecretConfigReferences(SCM scmConfig) {
        Map<CaseInsensitiveString, StringBuilder> ruleViolationErrors = validate(scmConfig.getSecretParams(), scmConfig.getClass(), scmConfig.getName(), "Pluggable SCM");

        if (!ruleViolationErrors.isEmpty()) {
            throw new RulesViolationException(errorString(ruleViolationErrors));
        }
    }

    public void validateSecretConfigReferences(PackageRepository packageRepository) {
        Map<CaseInsensitiveString, StringBuilder> errors = validate(packageRepository.getSecretParams(), packageRepository.getClass(), packageRepository.getName(), "Package Repository");

        if (!errors.isEmpty()) {
            throw new RulesViolationException(errorString(errors));
        }
    }

    public void validateSecretConfigReferences(PackageDefinition packageDefinition) {
        Map<CaseInsensitiveString, StringBuilder> errors = validate(packageDefinition.getSecretParams(), packageDefinition.getRepository().getClass(), packageDefinition.getRepository().getName(), "Package Repository");

        if (!errors.isEmpty()) {
            throw new RulesViolationException(errorString(errors));
        }
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

    private void addError(Map<CaseInsensitiveString, StringBuilder> pipelinesWithErrors, CaseInsensitiveString pipelineName, String message) {
        if (pipelinesWithErrors == null) {
            pipelinesWithErrors = new HashMap<>();
        }
        if (!pipelinesWithErrors.containsKey(pipelineName)) {
            pipelinesWithErrors.put(pipelineName, new StringBuilder());
        }
        StringBuilder stringBuilder = pipelinesWithErrors.get(pipelineName).append(message).append('\n');
        pipelinesWithErrors.put(pipelineName, stringBuilder);
    }

    private String errorString(Map<CaseInsensitiveString, StringBuilder> errors) {
        return join(errors.values(), '\n').trim();
    }

    protected Map<CaseInsensitiveString, StringBuilder> validate(SecretParams secretParams, Class<? extends Validatable> entityClass, String entityName, String entityNameOrErrorMessagePrefix) {
        Map<CaseInsensitiveString, StringBuilder> pipelinesWithErrors = new HashMap<>();
        secretParams
                .groupBySecretConfigId()
                .forEach((secretConfigId, secretParam) -> {
                    SecretConfig secretConfig = goConfigService.getSecretConfigById(secretConfigId);
                    if (secretConfig == null) {
                        addError(pipelinesWithErrors, new CaseInsensitiveString(entityName), format("%s '%s' is referring to none-existent secret config '%s'.", entityNameOrErrorMessagePrefix, entityName, secretConfigId));
                    } else if (!secretConfig.canRefer(entityClass, entityName)) {
                        addError(pipelinesWithErrors, new CaseInsensitiveString(entityName), format("%s '%s' does not have permission to refer to secrets using secret config '%s'.", entityNameOrErrorMessagePrefix, entityName, secretConfigId));
                    }
                });
        return pipelinesWithErrors;
    }
}
