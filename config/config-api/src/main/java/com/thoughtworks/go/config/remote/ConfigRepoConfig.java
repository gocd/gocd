/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
    private String configProviderPluginName = "gocd-xml";
    // plugin-name which will process the repository tree to return configuration.
    // as in https://github.com/gocd/gocd/issues/1133#issuecomment-109014208
    // then pattern-based plugin is just one option

    @ConfigAttribute(value = "id", allowNull = false)
    private String id = UUID.randomUUID().toString();

    public static final String AUTO_UPDATE = "autoUpdate";
    public static final String UNIQUE_REPO = "unique_repo";
    public static final String REPO = "repo";
    public static final String ID = "id";

    private ConfigErrors errors = new ConfigErrors();

    public ConfigRepoConfig() {
    }

    public ConfigRepoConfig(MaterialConfig repo, String configProviderPluginName) {
        this.repo = repo;
        this.configProviderPluginName = configProviderPluginName;
    }

    public ConfigRepoConfig(MaterialConfig repo, String configProviderPluginName, String id) {
        this(repo, configProviderPluginName);
        this.id = id;
    }

    public MaterialConfig getMaterialConfig() {
        return repo;
    }

    public void setMaterialConfig(MaterialConfig config) {
        this.repo = config;
    }

    public String getConfigProviderPluginName() {
        return configProviderPluginName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (StringUtils.isBlank(id))
            id = null;
        this.id = id;
    }

    public void setConfigProviderPluginName(String configProviderPluginName) {
        if (StringUtils.isBlank(configProviderPluginName))
            configProviderPluginName = null;
        this.configProviderPluginName = configProviderPluginName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigRepoConfig that = (ConfigRepoConfig) o;
        return Objects.equals(repo, that.repo) &&
                Objects.equals(configuration, that.configuration) &&
                Objects.equals(configProviderPluginName, that.configProviderPluginName) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repo, configuration, configProviderPluginName, id);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        this.validatePresenceOfId();
        this.validateRepoIsSet();
        this.validateAutoUpdateEnabled();
        this.validateAutoUpdateState(validationContext);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.errors.add(fieldName, message);
    }

    public void validateMaterialUniqueness(Map<String, ConfigRepoConfig> map) {
        if (this.getMaterialConfig() == null) {
            return;
        }
        String materialFingerprint = this.getMaterialConfig().getFingerprint();
        ConfigRepoConfig repoWithSameFingerprint = map.get(materialFingerprint);
        if (repoWithSameFingerprint != null) {
            repoWithSameFingerprint.addMaterialConflictError();
            addMaterialConflictError();
            return;
        }
        map.put(materialFingerprint, this);
    }

    public void validateIdUniqueness(ArrayList<String> allIds) {
        if (StringUtils.isBlank(this.id)) {
            this.errors.add("id", String.format("Invalid config-repo id", id));
        }
        if (allIds.contains(this.id)) {
            this.errors.add("unique_id", String.format("You have defined multiple configuration repositories with the same id - %s", id));
        }
    }

    private void validateAutoUpdateEnabled() {
        if (!this.getMaterialConfig().isAutoUpdate())
            this.errors.add(AUTO_UPDATE, String.format(
                    "Configuration repository material %s must have autoUpdate enabled",
                    this.getMaterialConfig().getDisplayName()));
    }

    private void addMaterialConflictError() {
        this.errors.add(UNIQUE_REPO, String.format(
                "You have defined multiple configuration repositories with the same repository - %s",
                this.repo.getDisplayName()));
    }

    private void validateRepoIsSet() {
        if (this.getMaterialConfig() == null) {
            this.errors.add(REPO, "Configuration repository material not specified");
        }
    }

    private void validatePresenceOfId() {
        if (this.getId() == null) {
            this.errors.add(ID, "Configuration repository id not specified");
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

        MaterialConfigs allMaterialsByFingerPrint = validationContext.getAllMaterialsByFingerPrint(material.getFingerprint());
        if (allMaterialsByFingerPrint != null) {
            for (MaterialConfig other : allMaterialsByFingerPrint) {
                if (!other.isAutoUpdate())
                    ((ScmMaterialConfig) other).setAutoUpdateMismatchErrorWithConfigRepo();
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
