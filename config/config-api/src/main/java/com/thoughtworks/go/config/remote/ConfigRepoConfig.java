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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Defines single source of remote configuration and name of plugin to interpet it.
 * This goes to standard static xml configuration.
 */
@ConfigTag("config-repo")
public class ConfigRepoConfig implements Validatable, Cacheable {
    // defines source of configuration. Any will fit
    @ConfigSubtag(optional = false)
    private MaterialConfig repo;

    @ConfigSubtag
    private Configuration configuration = new Configuration();

    // TODO something must instantiate this name into proper implementation of ConfigProvider
    // which can be a plugin or embedded class
    @ConfigAttribute(value = "pluginId", allowNull = false)
    private String pluginId;
    // plugin-name which will process the repository tree to return configuration.
    // as in https://github.com/gocd/gocd/issues/1133#issuecomment-109014208
    // then pattern-based plugin is just one option

    @ConfigAttribute(value = "id", allowNull = false)
    private String id = UUID.randomUUID().toString();

    public static final String ID = "id";

    private ConfigErrors errors = new ConfigErrors();

    public ConfigRepoConfig() {
    }

    public ConfigRepoConfig(MaterialConfig repo, String pluginId) {
        this.repo = repo;
        this.pluginId = pluginId;
    }

    public ConfigRepoConfig(MaterialConfig repo, String pluginId, String id) {
        this(repo, pluginId);
        this.id = id;
    }

    public MaterialConfig getMaterialConfig() {
        return repo;
    }

    public void setMaterialConfig(MaterialConfig config) {
        this.repo = config;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (isBlank(id))
            id = null;
        this.id = id;
    }

    public void setPluginId(String pluginId) {
        if (isBlank(pluginId))
            pluginId = null;
        this.pluginId = pluginId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigRepoConfig that = (ConfigRepoConfig) o;
        return Objects.equals(repo, that.repo) &&
                Objects.equals(configuration, that.configuration) &&
                Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repo, configuration, pluginId, id);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        this.validatePresenceOfId();
        this.validatePresenceOfPluginId();
        this.validateUniquenessOfId(validationContext);
        this.validateRepoIsSet();
        this.validateMaterial(validationContext);
        if (isValidMaterial()) {
            this.validateAutoUpdateEnabled();
            this.validateAutoUpdateState(validationContext);
            this.validateMaterialUniqueness(validationContext);
        }
    }

    private void validateMaterialUniqueness(ValidationContext validationContext) {
        if (this.getMaterialConfig() != null) {
            if (!validationContext.getCruiseConfig().getConfigRepos().isUniqueMaterial(this.getMaterialConfig().getFingerprint())) {
                this.errors.add("material", format(
                        "You have defined multiple configuration repositories with the same repository - '%s'.",
                        this.repo.getDisplayName()));
            }
        }
    }

    private void validateUniquenessOfId(ValidationContext validationContext) {
        if (!validationContext.getCruiseConfig().getConfigRepos().isUniqueId(id)) {
            this.errors.add(ID, format("You have defined multiple configuration repositories with the same id - '%s'.", id));
        }
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors.isEmpty();

        if (getMaterialConfig() != null) {
            isValid = getMaterialConfig().errors().isEmpty() && isValid;
        }

        return isValid;
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.errors.add(fieldName, message);
    }

    private void validateMaterial(ValidationContext validationContext) {
        MaterialConfig materialConfig = getMaterialConfig();

        if (materialConfig != null) {
            materialConfig.validateTree(validationContext);
        }
    }

    private boolean isValidMaterial() {
        MaterialConfig materialConfig = getMaterialConfig();
        if (materialConfig == null) {
            return false;
        }

        return materialConfig.errors().isEmpty();
    }

    private void validateAutoUpdateEnabled() {
        if (this.getMaterialConfig() != null) {
            if (!this.getMaterialConfig().isAutoUpdate())
                this.getMaterialConfig().errors().add("autoUpdate", format(
                        "Configuration repository material '%s' must have autoUpdate enabled.",
                        this.getMaterialConfig().getDisplayName()));
        }
    }

    private void validateRepoIsSet() {
        if (this.getMaterialConfig() == null) {
            this.errors.add("material", "Configuration repository material not specified.");
        }
    }

    private void validatePresenceOfId() {
        if (isBlank(this.getId())) {
            this.errors.add(ID, "Configuration repository id not specified");
        }
    }

    private void validatePresenceOfPluginId() {
        if (isBlank(this.getPluginId())) {
            this.errors.add("plugin_id", "Configuration repository plugin_id not specified");
        }
    }

    public boolean hasSameMaterial(MaterialConfig config) {
        return this.getMaterialConfig().getFingerprint().equals(config.getFingerprint());
    }

    public boolean hasMaterialWithFingerprint(String fingerprint) {
        return this.getMaterialConfig().getFingerprint().equals(fingerprint);
    }

    private void validateAutoUpdateState(ValidationContext validationContext) {
        if (validationContext == null)
            return;

        MaterialConfig material = this.getMaterialConfig();
        if (material != null) {
            MaterialConfigs allMaterialsByFingerPrint = validationContext.getAllMaterialsByFingerPrint(material.getFingerprint());
            if (allMaterialsByFingerPrint.stream().anyMatch(m -> !m.isAutoUpdate())) {
                getMaterialConfig().errors().add("autoUpdate", format("Material of type %s (%s) is specified as a configuration repository and pipeline material with disabled autoUpdate."
                        + " All copies of this material must have autoUpdate enabled or configuration repository must be removed", material.getTypeForDisplay(), material.getDescription()));
            }
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void addConfigurations(List<ConfigurationProperty> configuration) {
        this.configuration.addAll(configuration);
    }
}
