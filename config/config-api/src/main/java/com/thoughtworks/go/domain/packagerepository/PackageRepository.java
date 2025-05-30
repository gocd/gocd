/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.packagerepository;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.ConfigurationDisplayUtil;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.config.SecureKeyInfoProvider;
import com.thoughtworks.go.plugin.access.packagematerial.AbstractPackageMetaDataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@ConfigTag("repository")
public class PackageRepository implements Serializable, Validatable, SecretParamAware {

    @ConfigAttribute(value = "id", allowNull = true)
    private String id;

    @ConfigAttribute(value = "name", allowNull = false)
    private String name;

    @Expose
    @SerializedName("plugin")
    @ConfigSubtag
    private PluginConfiguration pluginConfiguration = new PluginConfiguration();

    @Expose
    @SerializedName("config")
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    @ConfigSubtag
    private transient Packages packages = new Packages();

    private ConfigErrors errors = new ConfigErrors();

    public static final String NAME = "name";
    public static final String REPO_ID = "repoId";
    public static final String PLUGIN_CONFIGURATION = "pluginConfiguration";
    public static final String CONFIGURATION = "repoConfiguration";

    public PackageRepository() {
    }

    @TestOnly
    public PackageRepository(String repoId, String name, PluginConfiguration pluginConfiguration, Configuration configurationProperties) {
        this.id = repoId;
        this.name = name;
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configurationProperties;
    }

    public String getId() {
        return id;
    }

