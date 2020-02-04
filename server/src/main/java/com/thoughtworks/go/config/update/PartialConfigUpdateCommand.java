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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.ErrorCollector;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.plugin.access.configrepo.InvalidPartialConfigException;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.rules.SupportedEntity.*;
import static java.lang.String.format;

public class PartialConfigUpdateCommand implements UpdateConfigCommand {
    private static final Cloner CLONER = new Cloner();

    private final PartialConfig partial;
    private final String fingerprint;
    private final PartialConfigResolver resolver;
    private final ConfigRepoConfig configRepoConfig;

    public PartialConfigUpdateCommand(final PartialConfig partial, final String fingerprint, PartialConfigResolver resolver, ConfigRepoConfig configRepoConfig) {
        this.partial = partial;
        this.fingerprint = fingerprint;
        this.resolver = resolver;
        this.configRepoConfig = configRepoConfig;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) {
        if (partial != null && fingerprint != null) {
            PartialConfig config = resolver.findPartialByFingerprint(cruiseConfig, fingerprint);

            if (null != config) {
                cruiseConfig.getPartials().remove(config);
            }

            PartialConfig cloned = CLONER.deepClone(partial);
            cruiseConfig.getPartials().add(cloned);

            if (!validateEntityForRules(cloned)) {
                return cruiseConfig;
            }

            for (PartialConfig partial : cruiseConfig.getPartials()) {
                for (EnvironmentConfig environmentConfig : partial.getEnvironments()) {
                    if (!cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentConfig.name())) {
                        cruiseConfig.addEnvironment(new BasicEnvironmentConfig(environmentConfig.name()));
                    }
                }
                for (PipelineConfigs pipelineConfigs : partial.getGroups()) {
                    if (!cruiseConfig.getGroups().hasGroup(pipelineConfigs.getGroup())) {
                        cruiseConfig.getGroups().add(new BasicPipelineConfigs(pipelineConfigs.getGroup(), new Authorization()));
                    }
                }
            }
        }
        return cruiseConfig;
    }

    private boolean validateEntityForRules(PartialConfig partialConfig) {
        //preflight check
        if (configRepoConfig == null) {
            return true;
        }

        if (configRepoConfig.getRules().isEmpty()) {
            throw new InvalidPartialConfigException(partialConfig, "Configurations can not be merged as no rules are defined.");
        }

        partialConfig.getEnvironments().stream()
                .filter(env -> !configRepoConfig.canRefer(ENVIRONMENT.getEntityType(), env.name().toString()))
                .forEach(envThatCanNotBeReferred -> {
                    envThatCanNotBeReferred.addError(ENVIRONMENT.getType(), format("Not allowed to refer environment '%s' from the config repository.", envThatCanNotBeReferred.name()));
                });

        partialConfig.getGroups().stream()
                .filter(pipelineGrp -> !configRepoConfig.canRefer(PIPELINE_GROUP.getEntityType(), pipelineGrp.getGroup()))
                .forEach(pipelineGrpThatCannotBeReferred -> {
                    pipelineGrpThatCannotBeReferred.addError(PIPELINE_GROUP.getType(), format("Not allowed to refer pipeline group '%s' from the config repository.", pipelineGrpThatCannotBeReferred.getGroup()));
                });

        List<DependencyMaterialConfig> dependencyMaterialConfigs = new ArrayList<>();
        partialConfig.getGroups().forEach((pipelineGrp) -> {
            pipelineGrp.forEach((pipelineConfig) -> dependencyMaterialConfigs.addAll(pipelineConfig.dependencyMaterialConfigs()));
        });
        dependencyMaterialConfigs.stream()
                .filter(dependencyMaterialConfig -> !doesPipelineExistInPartialConfig(partialConfig, dependencyMaterialConfig.getPipelineName()))
                .filter(dependencyMaterialConfig -> !configRepoConfig.canRefer(PIPELINE.getEntityType(), dependencyMaterialConfig.getPipelineName().toString()))
                .forEach(dependencyMaterialConfigThatCannotBeReferred -> {
                    dependencyMaterialConfigThatCannotBeReferred.addError(PIPELINE.getType(), format("Not allowed to refer pipeline '%s' from the config repository.", dependencyMaterialConfigThatCannotBeReferred.getPipelineName()));
                });

        return ErrorCollector.getAllErrors(partialConfig).isEmpty();
    }

    private boolean doesPipelineExistInPartialConfig(PartialConfig partialConfig, CaseInsensitiveString pipelineName) {
        return partialConfig.getGroups().stream().anyMatch(group -> group.findBy(pipelineName) != null);
    }
}
