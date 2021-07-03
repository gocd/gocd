/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SCMMetadataLoaderTest {
    private SCMMetadataLoader metadataLoader;
    private GoPluginDescriptor pluginDescriptor;
    private SCMExtension scmExtension;
    private PluginManager pluginManager;

    @BeforeEach
    public void setUp() throws Exception {
        pluginDescriptor = GoPluginDescriptor.builder().id("plugin-id").isBundledPlugin(true).build();
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
        assertThat(configurationMetadata.size()).isEqualTo(1);
        SCMConfiguration scmConfiguration = configurationMetadata.get("k1");
        assertThat(scmConfiguration.getKey()).isEqualTo("k1");
        assertThat(scmConfiguration.getOption(SCMProperty.REQUIRED)).isTrue();
        assertThat(scmConfiguration.getOption(SCMProperty.PART_OF_IDENTITY)).isFalse();
        SCMView viewMetadata = SCMMetadataStore.getInstance().getViewMetadata(pluginDescriptor.id());
        assertThat(viewMetadata.displayValue()).isEqualTo("display-value");
        assertThat(viewMetadata.template()).isEqualTo("template");
    }

    @Test
    public void shouldThrowExceptionWhenNullSCMConfigurationReturned() {
        when(scmExtension.getSCMConfiguration(pluginDescriptor.id())).thenReturn(null);
        try {
            metadataLoader.fetchSCMMetaData(pluginDescriptor);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Plugin[plugin-id] returned null SCM configuration");
        }
        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id())).isNull();
    }

    @Test
    public void shouldThrowExceptionWhenNullSCMViewReturned() {
        when(scmExtension.getSCMConfiguration(pluginDescriptor.id())).thenReturn(new SCMPropertyConfiguration());
        when(scmExtension.getSCMView(pluginDescriptor.id())).thenReturn(null);
        try {
            metadataLoader.fetchSCMMetaData(pluginDescriptor);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Plugin[plugin-id] returned null SCM view");
        }
        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id())).isNull();
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() throws Exception {
        metadataLoader = new SCMMetadataLoader(scmExtension, pluginManager);

        verify(pluginManager).addPluginChangeListener(metadataLoader);
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

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id())).isNull();
        assertThat(SCMMetadataStore.getInstance().getViewMetadata(pluginDescriptor.id())).isNull();
    }

    @Test
    public void shouldNotTryRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMView scmView = createSCMView(null, null);
        SCMMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), scmConfigurations, scmView);
        when(scmExtension.canHandlePlugin(pluginDescriptor.id())).thenReturn(false);

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata(pluginDescriptor.id())).isEqualTo(scmConfigurations);
        assertThat(SCMMetadataStore.getInstance().getViewMetadata(pluginDescriptor.id())).isEqualTo(scmView);
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
