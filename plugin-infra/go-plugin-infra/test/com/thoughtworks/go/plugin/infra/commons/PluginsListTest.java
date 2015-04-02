/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra.commons;

import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_GO_PROVIDED_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginsListTest {
    @Mock
    private SystemEnvironment systemEnvironment;
    private PluginsList pluginsList;
    private File bundledPluginsDir;
    private File externalPluginsDir;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        temporaryFolder.create();
        bundledPluginsDir = temporaryFolder.newFolder("bundled");
        externalPluginsDir = temporaryFolder.newFolder("external");

        File bundledYumPlugin = new File(bundledPluginsDir, "yum.jar");
        File externalPlugin1 = new File(externalPluginsDir, "external1.jar");
        File externalPlugin2 = new File(externalPluginsDir, "external2.jar");
        File externalPlugin3 = new File(externalPluginsDir, "external3.jar");
        FileUtils.writeStringToFile(bundledYumPlugin, bundledYumPlugin.getName());
        FileUtils.writeStringToFile(externalPlugin1, externalPlugin1.getName());
        FileUtils.writeStringToFile(externalPlugin2, externalPlugin2.getName());
        FileUtils.writeStringToFile(externalPlugin3, externalPlugin3.getName());

        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());

        pluginsList = new PluginsList(systemEnvironment);
    }

    @After
    public void tearDown() throws Exception {
        temporaryFolder.delete();
    }

    @Test
    public void shouldPopulateJarDetails() throws Exception {
        final Map<String, String> bundledPluginsMap = new LinkedHashMap<String, String>();
        pluginsList.fillDetailsOfPluginsInDirectory(bundledPluginsDir, bundledPluginsMap);

        assertThat(bundledPluginsMap.size(), is(1));
        assertThat(bundledPluginsMap.get("yum.jar"), is("LAVBbwaDykricDnAP57klg=="));

        final Map<String, String> externalPluginsMap = new LinkedHashMap<String, String>();
        pluginsList.fillDetailsOfPluginsInDirectory(externalPluginsDir, externalPluginsMap);

        assertThat(externalPluginsMap.size(), is(3));
        assertThat(externalPluginsMap.get("external1.jar"), is("+yWDK4+tYQtfqyh3tmT95A=="));
        assertThat(externalPluginsMap.get("external2.jar"), is("DS/Oa0vv5URteXfzSU7mvQ=="));
        assertThat(externalPluginsMap.get("external3.jar"), is("OQV644wsBJgbtQWR+L3UXA=="));
    }

    @Test
    public void shouldRemoveOlderEntriesInPluginsMap() throws Exception {
        final Map<String, String> bundledPluginsMap = new LinkedHashMap<String, String>();
        pluginsList.fillDetailsOfPluginsInDirectory(bundledPluginsDir, bundledPluginsMap);

        assertThat(bundledPluginsMap.size(), is(1));
        assertThat(bundledPluginsMap.get("yum.jar"), is("LAVBbwaDykricDnAP57klg=="));

        File bundledYumPlugin = new File(bundledPluginsDir, "yum.jar");
        FileUtils.deleteQuietly(bundledYumPlugin);

        pluginsList.fillDetailsOfPluginsInDirectory(bundledPluginsDir, bundledPluginsMap);
        assertThat(bundledPluginsMap.isEmpty(), is(true));
    }

    @Test
    public void shouldGetPluginsJSON() throws Exception {
        pluginsList.updatePluginsList();
        assertThat(ReflectionUtil.getField(pluginsList, "pluginsJSON"), is(nullValue()));

        String allPluginsJSON = pluginsList.getPluginsJSON();

        assertThat(ReflectionUtil.getField(pluginsList, "pluginsJSON"), is(notNullValue()));
        assertThat(allPluginsJSON, is("{\"bundled\":{\"yum.jar\":\"LAVBbwaDykricDnAP57klg\\u003d\\u003d\"},\"external\":{\"external1.jar\":\"+yWDK4+tYQtfqyh3tmT95A\\u003d\\u003d\",\"external2.jar\":\"DS/Oa0vv5URteXfzSU7mvQ\\u003d\\u003d\",\"external3.jar\":\"OQV644wsBJgbtQWR+L3UXA\\u003d\\u003d\"}}"));
    }
}