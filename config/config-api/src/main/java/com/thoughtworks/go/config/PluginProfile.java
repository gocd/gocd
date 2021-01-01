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

import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class PluginProfile extends Configuration implements Validatable {
    public static final String ID = "id";
    public static final String PLUGIN_ID = "pluginId";
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "id", optional = false)
    protected String id;
    @ConfigAttribute(value = "pluginId", allowNull = false)
    protected String pluginId;

    public PluginProfile(String id, String pluginId, ConfigurationProperty... props) {
        super(props);
        this.pluginId = pluginId;
        this.id = id;
    }

    public PluginProfile() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors().add(fieldName, message);
    }

    @Override
    public boolean hasErrors() {
        return super.hasErrors() || !errors().isEmpty();
    }

    public void validateTree(ValidationContext validationContext) {
        validate(validationContext);
        super.validateTree();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateUniqueness(getObjectDescription() + " " + (isBlank(id) ? "(noname)" : "'" + id + "'"));

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

    public void addConfigurations(List<ConfigurationProperty> configurations) {
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        for (ConfigurationProperty property : configurations) {
            add(builder.create(property.getConfigKeyName(),
                    property.getConfigValue(),
                    property.getEncryptedValue(),
                    isSecure(property.getConfigKeyName())));
        }
    }

    @PostConstruct
    public void encryptSecureConfigurations() {
        if (hasPluginInfo()) {
            for (ConfigurationProperty configuration : this) {
                configuration.handleSecureValueConfiguration(isSecure(configuration.getConfigKeyName()));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PluginProfile that = (PluginProfile) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return pluginId != null ? pluginId.equals(that.pluginId) : that.pluginId == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getObjectDescription() + "{" +
                "id='" + id + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", properties='" + super.toString() + '\'' +
                '}';
    }

    protected abstract String getObjectDescription();

    protected abstract boolean isSecure(String key);

    protected abstract boolean hasPluginInfo();

    void validateIdUniquness(Map<String, PluginProfile> profiles) {
        PluginProfile profileWithSameId = profiles.get(id);
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
