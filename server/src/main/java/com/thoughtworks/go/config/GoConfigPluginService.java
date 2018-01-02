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

import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.security.GoCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides config plugins implementations
 */
@Component
public class GoConfigPluginService {

    private final ConfigRepoExtension crExtension;
    private final XmlPartialConfigProvider embeddedXmlPlugin;
    private ConfigConverter configConverter;

    @Autowired public GoConfigPluginService(ConfigRepoExtension configRepoExtension,
            ConfigCache configCache,ConfigElementImplementationRegistry configElementImplementationRegistry,
            CachedGoConfig cachedGoConfig)
    {
        this.crExtension = configRepoExtension;
        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(configCache, configElementImplementationRegistry);
        embeddedXmlPlugin = new XmlPartialConfigProvider(loader);
        configConverter = new ConfigConverter(new GoCipher(),cachedGoConfig);
    }

    public PartialConfigProvider partialConfigProviderFor(ConfigRepoConfig repoConfig)
    {
        String pluginId = repoConfig.getConfigProviderPluginName();
        return partialConfigProviderFor(pluginId);
    }

    public PartialConfigProvider partialConfigProviderFor(String pluginId) {
        if(pluginId == null || pluginId.equals(XmlPartialConfigProvider.providerName))
            return embeddedXmlPlugin;

        return new ConfigRepoPlugin(configConverter,crExtension,pluginId);
    }
}
