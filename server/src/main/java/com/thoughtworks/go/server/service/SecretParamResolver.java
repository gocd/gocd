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
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.plugin.domain.secrets.Secret;
import com.thoughtworks.go.remote.work.BuildAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;

@Component
public class SecretParamResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretParamResolver.class);
    private SecretsExtension secretsExtension;
    private GoConfigService goConfigService;
    private RulesService rulesService;

    @Autowired
    public SecretParamResolver(SecretsExtension secretsExtension, GoConfigService goConfigService, RulesService rulesService) {
        this.secretsExtension = secretsExtension;
        this.goConfigService = goConfigService;
        this.rulesService = rulesService;
    }

    public void resolve(List<Material> materials) {
        materials.stream()
                .filter((material) -> material instanceof SecretParamAware)
                .forEach(this::resolve);
    }

    public void resolve(Material material) {
        if (material instanceof ScmMaterial) {
            this.resolve((ScmMaterial) material);
        } else if (material instanceof PluggableSCMMaterial) {
            this.resolve((PluggableSCMMaterial) material);
        } else if (material instanceof PackageMaterial) {
            this.resolve((PackageMaterial) material);
        }
    }

    private void resolve(ScmMaterial scmMaterial) {
        if (scmMaterial.hasSecretParams()) {
            rulesService.validateSecretConfigReferences(scmMaterial);

            resolve(scmMaterial.getSecretParams());
        } else {
            LOGGER.debug("No secret params to resolve in SCM material {}.", scmMaterial.getDisplayName());
        }
    }

    private void resolve(PluggableSCMMaterial material) {
        if (material.hasSecretParams()) {
            rulesService.validateSecretConfigReferences(material);

            resolve(material.getSecretParams());
        } else {
            LOGGER.debug("No secret params to resolve in pluggable SCM material {}.", material.getDisplayName());
        }
    }

    public void resolve(PackageMaterial material) {
        if (material.hasSecretParams()) {
            rulesService.validateSecretConfigReferences(material);
            resolve(material.getSecretParams());
        } else {
            LOGGER.debug("No secret params to resolve in package material {}.", material.getDisplayName());
        }
    }

    // Method used for check_connection in new pipeline flow
    public void resolve(ScmMaterial scmMaterial, String pipelineGroupName) {
        if (scmMaterial.hasSecretParams()) {
            rulesService.validateSecretConfigReferences(scmMaterial.getSecretParams(), PipelineConfigs.class, pipelineGroupName, format("Material with url: '%s' in Pipeline Group:", scmMaterial.getUriForDisplay()));
            resolve(scmMaterial.getSecretParams());
        } else {
            LOGGER.debug("No secret params to resolve in SCM material {}.", scmMaterial.getDisplayName());
        }
    }

    public void resolve(BuildAssignment buildAssignment) {
        if (buildAssignment.hasSecretParams()) {
            rulesService.validateSecretConfigReferences(buildAssignment);
            resolve(buildAssignment.getSecretParams());
        } else {
            LOGGER.debug("No secret params available in build assignment {}.", buildAssignment.getJobIdentifier());
        }
    }

    public void resolve(EnvironmentConfig environmentConfig) {
        if (environmentConfig.hasSecretParams()) {
            rulesService.validateSecretConfigReferences(environmentConfig);
            resolve(environmentConfig.getSecretParams());
        } else {
            LOGGER.debug("No secret params available in environment {}.", environmentConfig.name());
        }
    }

    public void resolve(SCM scmConfig) {
        if (scmConfig.hasSecretParams()) {
            rulesService.validateSecretConfigReferences(scmConfig);
            resolve(scmConfig.getSecretParams());
        } else {
            LOGGER.debug("No secret params available in pluggable SCM {}.", scmConfig.getName());
        }
    }

    protected void resolve(SecretParams secretParams) {
        secretParams.groupBySecretConfigId().forEach(lookupAndUpdateSecretParamsValue());
    }

    private BiConsumer<String, SecretParams> lookupAndUpdateSecretParamsValue() {
        return (secretConfigId, secretParamsToResolve) -> {
            Map<String, List<SecretParam>> secretParamMap = secretParamsToResolve.stream().collect(groupingBy(SecretParam::getKey, Collectors.toList()));
            final SecretConfig secretConfig = goConfigService.cruiseConfig().getSecretConfigs().find(secretConfigId);

            LOGGER.debug("Resolving secret params '{}' using secret config '{}'", secretParamMap.keySet(), secretConfig.getId());
            List<Secret> resolvedSecrets = secretsExtension.lookupSecrets(secretConfig.getPluginId(), secretConfig, secretParamMap.keySet());
            LOGGER.debug("Resolved secret size '{}'", resolvedSecrets.size());

            LOGGER.debug("Updating secret params '{}' with values.", secretParamMap.keySet());
            resolvedSecrets.forEach(assignValue(secretParamMap));
            LOGGER.debug("Secret params '{}' updated with values.", secretParamMap.keySet());
        };
    }

    private Consumer<Secret> assignValue(Map<String, List<SecretParam>> secretParamMap) {
        return secret -> secretParamMap.get(secret.getKey()).forEach(secretParam -> secretParam.setValue(secret.getValue()));
    }
}
