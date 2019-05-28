/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import java.util.*;

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
        return new GoPluginDescriptor("cd.go.authentication.passwordfile", "1.0.1-48", getAbout("1.0.1-48"),
                "/usr/gocd-filebased-authentication-plugin.jar", null, true);
    }

    private GoPluginDescriptor ldapPluginDescriptor() {
        return new GoPluginDescriptor("cd.go.authentication.ldap", "1.1", getAbout("1.1"),
                "/usr/gocd-ldap-authentication-plugin.jar", null, true);
    }

    private GoPluginDescriptor.About getAbout(String version) {
        return new GoPluginDescriptor.About("", version, "", "", null, null);
    }
}
