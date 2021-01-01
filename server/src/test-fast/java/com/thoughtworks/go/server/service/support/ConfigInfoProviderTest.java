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
package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigInfoProviderTest {
    private AuthorizationMetadataStore authorizationMetadataStore;
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Before
    public void setUp() throws Exception {
        authorizationMetadataStore = AuthorizationMetadataStore.instance();
    }

    @Test
    public void shouldProvideSecurityInformationWhenNoAuthorizationPluginIsConfigured() throws Exception {
        final GoConfigService goConfigService = goConfigService();
        final AuthorizationPluginInfo passwordFile = pluginInfo("cd.go.authentication.passwordfile", "Password File Authentication Plugin for GoCD");
        final AuthorizationPluginInfo ldap = pluginInfo("cd.go.authentication.ldap", "LDAP Authentication Plugin for GoCD");

        authorizationMetadataStore.setPluginInfo(passwordFile);
        authorizationMetadataStore.setPluginInfo(ldap);

        final Map<String, Object> map = new ConfigInfoProvider(goConfigService, authorizationMetadataStore, agentService()).asJson();

        final Map<String, Object> security = (Map<String, Object>) map.get("Security");
        assertNotNull(security);
        assertThat(security, hasEntry("Enabled", false));
        assertThat(security, hasEntry("Plugins", new ArrayList<>()));
    }

    @Test
    public void shouldProvideSecurityInformationWhenAuthorizationPluginsConfigured() throws Exception {
        final GoConfigService goConfigService = goConfigService();
        final AuthorizationPluginInfo passwordFile = pluginInfo("cd.go.authentication.passwordfile", "Password File Authentication Plugin for GoCD");
        final AuthorizationPluginInfo ldap = pluginInfo("cd.go.authentication.ldap", "LDAP Authentication Plugin for GoCD");

        authorizationMetadataStore.setPluginInfo(passwordFile);
        authorizationMetadataStore.setPluginInfo(ldap);
        goConfigService.security().securityAuthConfigs().add(new SecurityAuthConfig("file", "cd.go.authentication.passwordfile"));

        final Map<String, Object> map = new ConfigInfoProvider(goConfigService, authorizationMetadataStore, agentService()).asJson();

        final Map<String, Object> security = (Map<String, Object>) map.get("Security");
        assertNotNull(security);
        assertThat(security, hasEntry("Enabled", true));

        final List<Map<String, Boolean>> plugins = (List<Map<String, Boolean>>) security.get("Plugins");
        assertThat(plugins, containsInAnyOrder(
                singletonMap("Password File Authentication Plugin for GoCD", true),
                singletonMap("LDAP Authentication Plugin for GoCD", false)
        ));
    }

    private AuthorizationPluginInfo pluginInfo(String pluginId, String pluginName) {
        final AuthorizationPluginInfo pluginInfo = mock(AuthorizationPluginInfo.class);
        final PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        final PluginDescriptor.About about = mock(PluginDescriptor.About.class);

        when(pluginDescriptor.id()).thenReturn(pluginId);
        when(pluginDescriptor.about()).thenReturn(about);
        when(about.name()).thenReturn(pluginName);
        when(pluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        Capabilities capabilities = mock(Capabilities.class);
        when(capabilities.getSupportedAuthType()).thenReturn(SupportedAuthType.Password);
        when(pluginInfo.getCapabilities()).thenReturn(capabilities);
        return pluginInfo;
    }

    private GoConfigService goConfigService() {
        final GoConfigService goConfigService = mock(GoConfigService.class);
        final CruiseConfig cruiseConfig = mock(CruiseConfig.class);

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(emptyList());
        when(cruiseConfig.getEnvironments()).thenReturn(new EnvironmentsConfig());
        when(cruiseConfig.getAllUniqueMaterials()).thenReturn(emptySet());
        when(goConfigService.getSchedulableMaterials()).thenReturn(emptySet());
        when(goConfigService.security()).thenReturn(new SecurityConfig());
        return goConfigService;
    }

    private AgentService agentService() {
        AgentService agentService = mock(AgentService.class);
        AgentInstances agentInstances = mock(AgentInstances.class);
        when(agentService.getAgentInstances()).thenReturn(agentInstances);
        when(agentInstances.size()).thenReturn(0);
        return agentService;
    }
}
