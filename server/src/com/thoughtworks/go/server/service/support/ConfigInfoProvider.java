/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

@Component
public class ConfigInfoProvider implements ServerInfoProvider {
    private GoConfigService goConfigService;
    private final AuthorizationMetadataStore authorizationMetadataStore;

    @Autowired
    public ConfigInfoProvider(GoConfigService goConfigService) {
        this(goConfigService, AuthorizationMetadataStore.instance());
    }

    ConfigInfoProvider(GoConfigService goConfigService, AuthorizationMetadataStore authorizationMetadataStore) {
        this.goConfigService = goConfigService;
        this.authorizationMetadataStore = authorizationMetadataStore;
    }

    @Override
    public double priority() {
        return 2.0;
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();

        LinkedHashMap<String, Object> validConfig = new LinkedHashMap<>();
        validConfig.put("Number of pipelines", goConfigService.getAllPipelineConfigs().size());
        validConfig.put("Number of agents", goConfigService.agents().size());
        validConfig.put("Number of environments", currentConfig.getEnvironments().size());
        validConfig.put("Number of unique materials", currentConfig.getAllUniqueMaterials().size());
        validConfig.put("Number of schedulable materials", goConfigService.getSchedulableMaterials().size());

        json.put("Valid Config", validConfig);
        json.put("Security", securityInformation());

        return json;
    }

    private LinkedHashMap<String, Object> securityInformation() {
        final LinkedHashMap<String, Object> security = new LinkedHashMap<>();
        final ArrayList<Map<String, Object>> pluginsConfigured = new ArrayList<>();
        security.put("Plugins", pluginsConfigured);

        if (goConfigService.security().securityAuthConfigs().isEmpty()) {
            security.put("Enabled", false);
            return security;
        }

        security.put("Enabled", true);
        for (AuthorizationPluginInfo pluginInfo : authorizationMetadataStore.allPluginInfos()) {
            final String pluginName = pluginInfo.getDescriptor().about().name();
            final boolean hashAuthConfig = !goConfigService.security().securityAuthConfigs().findByPluginId(pluginInfo.getDescriptor().id()).isEmpty();
            pluginsConfigured.add(singletonMap(pluginName, hashAuthConfig));
        }

        return security;
    }

    @Override
    public String name() {
        return "Config Statistics";
    }
}
