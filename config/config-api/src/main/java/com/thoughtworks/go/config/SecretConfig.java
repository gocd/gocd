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

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.secrets.SecretsMetadataStore;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

@ConfigTag("secretConfig")
@ConfigCollection(value = ConfigurationProperty.class)
public class SecretConfig extends PluginProfile {
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    @ConfigSubtag
    private Rules rules = new Rules();

    @ConfigSubtag
    private Description description;

    private List<String> allowedActions = asList("refer");
    private List<String> allowedTypes = asList("pipeline_group");

    public SecretConfig() {
    }

    public SecretConfig(String id, String pluginId, ConfigurationProperty... configurationProperties) {
        this(id, pluginId, null, configurationProperties);
    }

    public SecretConfig(String id, String pluginId, Rules rules, ConfigurationProperty... configurationProperties) {
        super(id, pluginId, configurationProperties);
        if (rules != null) {
            this.rules = rules;
        }
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public String getDescription() {
        return this.description.text;
    }

    public Rules getRules() {
        return this.rules;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        super.validate(validationContext);
        rules.validate(new RulesValidationContext(validationContext, this.allowedActions, this.allowedTypes));
    }

    @Override
    protected String getObjectDescription() {
        return "Secret configuration";
    }

    @Override
    protected boolean isSecure(String key) {
        SecretsPluginInfo pluginInfo = this.metadataStore().getPluginInfo(getPluginId());

        if (pluginInfo == null
                || pluginInfo.getSecretsConfigSettings() == null
                || pluginInfo.getSecretsConfigSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getSecretsConfigSettings().getConfiguration(key).isSecure();
    }

    @Override
    protected boolean hasPluginInfo() {
        return !isNull(this.metadataStore().getPluginInfo(getPluginId()));
    }

    private SecretsMetadataStore metadataStore() {
        return SecretsMetadataStore.instance();
    }

    @ConfigTag("description")
    public static class Description {
        @ConfigValue
        private String text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecretConfig)) return false;
        if (!super.equals(o)) return false;
        SecretConfig that = (SecretConfig) o;
        return Objects.equals(configuration, that.configuration) &&
                Objects.equals(rules, that.rules) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), configuration, rules, description);
    }

    @Override
    public boolean hasErrors() {
        return super.hasErrors() || rules.hasErrors() || configuration.hasErrors();
    }
}