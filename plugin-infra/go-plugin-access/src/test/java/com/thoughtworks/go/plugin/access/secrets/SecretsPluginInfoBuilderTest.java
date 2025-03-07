/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.secrets;

import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecretsPluginInfoBuilderTest {
    private SecretsExtension extension;

    @BeforeEach
    public void setUp() {
        extension = mock(SecretsExtension.class);
    }

    @Test
    public void shouldBuildPluginInfoWithImage() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        Image icon = new Image("content_type", "data", "hash");

        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        SecretsPluginInfo pluginInfo = new SecretsPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getImage()).isEqualTo(icon);
    }

    @Test
    public void shouldBuildPluginInfoWithPluginDescriptor() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        SecretsPluginInfo pluginInfo = new SecretsPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor()).isEqualTo(descriptor);
    }

    @Test
    public void shouldBuildPluginInfoWithSecuritySettings() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        List<PluginConfiguration> pluginConfigurations = List.of(
                new PluginConfiguration("username", new Metadata(true, false)),
                new PluginConfiguration("password", new Metadata(true, true))
        );

        when(extension.getSecretsConfigMetadata(descriptor.id())).thenReturn(pluginConfigurations);
        when(extension.getSecretsConfigView(descriptor.id())).thenReturn("secrets_config_view");

        SecretsPluginInfo pluginInfo = new SecretsPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getSecretsConfigSettings()).isEqualTo(new PluggableInstanceSettings(pluginConfigurations, new PluginView("secrets_config_view")));
    }
}
