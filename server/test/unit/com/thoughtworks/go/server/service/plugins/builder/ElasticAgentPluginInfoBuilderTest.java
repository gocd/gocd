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

import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.ElasticPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class ElasticAgentPluginInfoBuilderTest {
    @After
    public void tearDown() throws Exception {
        ElasticAgentMetadataStore.instance().clear();
    }

    @Test
    public void pluginInfoFor_ShouldProvideEPluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings profileSettings = new com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings(Arrays.asList(new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("password", Collections.singletonMap("secure", true))),
                new com.thoughtworks.go.plugin.domain.common.PluginView("profile_view"));
        com.thoughtworks.go.plugin.domain.common.Image image =
                new com.thoughtworks.go.plugin.domain.common.Image("image/png", Base64.getEncoder().encodeToString("some-base64-encoded-data".getBytes(UTF_8)));

        ElasticAgentMetadataStore metadataStore = ElasticAgentMetadataStore.instance();
        metadataStore.setPluginInfo(new ElasticAgentPluginInfo(plugin, profileSettings, image));

        ElasticPluginInfo pluginInfo = new ElasticAgentPluginInfoBuilder(metadataStore).pluginInfoFor(plugin.id());

        PluggableInstanceSettings elasticProfileSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("password", Collections.singletonMap("secure", true))), new PluginView("profile_view"));

        assertEquals(new ElasticPluginInfo(plugin, elasticProfileSettings, new com.thoughtworks.go.plugin.access.common.models.Image(image.getContentType(), image.getData())), pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(ElasticAgentMetadataStore.instance());
        ElasticPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings profileSettings = new com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings(Arrays.asList(new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("password", Collections.singletonMap("secure", true))),
                new com.thoughtworks.go.plugin.domain.common.PluginView("profile_view"));
        com.thoughtworks.go.plugin.domain.common.Image image =
                new com.thoughtworks.go.plugin.domain.common.Image("image/png", Base64.getEncoder().encodeToString("some-base64-encoded-data".getBytes(UTF_8)));

        ElasticAgentMetadataStore metadataStore = ElasticAgentMetadataStore.instance();
        metadataStore.setPluginInfo(new ElasticAgentPluginInfo(plugin, profileSettings, image));

        Collection<ElasticPluginInfo> pluginInfos = new ElasticAgentPluginInfoBuilder(metadataStore).allPluginInfos();

        PluggableInstanceSettings elasticProfileSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("password", Collections.singletonMap("secure", true))), new PluginView("profile_view"));

        assertEquals(Arrays.asList(new ElasticPluginInfo(plugin, elasticProfileSettings, new com.thoughtworks.go.plugin.access.common.models.Image(image.getContentType(), image.getData()))), pluginInfos);
    }
}