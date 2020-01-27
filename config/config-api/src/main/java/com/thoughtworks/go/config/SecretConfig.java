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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.secrets.SecretsMetadataStore;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;

import java.util.List;

import static com.thoughtworks.go.config.rules.SupportedEntity.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;

@ConfigTag("secretConfig")
public class SecretConfig extends RuleAwarePluginProfile {
    private List<String> allowedActions = unmodifiableList(asList("refer"));
    private List<String> allowedTypes = unmodifiableListOf(PIPELINE_GROUP, ENVIRONMENT);

    public SecretConfig() {
        super();
    }

    public SecretConfig(String id, String pluginId, ConfigurationProperty... configurationProperties) {
        this(id, pluginId, null, configurationProperties);
    }

    public SecretConfig(String id, String pluginId, Rules rules, ConfigurationProperty... configurationProperties) {
        super(id, pluginId, rules, configurationProperties);
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

    @Override
    public List<String> allowedActions() {
        return allowedActions;
    }

    @Override
    public List<String> allowedTypes() {
        return allowedTypes;
    }

    private SecretsMetadataStore metadataStore() {
        return SecretsMetadataStore.instance();
    }
}
