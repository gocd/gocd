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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
public abstract class RuleAwarePluginProfile implements Validatable, RulesAware {
    protected static final String ID = "id";
    protected static final String PLUGIN_ID = "pluginId";

    @ConfigAttribute(value = "id")
    protected String id;

    @ConfigAttribute(value = "pluginId")
    protected String pluginId;

    @ConfigSubtag
    protected Configuration configuration;

    @ConfigSubtag
    protected Rules rules = new Rules();

    @ConfigSubtag
    protected Description description = new Description();

    public RuleAwarePluginProfile() {
        configuration = new Configuration();
    }

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    protected final ConfigErrors errors = new ConfigErrors();

    public RuleAwarePluginProfile(String id, String pluginId, Rules rules, ConfigurationProperty... configurationProperties) {
        this.id = id;
        this.pluginId = pluginId;
        this.configuration = new Configuration(configurationProperties);

        if (rules != null) {
            this.rules = rules;
        }
    }

    public void addConfigurations(List<ConfigurationProperty> configurations) {
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        for (ConfigurationProperty property : configurations) {
            final boolean isSecure = property.isSecure() || isSecure(property.getConfigKeyName());

            configuration.add(builder.create(property.getConfigKeyName(),
                    property.getConfigValue(),
                    property.getEncryptedValue(),
                    isSecure));
        }
    }

    public String getDescription() {
        return this.description.text;
    }

    public void setDescription(String description) {
        this.description.text = description;
    }

    @ConfigTag("description")
    @EqualsAndHashCode
    public static class Description {
        @ConfigValue
        private String text;
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
