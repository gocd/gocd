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

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.authentication.models.AuthenticationPluginConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AuthenticationPluginRegistryTest {
    private AuthenticationPluginRegistry registry;

    @Before
    public void setUp() {
        registry = new AuthenticationPluginRegistry();
        registry.registerPlugin("plugin-id-1", new AuthenticationPluginConfiguration("plugin 1", "image 1", true, true));
        registry.registerPlugin("plugin-id-2", new AuthenticationPluginConfiguration("plugin 2", null, false, false));
        registry.registerPlugin("plugin-id-3", new AuthenticationPluginConfiguration("plugin 3", "image 3", true, true));
        registry.registerPlugin("plugin-id-4", new AuthenticationPluginConfiguration("plugin 4", null, false, false));
    }

    @After
    public void tearDown() {
        registry.clear();
    }

    @Test
    public void shouldGetAllAuthenticationPlugins() {
        verifySetContents(registry.getAuthenticationPlugins(), "plugin-id-1", "plugin-id-2", "plugin-id-3", "plugin-id-4");
    }

    @Test
    public void shouldGetAllPluginsThatSupportsWebBasedAuthentication() {
        verifySetContents(registry.getPluginsThatSupportsWebBasedAuthentication(), "plugin-id-1", "plugin-id-3");
    }

    @Test
    public void shouldGetAllPluginsThatSupportsPasswordBasedAuthentication() {
        verifySetContents(registry.getPluginsThatSupportsPasswordBasedAuthentication(), "plugin-id-1", "plugin-id-3");
    }

    @Test
    public void shouldGetIndividualConfigurationExistingPlugin() {
        verifyConfigurationFor("plugin-id-1", "plugin 1", "image 1", true, true, true);
        verifyConfigurationFor("plugin-id-2", "plugin 2", null, false, false, true);
        verifyConfigurationFor("plugin-id-3", "plugin 3", "image 3", true, true, false);
        verifyConfigurationFor("plugin-id-4", "plugin 4", null, false, false, false);
    }

    @Test
    public void shouldGetIndividualConfigurationForNonExistingPlugin() {
        String pluginId = "non-existing-plugin";
        assertThat(registry.getDisplayNameFor(pluginId), is(nullValue()));
        assertThat(registry.getDisplayImageURLFor(pluginId), is(nullValue()));
        assertThat(registry.supportsPasswordBasedAuthentication(pluginId), is(false));
    }

    @Test
    public void shouldDeregisterPlugins() {
        registry.deregisterPlugin("plugin-id-2");
        registry.deregisterPlugin("plugin-id-3");

        verifySetContents(registry.getAuthenticationPlugins(), "plugin-id-1", "plugin-id-4");
        verifySetContents(registry.getPluginsThatSupportsWebBasedAuthentication(), "plugin-id-1");
        verifySetContents(registry.getPluginsThatSupportsPasswordBasedAuthentication(), "plugin-id-1");
    }

    private void verifySetContents(Set<String> set, String... values) {
        assertThat(set.size(), is(values.length));
        assertThat(set, containsInAnyOrder(values));
    }

    private void verifyConfigurationFor(String pluginId, String displayName, String displayImageURL, boolean supportsWebBasedAuthentication,
                                        boolean supportsPasswordBasedAuthentication, boolean supportsUserSearch) {
        assertThat(registry.getDisplayNameFor(pluginId), is(displayName));
        assertThat(registry.getDisplayImageURLFor(pluginId), is(displayImageURL));
        assertThat(registry.supportsPasswordBasedAuthentication(pluginId), is(supportsWebBasedAuthentication));
        assertThat(registry.supportsPasswordBasedAuthentication(pluginId), is(supportsPasswordBasedAuthentication));
    }
}
