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

    public SecretConfig() {
    }

    public SecretConfig(String id, String pluginId, ConfigurationProperty... configurationProperties) {
        super(id, pluginId, configurationProperties);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getDescription() {
        return description.text;
    }

    public Rules getRules() {
        return rules;
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
}