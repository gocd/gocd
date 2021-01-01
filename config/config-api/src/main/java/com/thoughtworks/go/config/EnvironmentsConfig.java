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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.domain.EnvironmentPipelineMatchers;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static java.util.stream.Collectors.*;

/**
 * @understands the current persistent information related to multiple logical groupings of machines
 */
@ConfigTag("environments")
@ConfigCollection(BasicEnvironmentConfig.class)
public class EnvironmentsConfig extends BaseCollection<EnvironmentConfig> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public EnvironmentsConfig() {
    }

    @Override
    public void validate(ValidationContext validationContext) {
        List<CaseInsensitiveString> allEnvironmentNames = new ArrayList<>();
        Map<CaseInsensitiveString, CaseInsensitiveString> pipelineToEnvMap = new HashMap<>();

        List<CaseInsensitiveString> allPipelineNames = validationContext.getCruiseConfig().getAllPipelineNames();
        for (EnvironmentConfig envConfig : this) {
            if (allEnvironmentNames.contains(envConfig.name())) {
                envConfig.addError("name", String.format("Environment with name '%s' already exists.", envConfig.name()));
            } else {
                allEnvironmentNames.add(envConfig.name());
            }

            for (EnvironmentPipelineConfig pipeline : envConfig.getPipelines()) {
                if (!allPipelineNames.contains(pipeline.getName())) {
                    envConfig.addError("pipeline", String.format("Environment '%s' refers to an unknown pipeline '%s'.", envConfig.name(), pipeline.getName()));
                }
                if (pipelineToEnvMap.containsKey(pipeline.getName())) {
                    envConfig.addError("pipeline", "Associating pipeline(s) which is already part of " + pipelineToEnvMap.get(pipeline.getName()) + " environment");
                } else {
                    pipelineToEnvMap.put(pipeline.getName(), envConfig.name());
                }
            }
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    EnvironmentPipelineMatcher matchersForPipeline(String pipelineName) {
        return this.stream()
                .filter(envConfig -> envConfig.containsPipeline(new CaseInsensitiveString(pipelineName)))
                .findFirst()
                .map(EnvironmentConfig::createMatcher)
                .orElse(null);
    }

    @Override
    public boolean add(EnvironmentConfig environment) {
        return super.add(environment);
    }

    public void addPipelinesToEnvironment(String envName, String... pipelineNames) {
        EnvironmentConfig envConfig = getOrCreateEnvironment(envName);
        Arrays.stream(pipelineNames).map(CaseInsensitiveString::new).forEach(envConfig::addPipeline);
    }

    public EnvironmentPipelineMatchers matchers() {
        return this.stream()
                .map(EnvironmentConfig::createMatcher)
                .collect(toCollection(EnvironmentPipelineMatchers::new));
    }

    public CaseInsensitiveString findEnvironmentNameForPipeline(final CaseInsensitiveString pipelineName) {
        EnvironmentConfig envConfig = findEnvironmentForPipeline(pipelineName);
        return envConfig == null ? null : envConfig.name();
    }

    public EnvironmentConfig findEnvironmentForPipeline(final CaseInsensitiveString pipelineName) {
        return this.stream().filter(env -> env.containsPipeline(pipelineName)).findFirst().orElse(null);
    }

    public boolean isPipelineAssociatedWithAnyEnvironment(final CaseInsensitiveString pipelineName) {
        return this.stream().anyMatch(env -> env.containsPipeline(pipelineName));
    }

    boolean isPipelineAssociatedWithRemoteEnvironment(final CaseInsensitiveString pipelineName) {
        return this.stream().anyMatch(env -> env.containsPipelineRemotely(pipelineName));
    }

    public boolean isAgentAssociatedWithEnvironment(String uuid) {
        return this.stream().anyMatch(env -> env.hasAgent(uuid));
    }

    public EnvironmentConfig named(CaseInsensitiveString env) {
        EnvironmentConfig envConfig = find(env);
        if (envConfig == null) {
            throw new RecordNotFoundException(EntityType.Environment, env);
        }
        return envConfig;
    }

    public EnvironmentConfig find(CaseInsensitiveString env) {
        return this.stream()
                .filter(envConfig -> envConfig.name().equals(env))
                .findFirst()
                .orElse(null);
    }

    public List<CaseInsensitiveString> names() {
        return this.stream().map(EnvironmentConfig::name).collect(toList());
    }

    public Set<String> getAgentEnvironmentNames(String uuid) {
        return this.stream()
                .filter(envConfig -> envConfig.hasAgent(uuid))
                .map(envConfig -> str(envConfig.name()))
                .collect(toCollection(HashSet::new));
    }

    public Set<EnvironmentConfig> getAgentEnvironments(String uuid) {
        return this.stream().filter(envConfig -> envConfig.hasAgent(uuid)).collect(toSet());
    }

    public boolean hasEnvironmentNamed(CaseInsensitiveString environmentName) {
        EnvironmentConfig environmentConfig = find(environmentName);
        return environmentConfig != null;
    }

    public void removeAgentFromAllEnvironments(String uuid) {
        this.forEach(envConfig -> envConfig.removeAgent(uuid));
    }

    public EnvironmentsConfig getLocal() {
        return this.stream()
                .filter(envConfig -> envConfig.getLocal() != null)
                .collect(toCollection(EnvironmentsConfig::new));
    }

    private EnvironmentConfig getOrCreateEnvironment(String envName) {
        return this.stream()
                .filter(envConfig -> envConfig.hasName(new CaseInsensitiveString(envName)))
                .findAny()
                .orElseGet(() -> createNewEnvironmentConfigAndAddToList(envName));
    }

    private EnvironmentConfig createNewEnvironmentConfigAndAddToList(String envName) {
        BasicEnvironmentConfig newEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(envName));
        add(newEnvConfig);
        return newEnvConfig;
    }
}
