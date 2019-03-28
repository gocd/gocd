/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.secrets.SecretsMetadataStore;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@ConfigTag("secretConfig")
//@ConfigCollection(value = ConfigurationProperty.class)
public abstract class NewPluginProfile implements Validatable {
    public static final String ID = "id";
    public static final String PLUGIN_ID = "pluginId";

    @ConfigAttribute(value = "id")
    private String id;

    @ConfigAttribute(value = "pluginId")
    private String pluginId;

    @ConfigSubtag
    private Configuration configuration;


    public NewPluginProfile() {
        configuration = new Configuration();
    }
    private final ConfigErrors errors = new ConfigErrors();

    public NewPluginProfile(String id, String pluginId, ConfigurationProperty... configurationProperties) {
        this.id = id;
        this.pluginId = pluginId;
        configuration = new Configuration(configurationProperties);
    }

    public Configuration getConfiguration() {
        return configuration;
    }


    public void addConfigurations(List<ConfigurationProperty> configurations) {
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        for (ConfigurationProperty property : configurations) {
            configuration.add(builder.create(property.getConfigKeyName(),
                    property.getConfigValue(),
                    property.getEncryptedValue(),
                    isSecure(property.getConfigKeyName())));
        }
    }

    public String getId() {
        return id;
    }

    public String getPluginId() {
        return pluginId;
    }


    @ConfigTag("description")
    public static class Description {

        @ConfigValue
        private String text;

    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof NewPluginProfile)) return false;
//        if (!super.equals(o)) return false;
//        NewPluginProfile that = (NewPluginProfile) o;
//        return Objects.equals(configuration, that.configuration) &&
//                Objects.equals(rules, that.rules) &&
//                Objects.equals(description, that.description);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(super.hashCode(), configuration, rules, description);
//    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewPluginProfile that = (NewPluginProfile) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (pluginId != null ? !pluginId.equals(that.pluginId) : that.pluginId != null) return false;
        if (configuration != null ? !configuration.equals(that.configuration) : that.configuration != null)
            return false;
        return errors != null ? errors.equals(that.errors) : that.errors == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        result = 31 * result + (errors != null ? errors.hashCode() : 0);
        return result;
    }

    private SecretsMetadataStore metadataStore() {
        return SecretsMetadataStore.instance();
    }

    @PostConstruct
    public void encryptSecureConfigurations() {
        if (hasPluginInfo()) {
            for (ConfigurationProperty configuration : this.getConfiguration()) {
                configuration.handleSecureValueConfiguration(isSecure(configuration.getConfigKeyName()));
            }
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors().add(fieldName, message);
    }

    public boolean hasErrors() {
        return configuration.hasErrors() || !errors().isEmpty();
    }

    public void validateTree(ValidationContext validationContext) {
        validate(validationContext);
        configuration.validateTree();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        configuration.validateUniqueness(getObjectDescription() + " " + (isBlank(id) ? "(noname)" : "'" + id + "'"));

        if (isBlank(id)) {
            addError(ID, getObjectDescription() + " cannot have a blank id.");
        }

        if (isBlank(pluginId)) {
            addError(PLUGIN_ID, getObjectDescription() + " cannot have a blank plugin id.");
        }

        if (new NameTypeValidator().isNameInvalid(id)) {
            addError(ID, String.format("Invalid id '%s'. %s", id, NameTypeValidator.ERROR_MESSAGE));
        }
    }

    protected abstract String getObjectDescription();

    protected abstract boolean isSecure(String key);

    protected abstract boolean hasPluginInfo();


    void validateIdUniqueness(Map<String, NewPluginProfile> profiles) {
        NewPluginProfile profileWithSameId = profiles.get(id);
        if (profileWithSameId == null) {
            profiles.put(id, this);
        } else {
            profileWithSameId.addError(ID, String.format(getObjectDescription() + " id '%s' is not unique", id));
            this.addError(ID, String.format(getObjectDescription() + " id '%s' is not unique", id));
        }
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }
}