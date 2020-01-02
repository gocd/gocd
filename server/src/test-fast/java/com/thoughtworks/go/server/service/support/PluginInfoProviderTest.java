/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginInfoProviderTest {

    private PluginInfoProvider pluginInfoProvider;

    @Mock
    private DefaultPluginInfoFinder pluginInfoFinder;

    @Mock
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        pluginInfoProvider = new PluginInfoProvider(pluginInfoFinder, pluginManager);
    }

    @Test
    public void shouldGetPluginInformationAsJson() {
        when(pluginManager.plugins())
                .thenReturn(Arrays.asList(passwordFilePluginDescriptor(), ldapPluginDescriptor()));
        when(pluginInfoFinder.pluginInfoFor("cd.go.authentication.passwordfile"))
                .thenReturn(new CombinedPluginInfo(
                        new PluginInfo(passwordFilePluginDescriptor(), "authorization", null, null)));
        Map<String, Object> json = pluginInfoProvider.asJson();

        Map<String, Object> expectedJson = new LinkedHashMap<>();
        Map<String, Object> passwordFilePluginJson = new LinkedHashMap<>();
        passwordFilePluginJson.put("id", "cd.go.authentication.passwordfile");
        passwordFilePluginJson.put("type", Collections.singletonList("authorization"));
        passwordFilePluginJson.put("version", "1.0.1-48");
        passwordFilePluginJson.put("bundled_plugin", true);
        passwordFilePluginJson.put("status", passwordFilePluginDescriptor().getStatus());

        Map<String, Object> ldapPluginJson = new LinkedHashMap<>();
        ldapPluginJson.put("id", "cd.go.authentication.ldap");
        ldapPluginJson.put("type", Collections.emptyList());
        ldapPluginJson.put("version", "1.1");
        ldapPluginJson.put("bundled_plugin", true);
        ldapPluginJson.put("status", ldapPluginDescriptor().getStatus());

        expectedJson.put("plugins", Arrays.asList(passwordFilePluginJson, ldapPluginJson));

        assertThat(json, is(expectedJson));
    }

    private GoPluginDescriptor passwordFilePluginDescriptor() {
        return getPluginDescriptor("cd.go.authentication.passwordfile", "/usr/gocd-filebased-authentication-plugin.jar", "1.0.1-48");
    }

    private GoPluginDescriptor ldapPluginDescriptor() {
        return getPluginDescriptor("cd.go.authentication.ldap", "/usr/gocd-ldap-authentication-plugin.jar", "1.1");
    }

    private GoPluginDescriptor getPluginDescriptor(String pluginId, String pluginJarFileLocation, String version) {
        return GoPluginDescriptor.builder()
                .version("1")
                .id(pluginId)
                .pluginJarFileLocation(pluginJarFileLocation)
                .about(GoPluginDescriptor.About.builder().version(version).build())
                .isBundledPlugin(true).build();
    }
}
