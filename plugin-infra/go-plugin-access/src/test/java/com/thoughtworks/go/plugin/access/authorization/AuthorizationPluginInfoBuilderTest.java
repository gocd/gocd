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
package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AuthorizationPluginInfoBuilderTest {

    private AuthorizationExtension extension;

    @BeforeEach
    public void setUp() {
        extension = mock(AuthorizationExtension.class);
        when(extension.getCapabilities(any(String.class))).thenReturn(new Capabilities(SupportedAuthType.Password, true, true, false));
    }

    @Test
    public void shouldBuildPluginInfoWithAuthSettings() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        List<PluginConfiguration> pluginConfigurations = Arrays.asList(
                new PluginConfiguration("username", new Metadata(true, false)),
                new PluginConfiguration("password", new Metadata(true, true))
        );

        when(extension.getAuthConfigMetadata(descriptor.id())).thenReturn(pluginConfigurations);
        when(extension.getAuthConfigView(descriptor.id())).thenReturn("auth_config");

        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getAuthConfigSettings(), is(new PluggableInstanceSettings(pluginConfigurations, new PluginView("auth_config"))));
    }

    @Test
    public void shouldBuildPluginInfoWithRoleSettings() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        List<PluginConfiguration> pluginConfigurations = Arrays.asList(
                new PluginConfiguration("group", new Metadata(true, false)),
                new PluginConfiguration("something_secure", new Metadata(true, true))
        );

        when(extension.getRoleConfigurationMetadata(descriptor.id())).thenReturn(pluginConfigurations);
        when(extension.getRoleConfigurationView(descriptor.id())).thenReturn("role_config");

        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getRoleSettings(), is(new PluggableInstanceSettings(pluginConfigurations, new PluginView("role_config"))));
    }

    @Test
    public void shouldNotHaveRoleSettingsInPluginInfoIfPluginCannotAuthorize() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        Capabilities capabilities = new Capabilities(SupportedAuthType.Password, true, false, false);

        when(extension.getCapabilities(descriptor.id())).thenReturn(capabilities);

        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertNull(pluginInfo.getRoleSettings());
    }

    @Test
    public void shouldBuildPluginInfoWithPluginDescriptor() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
    }

    @Test
    public void shouldBuildPluginInfoWithImage() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        Image icon = new Image("content_type", "data", "hash");

        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getImage(), is(icon));
    }

    @Test
    public void shouldBuildPluginInfoWithCapablities() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        Capabilities capabilities = new Capabilities(SupportedAuthType.Password, true, true, false);

        when(extension.getCapabilities(descriptor.id())).thenReturn(capabilities);

        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getCapabilities(), is(capabilities));
    }
}
