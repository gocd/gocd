/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.configrepo.InvalidPartialConfigException;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfigRepoPlugin implements PartialConfigProvider {
    private ConfigConverter configConverter;
    private ConfigRepoExtension crExtension;
    private String pluginId;

    public ConfigRepoPlugin(ConfigConverter configConverter, ConfigRepoExtension crExtension, String pluginId) {
        this.configConverter = configConverter;
        this.crExtension = crExtension;
        this.pluginId = pluginId;
    }

    @Override
    public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
        Collection<CRConfigurationProperty> cRconfigurations = getCrConfigurations(context.configuration());
        CRParseResult crPartialConfig = parseDirectory(configRepoCheckoutDirectory, cRconfigurations);
        return configConverter.toPartialConfig(crPartialConfig, context);
    }

    @Override
    public String displayName() {
        return "Plugin " + this.pluginId;
    }

    public CRParseResult parseDirectory(File configRepoCheckoutDirectory, Collection<CRConfigurationProperty> cRconfigurations) {
        CRParseResult crParseResult = this.crExtension.parseDirectory(this.pluginId, configRepoCheckoutDirectory.getAbsolutePath(), cRconfigurations);
        if (crParseResult.hasErrors())
            throw new InvalidPartialConfigException(crParseResult, crParseResult.getErrors().getErrorsAsText());
        return crParseResult;
    }

    public static List<CRConfigurationProperty> getCrConfigurations(Configuration configuration) {
        List<CRConfigurationProperty> config = new ArrayList<>();
        for (ConfigurationProperty prop : configuration) {
            String configKeyName = prop.getConfigKeyName();
            if (!prop.isSecure())
                config.add(new CRConfigurationProperty(configKeyName, prop.getValue(), null));
            else {
                CRConfigurationProperty crProp = new CRConfigurationProperty(configKeyName, null, prop.getEncryptedValue());
                config.add(crProp);
            }
        }
        return config;
    }
}