    //used in erb as it cannot access id attribute as it treats 'id' as keyword
    public String getRepoId() {
        return getId();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(PluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void addConfigurations(List<ConfigurationProperty> configurations) {
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        for (ConfigurationProperty property : configurations) {
            RepositoryConfiguration repositoryMetadata = RepositoryMetadataStore.getInstance().getRepositoryMetadata(pluginConfiguration.getId());
            if (isValidPluginConfiguration(property.getConfigKeyName(), repositoryMetadata)) {
                configuration.add(builder.create(property.getConfigKeyName(), property.getConfigValue(), property.getEncryptedValue(),
                        repositoryPropertyFor(property.getConfigKeyName(), repositoryMetadata).getOption(Property.SECURE)));
            } else {
                configuration.add(property);
            }
        }
    }

    private boolean isValidPluginConfiguration(String configKeyName, RepositoryConfiguration repositoryMetadata) {
        return doesPluginExist() && repositoryPropertyFor(configKeyName, repositoryMetadata) != null;
    }

    private Property repositoryPropertyFor(String configKey, RepositoryConfiguration repositoryMetadata) {
        return repositoryMetadata.get(configKey);
    }

    public Packages getPackages() {
        return packages;
    }

    public void setPackages(Packages packages) {
        this.packages = packages;
    }

    public void addPackage(PackageDefinition pkg) {
        packages.add(pkg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PackageRepository that = (PackageRepository) o;

        if (!Objects.equals(configuration, that.configuration)) {
            return false;
        }
        if (!Objects.equals(id, that.id)) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(packages, that.packages)) {
            return false;
        }
        return Objects.equals(pluginConfiguration, that.pluginConfiguration);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (pluginConfiguration != null ? pluginConfiguration.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        result = 31 * result + (packages != null ? packages.hashCode() : 0);
        return result;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (isBlank(name)) {
            errors().add(NAME, "Please provide name");
        } else if (new NameTypeValidator().isNameInvalid(name)) {
            errors().add(NAME, NameTypeValidator.errorMessage("PackageRepository", name));
        }
        configuration.validateUniqueness(String.format("Repository '%s'", name));
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @PostConstruct
    public void setRepositoryReferenceOnPackages() {
        for (PackageDefinition packageDefinition : packages) {
            packageDefinition.setRepository(this);
        }
    }

    public String getConfigForDisplay() {
        String pluginId = pluginConfiguration.getId();
        AbstractPackageMetaDataStore metadataStore = RepositoryMetadataStore.getInstance();
        List<ConfigurationProperty> propertiesToBeUsedForDisplay = ConfigurationDisplayUtil.getConfigurationPropertiesToBeUsedForDisplay(metadataStore, pluginId, configuration);

        String prefix = metadataStore.hasPlugin(pluginId) ? "" : "WARNING! Plugin missing for ";
        return prefix + "Repository: " + configuration.forDisplay(propertiesToBeUsedForDisplay);
    }

    public boolean doesPluginExist() {
        return RepositoryMetadataStore.getInstance().hasPlugin(pluginConfiguration.getId());
    }

    @PostConstruct
    public void applyPackagePluginMetadata() {
        String pluginId = pluginConfiguration.getId();
        for (ConfigurationProperty configurationProperty : configuration) {
            RepositoryMetadataStore repositoryMetadataStore = RepositoryMetadataStore.getInstance();
            if (repositoryMetadataStore.getMetadata(pluginId) != null) {
                boolean isSecureProperty = repositoryMetadataStore.hasOption(pluginId, configurationProperty.getConfigurationKey().getName(), PackageConfiguration.SECURE);
                configurationProperty.handleSecureValueConfiguration(isSecureProperty);
            }
        }

        for (PackageDefinition packageDefinition : packages) {
            packageDefinition.applyPackagePluginMetadata(pluginId);
        }
    }

    public void setConfigAttributes(Map<String, ?> attributes) {
        if (attributes.containsKey(NAME)) {
            name = ((String) attributes.get(NAME));
        }
        if (attributes.containsKey(REPO_ID)) {
            id = ((String) attributes.get(REPO_ID));
        }
        if (attributes.containsKey(PLUGIN_CONFIGURATION)) {
            pluginConfiguration.setConfigAttributes(attributes.get(PLUGIN_CONFIGURATION));
        }
        if (attributes.containsKey(Configuration.CONFIGURATION)) {
            configuration.clear();
            configuration.setConfigAttributes(attributes.get(Configuration.CONFIGURATION), getSecureKeyInfoProvider());
        }
    }

    private SecureKeyInfoProvider getSecureKeyInfoProvider() {
        final RepositoryMetadataStore repositoryMetadataStore = RepositoryMetadataStore.getInstance();
        final PackageConfigurations metadata = repositoryMetadataStore.getMetadata(pluginConfiguration.getId());
        if (metadata == null) {
            return null;
        }
        return key -> {
            PackageConfiguration packageConfiguration = metadata.get(key);
            return packageConfiguration.getOption(PackageConfiguration.SECURE);
        };
    }

    public void addConfigurationErrorFor(String key, String message) {
        configuration.addErrorFor(key, message);
    }

    public boolean isNew() {
        return isEmpty(id);
    }

    public PackageDefinition findPackage(String packageId) {
        return packages.find(packageId);
    }

    public void clearEmptyConfigurations() {
        configuration.clearEmptyConfigurations();
    }

    public void removePackage(String packageId) {
        PackageDefinition entryToBeDeleted = null;
        for (PackageDefinition packageDefinition : packages) {
            if (packageDefinition.getId().equals(packageId)) {
                entryToBeDeleted = packageDefinition;
            }
        }
        if (entryToBeDeleted == null) {
            throw new RuntimeException(format("Could not find package with id:[%s]", packageId));
        }
        packages.remove(entryToBeDeleted);
    }

    public PackageDefinition findOrCreatePackageDefinition(Map<String, Object> attributes) {
        if (attributes.get("packageId") != null) {
            return findPackage((String) attributes.get("packageId"));
        }
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setRepository(this);
        packageDefinition.setConfigAttributes(attributes.get("package_definition"));
        return packageDefinition;
    }

    public void validateNameUniqueness(Map<String, PackageRepository> nameMap) {
        if (name == null) {
            return;
        }
        String errorMessageForDuplicateName = String.format("You have defined multiple repositories called '%s'. Repository names are case-insensitive and must be unique.", name);
        PackageRepository repoWithSameFieldValue = nameMap.get(name.toLowerCase());
        if (repoWithSameFieldValue == null) {
            nameMap.put(name.toLowerCase(), this);
        } else {
            repoWithSameFieldValue.addError(NAME, errorMessageForDuplicateName);
            addError(NAME, errorMessageForDuplicateName);
        }
    }

    @PostConstruct
    public void ensureIdExists() {
        if (isBlank(getId())) {
            setId(UUID.randomUUID().toString());
        }
    }

    @Override
    public boolean hasSecretParams() {
        return this.getConfiguration().hasSecretParams();
    }

    @Override
    public SecretParams getSecretParams() {
        return this.getConfiguration().getSecretParams();
    }
}
