/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.elastic.Constants;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticAgentViewViewModelBuilderTest {
    @Mock
    private ElasticAgentPluginRegistry registry;

    @Mock
    private PluginManager manager;

    private ElasticAgentViewViewModelBuilder builder;
    private GoPluginDescriptor dockerPlugin;
    private GoPluginDescriptor awsPlugin;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new ElasticAgentViewViewModelBuilder(manager, registry);

        dockerPlugin = new GoPluginDescriptor("cd.go.elastic-agent.docker", "1.0",
                new GoPluginDescriptor.About("GoCD Docker Elastic Agent Plugin", "1.0", null, null, null, null),
                null, null, false);


        awsPlugin = new GoPluginDescriptor("cd.go.elastic-agent.aws", "1.0",
                new GoPluginDescriptor.About("GoCD AWS Elastic Agent Plugin", "1.0", null, null, null, null),
                null, null, false);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() throws Exception {
        when(registry.getPlugins()).thenReturn(new ArrayList<PluginDescriptor>(Arrays.asList(dockerPlugin, awsPlugin)));
        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));

        PluginInfo dockerPluginInfo = pluginInfos.get(0);
        PluginInfo awsPluginInfo = pluginInfos.get(1);

        assertEquals(new PluginInfo(dockerPlugin, Constants.EXTENSION_NAME, null, null), dockerPluginInfo);
        assertEquals(new PluginInfo(awsPlugin, Constants.EXTENSION_NAME, null, null), awsPluginInfo);
    }
}
