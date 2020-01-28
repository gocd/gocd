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
package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * List of remote configuration sources and how to interpret them
 */
@ConfigTag("config-repos")
@ConfigCollection(value = ConfigRepoConfig.class)
public class ConfigReposConfig extends BaseCollection<ConfigRepoConfig> implements Validatable, Cacheable {

    private ConfigErrors errors = new ConfigErrors();

    public ConfigReposConfig() {
    }

    public ConfigReposConfig(ConfigRepoConfig... configRepos) {
        this.addAll(Arrays.asList(configRepos));
    }

    public boolean hasMaterial(MaterialConfig materialConfig) {
        for (ConfigRepoConfig c : this) {
            if (c.getRepo().equals(materialConfig)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        this.validateMaterialUniqueness();
        this.validateIdUniqueness();
    }

    public boolean isUniqueId(final String id) {
        return Collections.frequency(allIds(), id) <= 1;
    }

    public boolean isUniqueMaterial(final String fingerPrint) {
        return Collections.frequency(allMaterialFingerPrints(), fingerPrint) <= 1;
    }

    private void validateIdUniqueness() {
        if (new HashSet<>(allIds()).size() != allIds().size()) {
            this.errors().add("id", "You have defined multiple configuration repositories with the same id.");
        }
    }

    private void validateMaterialUniqueness() {
        if (new HashSet<>(allMaterialFingerPrints()).size() != allMaterialFingerPrints()
                .size()) {
            this.errors().add("material", "You have defined multiple configuration repositories with the same repository.");
        }
    }

    private List<String> allIds() {
        return this.stream().map(ConfigRepoConfig::getId).collect(Collectors.toList());
    }

    private List<String> allMaterialFingerPrints() {
        return this.stream().map(cr -> cr.getRepo().getFingerprint()).collect(Collectors.toList());
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    public ConfigErrors getAllErrors() {
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.addAll(errors);
        for (ConfigRepoConfig repoConfig : this) {
            configErrors.addAll(repoConfig.errors());
        }

        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.errors().add(fieldName, message);
    }

    public ConfigRepoConfig getConfigRepo(MaterialConfig config) {
        for (ConfigRepoConfig repoConfig : this) {
            if (repoConfig.hasSameMaterial(config)) {
                return repoConfig;
            }
        }
        return null;
    }

    public ConfigRepoConfig getConfigRepo(String id) {
        for (ConfigRepoConfig repoConfig : this) {
            if (repoConfig.getId().equals(id)) {
                return repoConfig;
            }
        }

        return null;
    }


    public boolean hasMaterialWithFingerprint(String fingerprint) {
        for (ConfigRepoConfig repoConfig : this) {
            if (repoConfig.hasMaterialWithFingerprint(fingerprint)) {
                return true;
            }
        }
        return false;
    }

    public boolean isReferenceAllowed(ConfigOrigin from, ConfigOrigin to) {
        if (isLocal(from) && !isLocal(to)) {
            return false;
        }
        return true;
    }

    public boolean hasConfigRepo(String configRepoId) {
        return this.getConfigRepo(configRepoId) != null;
    }

    private boolean isLocal(ConfigOrigin from) {
        // we assume that configuration is local (from file or from UI) when origin is not specified
        if (from == null) {
            return true;
        }
        return from.isLocal();
    }
}
