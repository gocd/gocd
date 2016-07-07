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

package com.thoughtworks.go.domain.scm;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.ConfigurationDisplayUtil;
import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.StringUtil;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;

import static com.thoughtworks.go.util.ListUtil.join;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;

@ConfigTag("scm")
@ConfigReferenceCollection(collectionName = "scms", idFieldName = "id")
public class SCM implements Serializable, Validatable {
    public static final String SCM_ID = "scmId";
    public static final String NAME = "name";
    public static final String AUTO_UPDATE = "autoUpdate";
    public static final String PLUGIN_CONFIGURATION = "pluginConfiguration";
    public static final String VALUE_KEY = "value";
    public static final String ERRORS_KEY = "errors";

    private ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "id", allowNull = true)
    private String id;

    @ConfigAttribute(value = "name", allowNull = false)
    private String name;

    @ConfigAttribute(value = "autoUpdate", optional = true)
    private boolean autoUpdate = true;

    @Expose
    @SerializedName("plugin")
    @ConfigSubtag
    private PluginConfiguration pluginConfiguration = new PluginConfiguration();

    @Expose
    @SerializedName("config")
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    public SCM() {
    }

    public SCM(String id, PluginConfiguration pluginConfiguration, Configuration configuration) {
        this.id = id;
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configuration;
    }


    public String getId() {
        return id;
    }

    //used in erb as it cannot access id attribute as it treats 'id' as keyword
    public String getSCMId() {
        return getId();
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

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(PluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
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
            SCMConfigurations scmConfigurations = SCMMetadataStore.getInstance().getConfigurationMetadata(getPluginId());
            if (isValidPluginConfiguration(property.getConfigKeyName(), scmConfigurations)) {
                configuration.add(builder.create(property.getConfigKeyName(), property.getConfigValue(), property.getEncryptedValue(),
                                                 scmConfigurationFor(property.getConfigKeyName(), scmConfigurations).getOption(SCMConfiguration.SECURE)));
            }
            else {
                configuration.add(property);
            }
        }
    }

    private boolean isValidPluginConfiguration(String configKey, SCMConfigurations scmConfigurations) {
        return doesPluginExist() && scmConfigurationFor(configKey, scmConfigurations) != null;
    }

    private SCMConfiguration scmConfigurationFor(String configKey, SCMConfigurations scmConfigurations) {
        return scmConfigurations.get(configKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SCM that = (SCM) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (pluginConfiguration != null ? !pluginConfiguration.equals(that.pluginConfiguration) : that.pluginConfiguration != null) {
            return false;
        }
        if (configuration != null ? !configuration.equals(that.configuration) : that.configuration != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (pluginConfiguration != null ? pluginConfiguration.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (isBlank(name)) {
            errors().add(NAME, "Please provide name");
        } else if (new NameTypeValidator().isNameInvalid(name)) {
            errors().add(NAME, NameTypeValidator.errorMessage("SCM", name));
        }
        configuration.validateTree();
        configuration.validateUniqueness(String.format("SCM '%s'", name));
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public Map<String, Map<String, String>> getConfigAsMap() {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        for (ConfigurationProperty property : configuration) {
            Map<String, String> mapValue = new HashMap<>();
            mapValue.put(VALUE_KEY, property.getValue());
            if (!property.errors().isEmpty()) {
                mapValue.put(ERRORS_KEY, ListUtil.join(property.errors().getAll()));
            }
            configMap.put(property.getConfigKeyName(), mapValue);
        }
        return configMap;
    }

    public String getConfigForDisplay() {
        String pluginId = getPluginId();
        SCMMetadataStore metadataStore = SCMMetadataStore.getInstance();
        List<ConfigurationProperty> propertiesToBeUsedForDisplay = ConfigurationDisplayUtil.getConfigurationPropertiesToBeUsedForDisplay(metadataStore, pluginId, configuration);

        String prefix = metadataStore.hasPlugin(pluginId) ? "" : "WARNING! Plugin missing. ";
        return prefix + configuration.forDisplay(propertiesToBeUsedForDisplay);
    }

    private String getPluginId() {
        return pluginConfiguration.getId();
    }

    public Boolean doesPluginExist(){
        return SCMMetadataStore.getInstance().hasPlugin(getPluginId());
    }

    @PostConstruct
    public void applyPluginMetadata() {
        String pluginId = getPluginId();
        for (ConfigurationProperty configurationProperty : configuration) {
            SCMMetadataStore scmMetadataStore = SCMMetadataStore.getInstance();
            if (scmMetadataStore.getConfigurationMetadata(pluginId) != null) {
                boolean isSecureProperty = scmMetadataStore.hasOption(pluginId, configurationProperty.getConfigurationKey().getName(), SCMConfiguration.SECURE);
                configurationProperty.handleSecureValueConfiguration(isSecureProperty);
            }
        }
    }

    public void setConfigAttributes(Object attributes) {
        Map attributesMap = (Map) attributes;
        if (attributesMap.containsKey(SCM_ID)) {
            id = ((String) attributesMap.get(SCM_ID));
        }
        if (attributesMap.containsKey(NAME)) {
            name = ((String) attributesMap.get(NAME));
        }
        this.setAutoUpdate("true".equals(attributesMap.get(AUTO_UPDATE)));
        if (attributesMap.containsKey(PLUGIN_CONFIGURATION)) {
            pluginConfiguration.setConfigAttributes(attributesMap.get(PLUGIN_CONFIGURATION));
        }
        setPluginConfigurationAttributes(attributesMap);
    }

    protected void setPluginConfigurationAttributes(Map attributes) {
        SCMConfigurations scmConfigurations = SCMMetadataStore.getInstance().getConfigurationMetadata(pluginConfiguration.getId());
        if (scmConfigurations == null) {
            throw new RuntimeException("metadata unavailable for plugin: " + pluginConfiguration.getId());
        }
        for (SCMConfiguration scmConfiguration : scmConfigurations.list()) {
            String key = scmConfiguration.getKey();
            if (attributes.containsKey(key)) {
                if (configuration.getProperty(key) == null) {
                    configuration.addNewConfiguration(scmConfiguration.getKey(), scmConfiguration.getOption(Property.SECURE));
                }
                configuration.getProperty(key).setConfigurationValue(new ConfigurationValue((String) attributes.get(key)));
                configuration.getProperty(key).handleSecureValueConfiguration(scmConfiguration.getOption(Property.SECURE));
            }
        }
    }

    public String getFingerprint() {
        List<String> list = new ArrayList<>();
        list.add(format("%s=%s", "plugin-id", getPluginId()));
        handleSCMProperties(list);
        String fingerprint = join(list, AbstractMaterialConfig.FINGERPRINT_DELIMITER);
        // CAREFUL! the hash algorithm has to be same as the one used in 47_create_new_materials.sql
        return CachedDigestUtils.sha256Hex(fingerprint);
    }

    private void handleSCMProperties(List<String> list) {
        SCMConfigurations metadata = SCMMetadataStore.getInstance().getConfigurationMetadata(getPluginId());
        for (ConfigurationProperty configurationProperty : configuration) {
            handleProperty(list, metadata, configurationProperty);
        }
    }

    private void handleProperty(List<String> list, SCMConfigurations metadata, ConfigurationProperty configurationProperty) {
        SCMConfiguration scmConfiguration = null;

        if (metadata != null) {
            scmConfiguration = metadata.get(configurationProperty.getConfigurationKey().getName());
        }

        if (scmConfiguration == null || scmConfiguration.getOption(SCMConfiguration.PART_OF_IDENTITY)) {
            list.add(configurationProperty.forFingerprint());
        }
    }

    public boolean isNew() {
        return isEmpty(id);
    }

    public void clearEmptyConfigurations() {
        configuration.clearEmptyConfigurations();
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    @PostConstruct
    public void ensureIdExists() {
        if (StringUtil.isBlank(getId())) {
            setId(UUID.randomUUID().toString());
        }
    }

    public String getSCMType() {
        return "pluggable_material_" + getPluginConfiguration().getId().replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
