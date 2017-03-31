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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.authorization.models.SupportedAuthType;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadata;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKey;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.AuthorizationPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationPluginInfoBuilderTest {

    private Capabilities capabilities;

    @Before
    public void setUp() throws Exception {
        capabilities = new Capabilities(SupportedAuthType.Password, true, true, true);
    }

    @Test
    public void pluginInfoFor_ShouldProvidePluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        PluginProfileMetadataKeys securityAuthConfigMetadata = new PluginProfileMetadataKeys(Arrays.asList(new PluginProfileMetadataKey("password", new PluginProfileMetadata(true, true))));
        PluginProfileMetadataKeys roleConfigMetadata = new PluginProfileMetadataKeys(Arrays.asList(new PluginProfileMetadataKey("memberOf", new PluginProfileMetadata(true, false))));

        Image image = new Image("image/png", Base64.getEncoder().encodeToString("some-base64-encoded-data".getBytes(UTF_8)));
        String securityAuthConfigView = "some html view";
        String roleConfigView = "another html view";

        AuthorizationPluginConfigMetadataStore store = mock(AuthorizationPluginConfigMetadataStore.class);

        when(store.find("docker-plugin")).thenReturn(plugin);
        when(store.getProfileMetadata(plugin)).thenReturn(securityAuthConfigMetadata);
        when(store.getRoleMetadata(plugin)).thenReturn(roleConfigMetadata);
        when(store.getIcon(plugin)).thenReturn(image);
        when(store.getProfileView(plugin)).thenReturn(securityAuthConfigView);
        when(store.getRoleView(plugin)).thenReturn(roleConfigView);
        when(store.getCapabilities(plugin)).thenReturn(capabilities);
        when(store.canAuthorize(plugin.id())).thenReturn(capabilities.canAuthorize());

        AuthorizationPluginInfoBuilder builder = new AuthorizationPluginInfoBuilder(store);
        AuthorizationPluginInfo pluginInfo = builder.pluginInfoFor(plugin.id());

        PluggableInstanceSettings authConfigSettings = new PluggableInstanceSettings(PluginConfiguration.getPluginConfigurations(securityAuthConfigMetadata), new PluginView(securityAuthConfigView));
        PluggableInstanceSettings roleConfigSettings = new PluggableInstanceSettings(PluginConfiguration.getPluginConfigurations(roleConfigMetadata), new PluginView(roleConfigView));

        assertEquals(new AuthorizationPluginInfo(plugin, authConfigSettings, roleConfigSettings, image, capabilities), pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        AuthorizationPluginConfigMetadataStore store = mock(AuthorizationPluginConfigMetadataStore.class);
        when(store.find("docker-plugin")).thenReturn(null);

        AuthorizationPluginInfoBuilder builder = new AuthorizationPluginInfoBuilder(store);
        AuthorizationPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        PluginProfileMetadataKeys securityAuthConfigMetadata = new PluginProfileMetadataKeys(Arrays.asList(new PluginProfileMetadataKey("password", new PluginProfileMetadata(true, true))));
        PluginProfileMetadataKeys roleConfigMetadata = new PluginProfileMetadataKeys(Arrays.asList(new PluginProfileMetadataKey("memberOf", new PluginProfileMetadata(true, false))));

        Image image = new Image("image/png", Base64.getEncoder().encodeToString("some-base64-encoded-data".getBytes(UTF_8)));
        String securityAuthConfigView = "some html view";
        String roleConfigView = "another html view";

        AuthorizationPluginConfigMetadataStore store = mock(AuthorizationPluginConfigMetadataStore.class);

        when(store.getPlugins()).thenReturn(Arrays.asList(plugin));
        when(store.find("docker-plugin")).thenReturn(plugin);
        when(store.getProfileMetadata(plugin)).thenReturn(securityAuthConfigMetadata);
        when(store.getRoleMetadata(plugin)).thenReturn(roleConfigMetadata);
        when(store.getIcon(plugin)).thenReturn(image);
        when(store.getProfileView(plugin)).thenReturn(securityAuthConfigView);
        when(store.getRoleView(plugin)).thenReturn(roleConfigView);
        when(store.getCapabilities(plugin)).thenReturn(capabilities);
        when(store.canAuthorize(plugin.id())).thenReturn(capabilities.canAuthorize());

        AuthorizationPluginInfoBuilder builder = new AuthorizationPluginInfoBuilder(store);
        Collection<AuthorizationPluginInfo> pluginInfos = builder.allPluginInfos();

        PluggableInstanceSettings authConfigSettings = new PluggableInstanceSettings(PluginConfiguration.getPluginConfigurations(securityAuthConfigMetadata), new PluginView(securityAuthConfigView));
        PluggableInstanceSettings roleConfigSettings = new PluggableInstanceSettings(PluginConfiguration.getPluginConfigurations(roleConfigMetadata), new PluginView(roleConfigView));

        assertEquals(Arrays.asList(new AuthorizationPluginInfo(plugin, authConfigSettings, roleConfigSettings, image, capabilities)), pluginInfos);
    }
}
