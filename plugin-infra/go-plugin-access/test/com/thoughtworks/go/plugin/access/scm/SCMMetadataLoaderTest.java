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

package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SCMMetadataLoaderTest {
    private SCMMetadataLoader metadataLoader;
    private GoPluginDescriptor pluginDescriptor;
    private SCMExtension scmExtension;
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        pluginDescriptor = new GoPluginDescriptor("plugin-id", "1.0", null, null, null, true);
        pluginManager = mock(PluginManager.class);
        scmExtension = mock(SCMExtension.class);
        metadataLoader = new SCMMetadataLoader(scmExtension, pluginManager);

        SCMMetadataStore.getInstance().removeMetadata(pluginDescriptor.id());
    }

    @Test
    public void shouldFetchSCMMetadataForPluginsWhichImplementSCMExtensionPoint() {
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("k1").with(SCMProperty.REQUIRED, true).with(SCMProperty.PART_OF_IDENTITY, false));

        when(scmExtension.getSCMConfiguration(pluginDescriptor.id())).thenReturn(scmPropertyConfiguration);
        when(scmExtension.getSCMView(pluginDescriptor.id())).thenReturn(createSCMView("display-value", "template"));

        metadataLoader.fetchSCMMetaData(pluginDescriptor);

        SCMConfigurations configurationMetadata = SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id());
        assertThat(configurationMetadata.size(), is(1));
        SCMConfiguration scmConfiguration = configurationMetadata.get("k1");
        assertThat(scmConfiguration.getKey(), is("k1"));
        assertThat(scmConfiguration.getOption(SCMProperty.REQUIRED), is(true));
        assertThat(scmConfiguration.getOption(SCMProperty.PART_OF_IDENTITY), is(false));
        SCMView viewMetadata = SCMMetadataStore.getInstance().getViewMetadata(pluginDescriptor.id());
        assertThat(viewMetadata.displayValue(), is("display-value"));
        assertThat(viewMetadata.template(), is("template"));
    }

    @Test
    public void shouldThrowExceptionWhenNullSCMConfigurationReturned() {
        when(scmExtension.getSCMConfiguration(pluginDescriptor.id())).thenReturn(null);
        try {
            metadataLoader.fetchSCMMetaData(pluginDescriptor);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Plugin[plugin-id] returned null SCM configuration"));
        }
        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id()), nullValue());
    }

    @Test
    public void shouldThrowExceptionWhenNullSCMViewReturned() {
        when(scmExtension.getSCMConfiguration(pluginDescriptor.id())).thenReturn(new SCMPropertyConfiguration());
        when(scmExtension.getSCMView(pluginDescriptor.id())).thenReturn(null);
        try {
            metadataLoader.fetchSCMMetaData(pluginDescriptor);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Plugin[plugin-id] returned null SCM view"));
        }
        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id()), nullValue());
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() throws Exception {
        metadataLoader = new SCMMetadataLoader(scmExtension, pluginManager);

        verify(pluginManager).addPluginChangeListener(metadataLoader, GoPlugin.class);
    }

    @Test
    public void shouldFetchMetadataOnPluginLoadedCallback() throws Exception {
        SCMMetadataLoader spy = spy(metadataLoader);
        doNothing().when(spy).fetchSCMMetaData(pluginDescriptor);
        when(scmExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);

        spy.pluginLoaded(pluginDescriptor);

        verify(spy).fetchSCMMetaData(pluginDescriptor);
    }

    @Test
    public void shouldNotTryToFetchMetadataOnPluginLoadedCallback() throws Exception {
        SCMMetadataLoader spy = spy(metadataLoader);
        when(scmExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(false);

        spy.pluginLoaded(pluginDescriptor);

        verify(spy, never()).fetchSCMMetaData(pluginDescriptor);
    }

    @Test
    public void shouldRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        SCMMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), new SCMConfigurations(), createSCMView(null, null));
        when(scmExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(true);

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id()), is(nullValue()));
        assertThat(SCMMetadataStore.getInstance().getViewMetadata(pluginDescriptor.id()), is(nullValue()));
    }

    @Test
    public void shouldNotTryRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMView scmView = createSCMView(null, null);
        SCMMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), scmConfigurations, scmView);
        when(scmExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(false);

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id()), is(scmConfigurations));
        assertThat(SCMMetadataStore.getInstance().getViewMetadata(pluginDescriptor.id()), is(scmView));
    }

    private SCMView createSCMView(final String displayValue, final String template) {
        return new SCMView() {
            @Override
            public String displayValue() {
                return displayValue;
            }

            @Override
            public String template() {
                return template;
            }
        };
    }
}
