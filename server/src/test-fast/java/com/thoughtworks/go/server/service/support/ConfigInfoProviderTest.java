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

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class ConfigInfoProviderTest {
    private AuthorizationMetadataStore authorizationMetadataStore;
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @BeforeEach
    void setUp() {
        authorizationMetadataStore = AuthorizationMetadataStore.instance();
    }

    @Test
    void shouldAddMaterialCountForMaterialDefinedUsingSecretParams() {
        final GoConfigService goConfigService = goConfigService();
        final CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        final Set<MaterialConfig> uniqueMaterials = Stream.of(
                new GitMaterialConfig("https://{{SECRET:[id][token]}}@gocd.org/foo"),
                new GitMaterialConfig("https://gocd.org/bar"),
                new SvnMaterialConfig("svn://gocd.org/bar", "bob", "{{SECRET:[id][password]}}", false))
                .collect(toSet());

        when(currentConfig.getAllUniqueMaterials()).thenReturn(uniqueMaterials);

        final Map<String, Object> map = new ConfigInfoProvider(goConfigService, authorizationMetadataStore).asJson();

        final Map<String, Object> validConfig = (Map<String, Object>) map.get("Valid Config");
        assertThat(validConfig)
                .containsEntry("Number of unique materials", 3)
                .containsEntry("Number of unique materials with secret params", 2);
    }

    @Test
    void shouldProvideSecurityInformationWhenNoAuthorizationPluginIsConfigured() {
        final GoConfigService goConfigService = goConfigService();
        final AuthorizationPluginInfo passwordFile = pluginInfo("cd.go.authentication.passwordfile", "Password File Authentication Plugin for GoCD");
        final AuthorizationPluginInfo ldap = pluginInfo("cd.go.authentication.ldap", "LDAP Authentication Plugin for GoCD");

        authorizationMetadataStore.setPluginInfo(passwordFile);
        authorizationMetadataStore.setPluginInfo(ldap);

        final Map<String, Object> map = new ConfigInfoProvider(goConfigService, authorizationMetadataStore).asJson();

        final Map<String, Object> security = (Map<String, Object>) map.get("Security");
        assertThat(security).isNotNull();
        assertThat(security).containsEntry("Enabled", false);
        assertThat(security).containsEntry("Plugins", new ArrayList<>());
    }

    @Test
    void shouldProvideSecurityInformationWhenAuthorizationPluginsConfigured() {
        final GoConfigService goConfigService = goConfigService();
        final AuthorizationPluginInfo passwordFile = pluginInfo("cd.go.authentication.passwordfile", "Password File Authentication Plugin for GoCD");
        final AuthorizationPluginInfo ldap = pluginInfo("cd.go.authentication.ldap", "LDAP Authentication Plugin for GoCD");

        authorizationMetadataStore.setPluginInfo(passwordFile);
        authorizationMetadataStore.setPluginInfo(ldap);
        goConfigService.security().securityAuthConfigs().add(new SecurityAuthConfig("file", "cd.go.authentication.passwordfile"));

        final Map<String, Object> map = new ConfigInfoProvider(goConfigService, authorizationMetadataStore).asJson();

        final Map<String, Object> security = (Map<String, Object>) map.get("Security");
        assertThat(security).isNotNull();
        assertThat(security).containsEntry("Enabled", true);

        final List<Map<String, Boolean>> plugins = (List<Map<String, Boolean>>) security.get("Plugins");
        assertThat(plugins).contains(singletonMap("Password File Authentication Plugin for GoCD", true), singletonMap("LDAP Authentication Plugin for GoCD", false));
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
        when(goConfigService.agents()).thenReturn(new Agents());
        when(cruiseConfig.getEnvironments()).thenReturn(new EnvironmentsConfig());
        when(cruiseConfig.getAllUniqueMaterials()).thenReturn(emptySet());
        when(goConfigService.getSchedulableMaterials()).thenReturn(emptySet());
        when(goConfigService.security()).thenReturn(new SecurityConfig());
        return goConfigService;
    }
}