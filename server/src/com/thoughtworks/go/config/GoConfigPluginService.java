/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides config plugins implementations
 */
@Component
public class GoConfigPluginService {

    private final XmlPartialConfigProvider embeddedXmlPlugin;

    @Autowired public GoConfigPluginService(ConfigCache configCache,ConfigElementImplementationRegistry configElementImplementationRegistry,
                                 MetricsProbeService metricsProbeService)
    {
        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(configCache, configElementImplementationRegistry, metricsProbeService);
        embeddedXmlPlugin = new XmlPartialConfigProvider(loader);
    }

    public PartialConfigProvider partialConfigProviderFor(ConfigRepoConfig repoConfig)
    {
        return embeddedXmlPlugin;
    }
}
