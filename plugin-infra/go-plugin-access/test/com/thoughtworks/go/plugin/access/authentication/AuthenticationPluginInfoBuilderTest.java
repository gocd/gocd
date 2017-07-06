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

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.authentication.models.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.domain.authentication.AuthenticationPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class AuthenticationPluginInfoBuilderTest {

    private AuthenticationExtension extension;

    @Before
    public void setUp() throws Exception {
        extension = mock(AuthenticationExtension.class);

        PluginSettingsConfiguration value = new PluginSettingsConfiguration();
        value.add(new PluginSettingsProperty("username", null).with(Property.REQUIRED, true).with(Property.SECURE, false));
        value.add(new PluginSettingsProperty("password", null).with(Property.REQUIRED, true).with(Property.SECURE, true));
        stub(extension.getPluginSettingsConfiguration("plugin1")).toReturn(value);
        stub(extension.getPluginConfiguration("plugin1")).toReturn(new AuthenticationPluginConfiguration("foo authentication plugin", "http://some-image", true, true));
    }

    @Test
    public void shouldBuildPluginInfoWith() throws Exception {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);
        stub(extension.getPluginSettingsView("plugin1")).toReturn("some-html");

        AuthenticationPluginInfo pluginInfo = new AuthenticationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        List<PluginConfiguration> pluginConfigurations = Arrays.asList(
                new PluginConfiguration("username", new Metadata(true, false)),
                new PluginConfiguration("password", new Metadata(true, true))
        );
        PluginView pluginView = new PluginView("some-html");

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("authentication"));
        assertThat(pluginInfo.getPluginSettings(), is(new PluggableInstanceSettings(pluginConfigurations, pluginView)));

        assertThat(pluginInfo.getDisplayImageURL(), is("http://some-image"));
        assertThat(pluginInfo.getDisplayName(), is("foo authentication plugin"));
        assertThat(pluginInfo.isSupportsPasswordBasedAuthentication(), is(true));
        assertThat(pluginInfo.isSupportsWebBasedAuthentication(), is(true));
    }

    @Test
    public void shouldContinueBuildingPluginInfoIfPluginSettingsIsNotProvidedByPlugin() {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);

        doThrow(new RuntimeException("foo")).when(extension).getPluginSettingsConfiguration("plugin1");
        AuthenticationPluginInfo pluginInfo = new AuthenticationPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("authentication"));
        assertNull(pluginInfo.getPluginSettings());

        assertThat(pluginInfo.getDisplayImageURL(), is("http://some-image"));
        assertThat(pluginInfo.getDisplayName(), is("foo authentication plugin"));
        assertThat(pluginInfo.isSupportsPasswordBasedAuthentication(), is(true));
        assertThat(pluginInfo.isSupportsWebBasedAuthentication(), is(true));
    }

}