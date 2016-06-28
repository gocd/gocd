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

import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PackageViewModelBuilderTest {
    @Mock
    PluginManager manager;

    private PackageViewModelBuilder builder;
    private GoPluginDescriptor yumPoller;
    private GoPluginDescriptor npmPoller;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new PackageViewModelBuilder(manager);
        yumPoller = new GoPluginDescriptor("yum.poller", "version1",
                                           new GoPluginDescriptor.About("Yum Poller", "1.0", null, null, null,null),
                                           null, null, false);

        npmPoller = new GoPluginDescriptor("npm.poller", "version1",
                                           new GoPluginDescriptor.About("NPM Poller", "2.0", null, null, null,null),
                                           null, null, false);

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new PackageConfiguration("key1"));
        packageConfigurations.add(new PackageConfiguration("key2"));

        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        repositoryConfigurations.add(new PackageConfiguration("key1"));

        PackageMetadataStore.getInstance().addMetadataFor(yumPoller.id(), packageConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor(npmPoller.id(), new PackageConfigurations());

        RepositoryMetadataStore.getInstance().addMetadataFor(yumPoller.id(), repositoryConfigurations);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() {
        when(manager.getPluginDescriptorFor("yum.poller")).thenReturn(yumPoller);
        when(manager.getPluginDescriptorFor("npm.poller")).thenReturn(npmPoller);

        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));
        PluginInfo pluginInfo = pluginInfos.get(0).getId() == "yum.poller" ? pluginInfos.get(0) : pluginInfos.get(1);
        assertThat(pluginInfo.getId(), is("yum.poller"));
        assertThat(pluginInfo.getType(), is("package-repository"));
        assertThat(pluginInfo.getName(), is(yumPoller.about().name()));
        assertThat(pluginInfo.getVersion(), is(yumPoller.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings());
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenId() {
        when(manager.getPluginDescriptorFor("yum.poller")).thenReturn(yumPoller);

        PluginInfo pluginInfo = builder.pluginInfoFor("yum.poller");

        assertThat(pluginInfo.getId(), is("yum.poller"));
        assertThat(pluginInfo.getType(), is("package-repository"));
        assertThat(pluginInfo.getName(), is(yumPoller.about().name()));
        assertThat(pluginInfo.getVersion(), is(yumPoller.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings().getView());
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenIdWithConfigurations() {
        when(manager.getPluginDescriptorFor("yum.poller")).thenReturn(yumPoller);

        PluginInfo pluginInfo = builder.pluginInfoFor("yum.poller");

        HashMap expectedMetadata = new HashMap<String, Object>() {{
            put("required",true);
            put("secure",false);
            put("part_of_identity",true);
        }};

        List<PluginConfiguration> configurations = pluginInfo.getPluggableInstanceSettings().getConfigurations();
        assertThat(configurations.size(), is(3));

        PluginConfiguration configuration2 = configurations.get(0);
        assertThat(configuration2.getKey(), is("key1"));
        assertThat(configuration2.getType(), is("package"));
        assertThat(configuration2.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));

        PluginConfiguration configuration3 = configurations.get(1);
        assertThat(configuration3.getKey(), is("key2"));
        assertThat(configuration3.getType(), is("package"));
        assertThat(configuration3.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));

        PluginConfiguration configuration1 = configurations.get(2);
        assertThat(configuration1.getKey(), is("key1"));
        assertThat(configuration1.getType(), is("repository"));
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));
    }

    @Test
    public void shouldBeNullIfPluginNotRegistered() {
        assertNull(builder.pluginInfoFor("unregistered_plugin"));
    }
}
