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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.domain.EnvironmentPipelineMatchers;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @understands the current persistent information related to multiple logical groupings of machines
 */
@ConfigTag("environments")
@ConfigCollection(BasicEnvironmentConfig.class)
public class EnvironmentsConfig extends BaseCollection<EnvironmentConfig> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public EnvironmentsConfig() {
    }

    public void validate(ValidationContext validationContext) {
        List<CaseInsensitiveString> allPipelineNames = validationContext.getCruiseConfig().getAllPipelineNames();
        List<CaseInsensitiveString> allEnvironmentNames = new ArrayList<>();
        Map<CaseInsensitiveString, CaseInsensitiveString> pipelineToEnvMap = new HashMap<>();
        for (EnvironmentConfig environmentConfig : this) {
            if (allEnvironmentNames.contains(environmentConfig.name())) {
                environmentConfig.addError("name", String.format("Environment with name '%s' already exists.", environmentConfig.name()));
            } else {
                allEnvironmentNames.add(environmentConfig.name());
            }

            for (EnvironmentPipelineConfig pipeline : environmentConfig.getPipelines()) {
                if (!allPipelineNames.contains(pipeline.getName())) {
                    environmentConfig.addError("pipeline", String.format("Environment '%s' refers to an unknown pipeline '%s'.", environmentConfig.name(), pipeline.getName()));
                }
                if (pipelineToEnvMap.containsKey(pipeline.getName())) {
                    environmentConfig.addError("pipeline", "Associating pipeline(s) which is already part of " + pipelineToEnvMap.get(pipeline.getName()) + " environment");
                } else {
                    pipelineToEnvMap.put(pipeline.getName(), environmentConfig.name());
                }
            }
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean validateContainOnlyUuids(Set<String> uuids) {
        boolean isValid = true;
        for (EnvironmentConfig environmentConfig : this) {
            isValid = environmentConfig.validateContainsOnlyUuids(uuids) && isValid;
        }
        return isValid;
    }

    public void addAgentsToEnvironment(String environmentName, String... uuids) {
        EnvironmentConfig environment = getOrCreateEnvironment(environmentName);
        for (String uuid : uuids) {
            environment.addAgent(uuid);
        }
    }

    private EnvironmentConfig getOrCreateEnvironment(String environmentName) {
        for (EnvironmentConfig config : this) {
            if (config.hasName(new CaseInsensitiveString(environmentName))) {
                return config;
            }
        }
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        add(config);
        return config;
    }

    public EnvironmentPipelineMatcher matchersForPipeline(String pipelineName) {
        for (EnvironmentConfig config : this) {
            if (config.containsPipeline(new CaseInsensitiveString(pipelineName))) {
                return config.createMatcher();
            }
        }
        return null;
    }

    @Override
    public boolean add(EnvironmentConfig environment) {
        return super.add(environment);
    }

    public void addPipelinesToEnvironment(String environmentName, String... pipelineNames) {
        EnvironmentConfig environment = getOrCreateEnvironment(environmentName);
        for (String pipelineName : pipelineNames) {
            environment.addPipeline(new CaseInsensitiveString(pipelineName));
        }
    }

    public EnvironmentPipelineMatchers matchers() {
        EnvironmentPipelineMatchers environmentPipelineMatchers = new EnvironmentPipelineMatchers();
        for (EnvironmentConfig environment : this) {
            environmentPipelineMatchers.add(environment.createMatcher());
        }
        return environmentPipelineMatchers;
    }

    public CaseInsensitiveString findEnvironmentNameForPipeline(final CaseInsensitiveString pipelineName) {
        EnvironmentConfig environment = findEnvironmentForPipeline(pipelineName);
        return environment == null ? null : environment.name();
    }

    public EnvironmentConfig findEnvironmentForPipeline(final CaseInsensitiveString pipelineName) {
        for (EnvironmentConfig config : this) {
            if (config.containsPipeline(pipelineName)) {
                return config;
            }
        }
        return null;
    }

    public boolean isPipelineAssociatedWithAnyEnvironment(final CaseInsensitiveString pipelineName) {
        for (EnvironmentConfig environment : this) {
            if (environment.containsPipeline(pipelineName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPipelineAssociatedWithRemoteEnvironment(final CaseInsensitiveString pipelineName) {
        for (EnvironmentConfig environment : this) {
            if (environment.containsPipelineRemotely(pipelineName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAgentUnderEnvironment(String agentUuid) {
        for (EnvironmentConfig environment : this) {
            if (environment.hasAgent(agentUuid)) {
                return true;
            }
        }
        return false;
    }

    public EnvironmentConfig named(final CaseInsensitiveString envName) {
        EnvironmentConfig environmentConfig = find(envName);
        if (environmentConfig != null) return environmentConfig;
        throw new RecordNotFoundException("Environment named " + envName + " was not found!");
    }

    public EnvironmentConfig find(CaseInsensitiveString envName) {
        for (EnvironmentConfig environmentConfig : this) {
            if (environmentConfig.name().equals(envName)) {
                return environmentConfig;
            }
        }
        return null;
    }

    public List<CaseInsensitiveString> names() {
        ArrayList<CaseInsensitiveString> names = new ArrayList<>();
        for (EnvironmentConfig environment : this) {
            names.add(environment.name());
        }
        return names;
    }

    public TreeSet<String> environmentsForAgent(String agentUuid) {
        return environmentConfigsForAgent(agentUuid).stream()
                .map(environmentConfig -> CaseInsensitiveString.str(environmentConfig.name()))
                .collect(Collectors.toCollection(() -> new TreeSet<>(new AlphaAsciiComparator())));
    }

    public Set<EnvironmentConfig> environmentConfigsForAgent(String agentUuid) {
        return this.stream()
                .filter(environmentConfig -> environmentConfig.hasAgent(agentUuid))
                .collect(Collectors.toSet());
    }

    public boolean hasEnvironmentNamed(CaseInsensitiveString environmentName) {
        return find(environmentName) != null;
    }


    public void removeAgentFromAllEnvironments(String uuid) {
        for (EnvironmentConfig environmentConfig : this) {
            environmentConfig.removeAgent(uuid);
        }
    }

    public EnvironmentsConfig getLocal() {
        EnvironmentsConfig locals = new EnvironmentsConfig();
        for (EnvironmentConfig environmentConfig : this) {
            EnvironmentConfig local = environmentConfig.getLocal();
            if (local != null)
                locals.add(local);
        }
        return locals;
    }
}
