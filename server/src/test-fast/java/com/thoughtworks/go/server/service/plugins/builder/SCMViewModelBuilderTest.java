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

import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMPreference;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;
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

public class SCMViewModelBuilderTest {
    @Mock
    PluginManager manager;

    private SCMViewModelBuilder builder;
    private GoPluginDescriptor githubPR;
    private GoPluginDescriptor stashPR;
    private SCMPreference preference;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new SCMViewModelBuilder(manager);

        githubPR = new GoPluginDescriptor("github.pr", "version1",
                new GoPluginDescriptor.About("Github PR", "1.0", null, null, null,null),
                null, null, false);

        stashPR = new GoPluginDescriptor("stash.pr", "version1",
                new GoPluginDescriptor.About("Stash PR", "2.0", null, null, null,null),
                null, null, false);

        SCMConfigurations configurations = new SCMConfigurations();
        configurations.add(new SCMConfiguration("key1"));
        configurations.add(new SCMConfiguration("key2"));

        SCMView view = new SCMView() {
            @Override
            public String displayValue() {
                return "SCM Display Value";
            }

            @Override
            public String template() {
                return "scm view template";
            }
        };

        preference = new SCMPreference(configurations, view);
        SCMMetadataStore.getInstance().setPreferenceFor("github.pr", preference);
        SCMMetadataStore.getInstance().setPreferenceFor("stash.pr", preference);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() {
        when(manager.getPluginDescriptorFor("github.pr")).thenReturn(githubPR);
        when(manager.getPluginDescriptorFor("stash.pr")).thenReturn(stashPR);

        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));
        PluginInfo pluginInfo = pluginInfos.get(0).getId() == "github.pr" ? pluginInfos.get(0) : pluginInfos.get(1);
        assertThat(pluginInfo.getId(), is("github.pr"));
        assertThat(pluginInfo.getType(), is("scm"));
        assertThat(pluginInfo.getName(), is(githubPR.about().name()));
        assertThat(pluginInfo.getDisplayName(), is(preference.getScmView().displayValue()));
        assertThat(pluginInfo.getVersion(), is(githubPR.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings());
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenId() {
        when(manager.getPluginDescriptorFor("github.pr")).thenReturn(githubPR);

        PluginInfo pluginInfo = builder.pluginInfoFor("github.pr");

        assertThat(pluginInfo.getId(), is("github.pr"));
        assertThat(pluginInfo.getType(), is("scm"));
        assertThat(pluginInfo.getName(), is(githubPR.about().name()));
        assertThat(pluginInfo.getDisplayName(), is(preference.getScmView().displayValue()));
        assertThat(pluginInfo.getVersion(), is(githubPR.about().version()));
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenIdWithViewTemplate() {
        when(manager.getPluginDescriptorFor("github.pr")).thenReturn(githubPR);

        PluginInfo pluginInfo = builder.pluginInfoFor("github.pr");

        PluginView view = pluginInfo.getPluggableInstanceSettings().getView();
        assertThat(view.getTemplate(), is("scm view template"));
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenIdWithConfigurations() {
        when(manager.getPluginDescriptorFor("github.pr")).thenReturn(githubPR);

        PluginInfo pluginInfo = builder.pluginInfoFor("github.pr");

        HashMap expectedMetadata = new HashMap<String, Object>() {{
            put("required",true);
            put("secure",false);
            put("part_of_identity",true);
        }};

        List<PluginConfiguration> configurations = pluginInfo.getPluggableInstanceSettings().getConfigurations();
        assertThat(configurations.size(), is(2));
        PluginConfiguration configuration1 = configurations.get(0);
        assertThat(configuration1.getKey(), is("key1"));
        assertNull(configuration1.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));

        PluginConfiguration configuration2 = configurations.get(1);
        assertThat(configuration2.getKey(), is("key2"));
        assertNull(configuration2.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));
    }

    @Test
    public void shouldBeNullIfPluginNotRegistered() {
        assertNull(builder.pluginInfoFor("unregistered_plugin"));
    }
}
