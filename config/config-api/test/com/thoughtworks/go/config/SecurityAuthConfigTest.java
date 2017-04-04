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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityAuthConfigTest {
    @Test
    public void addConfigurations_shouldAddConfigurationsWithValue() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name"));
        AuthorizationMetadataStore store = mock(AuthorizationMetadataStore.class);

        SecurityAuthConfig profile = new SecurityAuthConfig("id", "plugin_id", store);

        profile.addConfigurations(Arrays.asList(property));

        assertThat(profile.size(), is(1));
        assertThat(profile, contains(new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name"))));
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithEncryptedValue() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name"));
        AuthorizationMetadataStore store = mock(AuthorizationMetadataStore.class);

        SecurityAuthConfig profile = new SecurityAuthConfig("id", "plugin_id", store);

        profile.addConfigurations(Arrays.asList(property));

        assertThat(profile.size(), is(1));
        assertThat(profile, contains(new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name"))));
    }

    @Test
    public void addConfiguration_shouldEncryptASecureVariable() throws Exception {
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("password", new Metadata(true, true))));
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(pluginDescriptor("plugin_id"), profileSettings, null, null, null);
        AuthorizationMetadataStore store = mock(AuthorizationMetadataStore.class);

        when(store.getPluginInfo(pluginInfo.getDescriptor().id())).thenReturn(pluginInfo);
        SecurityAuthConfig profile = new SecurityAuthConfig("id", "plugin_id", store);
        profile.addConfigurations(Arrays.asList(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));

        assertThat(profile.size(), is(1));
        assertTrue(profile.first().isSecure());
    }

    @Test
    public void addConfiguration_shouldIgnoreEncryptionInAbsenceOfCorrespondingConfigurationInStore() throws Exception {
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(pluginDescriptor("plugin_id"), new PluggableInstanceSettings(new ArrayList<>()), null, null, null);
        AuthorizationMetadataStore store = mock(AuthorizationMetadataStore.class);

        when(store.getPluginInfo(pluginInfo.getDescriptor().id())).thenReturn(pluginInfo);
        SecurityAuthConfig profile = new SecurityAuthConfig("id", "plugin_id", store);
        profile.addConfigurations(Arrays.asList(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));

        assertThat(profile.size(), is(1));
        assertFalse(profile.first().isSecure());
        assertThat(profile, contains(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));
    }

    @Test
    public void postConstruct_shouldEncryptSecureConfigurations() throws Exception {
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("password", new Metadata(true, true))));
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(pluginDescriptor("plugin_id"), profileSettings, null, null, null);
        AuthorizationMetadataStore store = mock(AuthorizationMetadataStore.class);

        when(store.getPluginInfo(pluginInfo.getDescriptor().id())).thenReturn(pluginInfo);
        SecurityAuthConfig profile = new SecurityAuthConfig("id", "plugin_id", store, new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

        profile.encryptSecureConfigurations();

        assertThat(profile.size(), is(1));
        assertTrue(profile.first().isSecure());
    }

    private PluginDescriptor pluginDescriptor(String pluginId) {
        return new PluginDescriptor() {
            @Override
            public String id() {
                return pluginId;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public About about() {
                return null;
            }
        };
    }
}
