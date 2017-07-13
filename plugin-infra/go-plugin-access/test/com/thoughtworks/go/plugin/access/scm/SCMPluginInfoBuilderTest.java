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

package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.when;

public class SCMPluginInfoBuilderTest {
    private SCMExtension extension;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        extension = mock(SCMExtension.class);

        SCMPropertyConfiguration value = new SCMPropertyConfiguration();
        value.add(new SCMProperty("username", null).with(Property.REQUIRED, true).with(Property.SECURE, false).with(Property.PART_OF_IDENTITY, true).with(Property.DISPLAY_ORDER, 1));
        value.add(new SCMProperty("password", null).with(Property.REQUIRED, true).with(Property.SECURE, true).with(Property.PART_OF_IDENTITY, false).with(Property.DISPLAY_ORDER, 2));
        stub(extension.getSCMConfiguration("plugin1")).toReturn(value);
        stub(extension.getSCMView("plugin1")).toReturn(new SCMView() {
            @Override
            public String displayValue() {
                return "some scm plugin";
            }

            @Override
            public String template() {
                return "some html";
            }
        });
        PluginSettingsConfiguration pluginSettingsConfiguration = new PluginSettingsConfiguration();
        pluginSettingsConfiguration.add(new PluginSettingsProperty("k1", null).with(Property.REQUIRED, true).with(Property.SECURE, false).with(Property.DISPLAY_ORDER, 3));
        stub(extension.getPluginSettingsConfiguration("plugin1")).toReturn(pluginSettingsConfiguration);
        stub(extension.getPluginSettingsView("plugin1")).toReturn("settings view");
    }

    @Test
    public void shouldBuildPluginInfo() throws Exception {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);

        SCMPluginInfo pluginInfo = new SCMPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        List<PluginConfiguration> scmConfigurations = Arrays.asList(
                new PluginConfiguration("username", new MetadataWithPartOfIdentity(true, false, true)),
                new PluginConfiguration("password", new MetadataWithPartOfIdentity(true, true, false))
        );
        PluginView pluginView = new PluginView("some html");
        List<PluginConfiguration> pluginSettings = Arrays.asList(new PluginConfiguration("k1", new Metadata(true, false)));

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("scm"));
        assertThat(pluginInfo.getDisplayName(), is("some scm plugin"));
        assertThat(pluginInfo.getScmSettings(), is(new PluggableInstanceSettings(scmConfigurations, pluginView)));
        assertThat(pluginInfo.getPluginSettings(), is(new PluggableInstanceSettings(pluginSettings, new PluginView("settings view"))));

    }

    @Test
    public void shouldThrowAnExceptionIfScmConfigReturnedByPluginIsNull() {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);
        when(extension.getSCMConfiguration("plugin1")).thenReturn(null);
        thrown.expectMessage("Plugin[plugin1] returned null scm configuration");
        new SCMPluginInfoBuilder(extension).pluginInfoFor(descriptor);
    }

    @Test
    public void shouldThrowAnExceptionIfScmViewReturnedByPluginIsNull() {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);
        when(extension.getSCMView("plugin1")).thenReturn(null);
        thrown.expectMessage("Plugin[plugin1] returned null scm view");
        new SCMPluginInfoBuilder(extension).pluginInfoFor(descriptor);
    }
}