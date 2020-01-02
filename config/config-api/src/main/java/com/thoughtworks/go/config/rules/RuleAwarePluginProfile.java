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
package com.thoughtworks.go.config.rules;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class RuleAwarePluginProfile implements Validatable, RulesAware {
    public static final String ID = "id";
    public static final String PLUGIN_ID = "pluginId";

    @ConfigAttribute(value = "id")
    private String id;

    @ConfigAttribute(value = "pluginId")
    private String pluginId;

    @ConfigSubtag
    private Configuration configuration;

    @ConfigSubtag
    private Rules rules = new Rules();

    @ConfigSubtag
    private Description description = new Description();


    public RuleAwarePluginProfile() {
        configuration = new Configuration();
    }

    private final ConfigErrors errors = new ConfigErrors();

    public RuleAwarePluginProfile(String id, String pluginId, Rules rules, ConfigurationProperty... configurationProperties) {
        this.id = id;
        this.pluginId = pluginId;
        this.configuration = new Configuration(configurationProperties);

        if (rules != null) {
            this.rules = rules;
        }
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

    public String getDescription() {
        return this.description.text;
    }

    public void setDescription(String description) {
        this.description.text = description;
    }

    //TODO: return clone instead?
    @Override
    public Rules getRules() {
        return this.rules;
    }

    @ConfigTag("description")
    public static class Description {
        @ConfigValue
        private String text;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Description that = (Description) o;

            return text != null ? text.equals(that.text) : that.text == null;
        }

        @Override
        public int hashCode() {
            return text != null ? text.hashCode() : 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleAwarePluginProfile that = (RuleAwarePluginProfile) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (pluginId != null ? !pluginId.equals(that.pluginId) : that.pluginId != null) return false;
        if (configuration != null ? !configuration.equals(that.configuration) : that.configuration != null)
            return false;
        if (rules != null ? !rules.equals(that.rules) : that.rules != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id.hashCode(),
                pluginId.hashCode(),
                configuration.hashCode(),
                (rules != null ? rules.hashCode() : 0),
                (description != null ? description.hashCode() : 0)
        );
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
        return (!this.errors().isEmpty()) || rules.hasErrors() || configuration.hasErrors();
    }

    public void validateTree(ValidationContext validationContext) {
        validate(validationContext);
        configuration.validateTree();
        rules.validateTree(new DelegatingValidationContext(validationContext) {
            @Override
            public RulesValidationContext getRulesValidationContext() {
                return new RulesValidationContext(allowedActions(), allowedTypes());
            }
        });
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

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    protected abstract String getObjectDescription();

    protected abstract boolean isSecure(String key);

    protected abstract boolean hasPluginInfo();

    void validateIdUniqueness(Map<String, RuleAwarePluginProfile> profiles) {
        RuleAwarePluginProfile profileWithSameId = profiles.get(id);
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