/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thoughtworks.go.domain.packagerepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigReferenceCollection;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.IgnoreTraversal;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.ConfigurationDisplayUtil;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.SecureKeyInfoProvider;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.plugin.access.packagematerial.AbstractMetaDataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;

import static com.thoughtworks.go.util.ListUtil.join;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

@ConfigTag("package")
@ConfigReferenceCollection(collectionName = "packages", idFieldName = "id")
public class PackageDefinition implements Serializable, Validatable, ParamsAttributeAware {
    public static final String NAME = "name";
    public static final String ID = "id";
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "id", allowNull = true)
    private String id;

    @ConfigAttribute(value = "name", allowNull = false)
    private String name;

    @ConfigAttribute(value = "autoUpdate", optional = true)
    private boolean autoUpdate = true;

    @Expose
    @SerializedName("config")
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    @Expose
    @SerializedName("repository")
    @IgnoreTraversal
    private PackageRepository packageRepository;

    public PackageDefinition() {
    }

    public PackageDefinition(String id, String name, Configuration configuration) {
        this.id = id;
        this.name = name;
        this.configuration = configuration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public PackageRepository getRepository() {
        return packageRepository;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PackageDefinition that = (PackageDefinition) o;

        if (configuration != null ? !configuration.equals(that.configuration) : that.configuration != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (isBlank(name)) {
            errors().add(NAME, "Package name is mandatory");
        } else if (new NameTypeValidator().isNameInvalid(name)) {
            errors().add(NAME, NameTypeValidator.errorMessage("Package", name));
        }
        configuration.validateUniqueness(String.format("Package '%s'", name));
    }

    public void validateFingerprintUniqueness(Map<String, Packages> packagesMap) {
        String fingerprint = getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER);
        Packages packageDefinitionsWithSameFingerprint = packagesMap.get(fingerprint);

        if (packageDefinitionsWithSameFingerprint.size() > 1) {
            List<String> packageNames = new ArrayList<>();
            for (PackageDefinition packageDefinition : packageDefinitionsWithSameFingerprint) {
                packageNames.add(format("[Repo Name: '%s', Package Name: '%s']", packageDefinition.getRepository().getName(), packageDefinition.getName()));
            }
            addError(ID, String.format("Cannot save package or repo, found duplicate packages. %s", join(packageNames)));
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public void setRepository(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public String getConfigForDisplay() {
        AbstractMetaDataStore metadataStore = PackageMetadataStore.getInstance();
        List<ConfigurationProperty> propertiesToBeUsedForDisplay = ConfigurationDisplayUtil.getConfigurationPropertiesToBeUsedForDisplay(metadataStore, pluginId(), configuration);

        return format("%s - Package: %s", getRepository().getConfigForDisplay(), configuration.forDisplay(propertiesToBeUsedForDisplay));
    }

    public String getFingerprint(String fingerprintDelimiter) {
        List<String> list = new ArrayList<>();
        list.add(format("%s=%s", "plugin-id", pluginId()));
        handlePackageDefinitionProperties(list);
        handlePackageRepositoryProperties(list);
        String fingerprint = join(list, fingerprintDelimiter);
        // CAREFUL! the hash algorithm has to be same as the one used in 47_create_new_materials.sql
        return CachedDigestUtils.sha256Hex(fingerprint);
    }

    private void handlePackageDefinitionProperties(List<String> list) {
        PackageConfigurations metadata = PackageMetadataStore.getInstance().getMetadata(pluginId());
        for (ConfigurationProperty configurationProperty : configuration) {
            handleProperty(list, metadata, configurationProperty);
        }
    }

    private String pluginId() {
        return packageRepository.getPluginConfiguration().getId();
    }

    private void handlePackageRepositoryProperties(List<String> list) {
        PackageConfigurations metadata = RepositoryMetadataStore.getInstance().getMetadata(pluginId());
        for (ConfigurationProperty configurationProperty : packageRepository.getConfiguration()) {
            handleProperty(list, metadata, configurationProperty);
        }
    }

    private void handleProperty(List<String> list, PackageConfigurations metadata, ConfigurationProperty configurationProperty) {
        PackageConfiguration packageConfiguration = null;

        if (metadata != null) {
            packageConfiguration = metadata.get(configurationProperty.getConfigurationKey().getName());
        }

        if (packageConfiguration == null || packageConfiguration.getOption(PackageConfiguration.PART_OF_IDENTITY)) {
            list.add(configurationProperty.forFingerprint());
        }
    }

    public void applyPackagePluginMetadata(String pluginId) {
        for (ConfigurationProperty configurationProperty : configuration) {
            PackageMetadataStore packageMetadataStore = PackageMetadataStore.getInstance();
            if (packageMetadataStore.getMetadata(pluginId) != null) {
                boolean isSecureProperty = packageMetadataStore.hasOption(pluginId, configurationProperty.getConfigurationKey().getName(), PackageConfiguration.SECURE);
                configurationProperty.handleSecureValueConfiguration(isSecureProperty);
            }
        }
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        Map map = (Map) attributes;
        name = (String) map.get("name");
        if (map.containsKey(Configuration.CONFIGURATION) && packageRepository != null) {
            configuration.setConfigAttributes(map.get(Configuration.CONFIGURATION), getSecureKeyInfoProvider());
        }
    }


    private SecureKeyInfoProvider getSecureKeyInfoProvider() {
        PackageMetadataStore packageMetadataStore = PackageMetadataStore.getInstance();
        final PackageConfigurations metadata = packageMetadataStore.getMetadata(pluginId());
        if(metadata==null){
            return null;
        }
        return new SecureKeyInfoProvider() {
            @Override
            public boolean isSecure(String key) {
                PackageConfiguration packageConfiguration = metadata.get(key);
                return packageConfiguration.getOption(PackageConfiguration.SECURE);
            }
        };
    }


    public void addConfigurationErrorFor(String key, String message) {
        configuration.addErrorFor(key, message);
    }

    @Override
    public String toString() {
        return "PackageDefinition{" +
                "configuration=" + configuration +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public void clearEmptyConfigurations() {
        configuration.clearEmptyConfigurations();
    }

    public void validateNameUniqueness(HashMap<String, PackageDefinition> nameMap) {
        String errorMessageForDuplicateName = String.format("You have defined multiple packages called '%s'. Package names are case-insensitive and must be unique within a repository.", name);
        PackageDefinition repoWithSameFieldValue = nameMap.get(name.toLowerCase());
        if (repoWithSameFieldValue == null) {
            nameMap.put(name.toLowerCase(), this);
        } else {
            addError(NAME, errorMessageForDuplicateName);
        }
    }

    @PostConstruct
    public void ensureIdExists() {
        if (StringUtil.isBlank(getId())) {
            setId(UUID.randomUUID().toString());
        }
    }
}
