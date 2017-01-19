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

import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadata;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKey;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants;
import com.thoughtworks.go.plugin.access.elastic.ElasticPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

import static com.thoughtworks.go.server.service.plugins.builder.ViewModelBuilder.REQUIRED_OPTION;
import static com.thoughtworks.go.server.service.plugins.builder.ViewModelBuilder.SECURE_OPTION;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticAgentViewViewModelBuilderTest {
    @Mock
    private ElasticPluginConfigMetadataStore elasticPluginConfigMetadataStore;

    private ElasticAgentViewViewModelBuilder builder;
    private GoPluginDescriptor dockerPlugin;
    private GoPluginDescriptor awsPlugin;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new ElasticAgentViewViewModelBuilder(elasticPluginConfigMetadataStore);

        dockerPlugin = new GoPluginDescriptor("cd.go.elastic-agent.docker", "1.0",
                new GoPluginDescriptor.About("GoCD Docker Elastic Agent Plugin", "1.0", null, null, null, null),
                null, null, false);


        awsPlugin = new GoPluginDescriptor("cd.go.elastic-agent.aws", "1.0",
                new GoPluginDescriptor.About("GoCD AWS Elastic Agent Plugin", "1.0", null, null, null, null),
                null, null, false);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() throws Exception {
        when(elasticPluginConfigMetadataStore.getPlugins()).thenReturn(new ArrayList<>(Arrays.asList(dockerPlugin, awsPlugin)));
        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));

        PluginInfo dockerPluginInfo = pluginInfos.get(0);
        PluginInfo awsPluginInfo = pluginInfos.get(1);

        assertEquals(new PluginInfo(dockerPlugin, ElasticAgentPluginConstants.EXTENSION_NAME, null, null), dockerPluginInfo);
        assertEquals(new PluginInfo(awsPlugin, ElasticAgentPluginConstants.EXTENSION_NAME, null, null), awsPluginInfo);
    }

    @Test
    public void shouldBeAbleToFetchPluginInfoForSinglePlugin() throws Exception {
        when(elasticPluginConfigMetadataStore.find(dockerPlugin.id())).thenReturn(dockerPlugin);
        Image image = new Image("foo", "bar");
        when(elasticPluginConfigMetadataStore.getIcon(dockerPlugin)).thenReturn(image);
        when(elasticPluginConfigMetadataStore.getProfileView(dockerPlugin)).thenReturn("html");

        PluginProfileMetadataKey pluginProfileMetadataKey = new PluginProfileMetadataKey("foo", new PluginProfileMetadata(true, true));
        PluginProfileMetadataKeys pluginProfileMetadataKeys = new PluginProfileMetadataKeys(Collections.singletonList(pluginProfileMetadataKey));
        when(elasticPluginConfigMetadataStore.getProfileMetadata(dockerPlugin)).thenReturn(pluginProfileMetadataKeys);

        PluginInfo pluginInfo = builder.pluginInfoFor(dockerPlugin.id());

        assertThat(pluginInfo.getImage(), is(image));
        assertThat(pluginInfo.getPluggableInstanceSettings().getView(), is(new PluginView("html")));
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(REQUIRED_OPTION, true);
        metadata.put(SECURE_OPTION, true);
        assertEquals(pluginInfo.getPluggableInstanceSettings().getConfigurations(), Arrays.asList(new PluginConfiguration("foo", metadata, null)));
    }
}
