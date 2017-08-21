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
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticAgentViewModelBuilderTest {
    private ElasticAgentViewModelBuilder builder;
    private GoPluginDescriptor dockerPlugin;
    private GoPluginDescriptor awsPlugin;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new ElasticAgentViewModelBuilder(ElasticAgentMetadataStore.instance());

        dockerPlugin = new GoPluginDescriptor("cd.go.elastic-agent.docker", "1.0",
                new GoPluginDescriptor.About("GoCD Docker Elastic Agent Plugin", "1.0", null, null, null, null),
                null, null, false);


        awsPlugin = new GoPluginDescriptor("cd.go.elastic-agent.aws", "1.0",
                new GoPluginDescriptor.About("GoCD AWS Elastic Agent Plugin", "1.0", null, null, null, null),
                null, null, false);
    }

    @After
    public void tearDown() throws Exception {
        ElasticAgentMetadataStore.instance().clear();
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() throws Exception {
        ElasticAgentMetadataStore metadataStore = ElasticAgentMetadataStore.instance();
        metadataStore.setPluginInfo(new ElasticAgentPluginInfo(dockerPlugin, null, null, null, null));
        metadataStore.setPluginInfo(new ElasticAgentPluginInfo(awsPlugin, null, null, null, null));

        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));

        PluginInfo dockerPluginInfo = pluginInfos.get(0);
        PluginInfo awsPluginInfo = pluginInfos.get(1);

        assertEquals(new PluginInfo(dockerPlugin, ElasticAgentPluginConstants.EXTENSION_NAME, null, null), dockerPluginInfo);
        assertEquals(new PluginInfo(awsPlugin, ElasticAgentPluginConstants.EXTENSION_NAME, null, null), awsPluginInfo);
    }

    @Test
    public void shouldBeAbleToFetchPluginInfoForSinglePlugin() throws Exception {
        ElasticAgentMetadataStore metadataStore = ElasticAgentMetadataStore.instance();
        com.thoughtworks.go.plugin.domain.common.Image image = new com.thoughtworks.go.plugin.domain.common.Image("image/png", Base64.getEncoder().encodeToString("some-base64-encoded-data".getBytes(UTF_8)), "hash");;
        ElasticAgentPluginInfo elasticAgentPluginInfo = new ElasticAgentPluginInfo(dockerPlugin, new PluggableInstanceSettings(Arrays.asList(new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("foo", new Metadata(false, true))),
                        new com.thoughtworks.go.plugin.domain.common.PluginView("foo_template")), image, null, null);

        metadataStore.setPluginInfo(elasticAgentPluginInfo);

        PluginInfo pluginInfo = builder.pluginInfoFor(elasticAgentPluginInfo.getDescriptor().id());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("required", false);
        metadata.put("secure", true);
        PluginInfo info = new PluginInfo(dockerPlugin, "elastic-agent", null, new com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("foo", metadata)),
                new PluginView("foo_template")), new Image(image.getContentType(), image.getData()));

        assertEquals(info, pluginInfo);
    }
}
