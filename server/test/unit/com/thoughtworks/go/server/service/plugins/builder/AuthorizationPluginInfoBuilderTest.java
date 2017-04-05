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

import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.AuthorizationPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import org.junit.After;
import org.junit.Test;

import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class AuthorizationPluginInfoBuilderTest {
    @After
    public void tearDown() throws Exception {
        AuthorizationMetadataStore.instance().clear();
    }

    @Test
    public void pluginInfoFor_ShouldProvidePluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings authSettings =
                new com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings(Arrays.asList(new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("password", new Metadata(true, true))),
                        new com.thoughtworks.go.plugin.domain.common.PluginView("auth_config_view"));
        com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings roleSettings =
                new com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings(Arrays.asList(new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("memberOf", new Metadata(true, false))),
                        new com.thoughtworks.go.plugin.domain.common.PluginView("role_config_view"));

        com.thoughtworks.go.plugin.domain.common.Image image = new com.thoughtworks.go.plugin.domain.common.Image("image/png", Base64.getEncoder().encodeToString("some-base64-encoded-data".getBytes(UTF_8)));
        AuthorizationMetadataStore authorizationMetadataStore = AuthorizationMetadataStore.instance();
        authorizationMetadataStore.setPluginInfo(new com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo(plugin, authSettings, roleSettings, image, null));

        AuthorizationPluginInfoBuilder builder = new AuthorizationPluginInfoBuilder(authorizationMetadataStore);
        AuthorizationPluginInfo pluginInfo = builder.pluginInfoFor(plugin.id());

        Map<String, Object> passwordMetadata = new HashMap<>();
        passwordMetadata.put("required", true);
        passwordMetadata.put("secure", true);

        Map<String, Object> memberOfMetadata = new HashMap<>();
        memberOfMetadata.put("required", true);
        memberOfMetadata.put("secure", false);

        PluggableInstanceSettings authConfigSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("password", passwordMetadata)), new PluginView("auth_config_view"));
        PluggableInstanceSettings roleConfigSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("memberOf", memberOfMetadata)), new PluginView("role_config_view"));


        assertEquals(new AuthorizationPluginInfo(plugin, authConfigSettings, roleConfigSettings,
                new com.thoughtworks.go.plugin.access.common.models.Image(image.getContentType(), image.getData())), pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        AuthorizationPluginInfoBuilder builder = new AuthorizationPluginInfoBuilder(AuthorizationMetadataStore.instance());
        AuthorizationPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings authSettings =
                new com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings(Arrays.asList(new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("password", new Metadata(true, true))),
                        new com.thoughtworks.go.plugin.domain.common.PluginView("auth_config_view"));
        com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings roleSettings =
                new com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings(Arrays.asList(new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("memberOf", new Metadata(true, false))),
                        new com.thoughtworks.go.plugin.domain.common.PluginView("role_config_view"));

        com.thoughtworks.go.plugin.domain.common.Image image = new com.thoughtworks.go.plugin.domain.common.Image("image/png", Base64.getEncoder().encodeToString("some-base64-encoded-data".getBytes(UTF_8)));
        AuthorizationMetadataStore authorizationMetadataStore = AuthorizationMetadataStore.instance();
        authorizationMetadataStore.setPluginInfo(new com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo(plugin, authSettings, roleSettings, image, null));

        AuthorizationPluginInfoBuilder builder = new AuthorizationPluginInfoBuilder(authorizationMetadataStore);
        Collection<AuthorizationPluginInfo> pluginInfos = builder.allPluginInfos();

        Map<String, Object> passwordMetadata = new HashMap<>();
        passwordMetadata.put("required", true);
        passwordMetadata.put("secure", true);

        Map<String, Object> memberOfMetadata = new HashMap<>();
        memberOfMetadata.put("required", true);
        memberOfMetadata.put("secure", false);

        PluggableInstanceSettings authConfigSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("password", passwordMetadata)), new PluginView("auth_config_view"));
        PluggableInstanceSettings roleConfigSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("memberOf", memberOfMetadata)), new PluginView("role_config_view"));


        assertEquals(Arrays.asList(new AuthorizationPluginInfo(plugin, authConfigSettings, roleConfigSettings,
                new com.thoughtworks.go.plugin.access.common.models.Image(image.getContentType(), image.getData()))), pluginInfos);
    }
}
