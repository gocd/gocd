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

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;

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

        when(systemEnvironment.getBundledPluginAbsolutePath()).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironment.getExternalPluginAbsolutePath()).thenReturn(externalPluginsDir.getAbsolutePath());

        pluginsList = new PluginsList(systemEnvironment);
    }

    @Test
    public void shouldUpdatePluginsList() throws Exception {
        pluginsList.update();
        PluginsList.PluginEntries bundled = (PluginsList.PluginEntries) ReflectionUtil.getField(pluginsList, "bundled");
        PluginsList.PluginEntries external = (PluginsList.PluginEntries) ReflectionUtil.getField(pluginsList, "external");

        assertThat(bundled.size(), is(1));
        assertThat(bundled.get(0).getName(), is("yum.jar"));
        assertThat(bundled.get(0).getMd5(), is("LAVBbwaDykricDnAP57klg=="));

        assertThat(external.size(), is(3));
        assertThat(external.get(0).getName(), is("external1.jar"));
        assertThat(external.get(0).getMd5(), is("+yWDK4+tYQtfqyh3tmT95A=="));
        assertThat(external.get(1).getName(), is("external2.jar"));
        assertThat(external.get(1).getMd5(), is("DS/Oa0vv5URteXfzSU7mvQ=="));
        assertThat(external.get(2).getName(), is("external3.jar"));
        assertThat(external.get(2).getMd5(), is("OQV644wsBJgbtQWR+L3UXA=="));
    }

    @Test
    public void shouldHaveThePluginsListedInSortedOrderBasedOnNameInPluginsJSON() throws Exception {
        FileUtils.cleanDirectory(externalPluginsDir);
        FileUtil.createFilesByPath(externalPluginsDir, "foo", "bar", "baz", "ano");
        pluginsList.update();

        String pluginsJSON = pluginsList.getPluginsJSON();

        PluginsList deserialized = JsonHelper.fromJson(pluginsJSON, PluginsList.class);
        PluginsList.PluginEntries externalPlugins = (PluginsList.PluginEntries) ReflectionUtil.getField(deserialized, "external");

        assertThat(externalPlugins.size(), is(4));
        assertThat(externalPlugins.get(0).getName(), is("ano"));
        assertThat(externalPlugins.get(1).getName(), is("bar"));
        assertThat(externalPlugins.get(2).getName(), is("baz"));
        assertThat(externalPlugins.get(3).getName(), is("foo"));
    }

    @Test
    public void shouldRemoveOlderEntriesDuringUpdate() throws Exception {
        pluginsList.update();
        PluginsList.PluginEntries bundled = (PluginsList.PluginEntries) ReflectionUtil.getField(pluginsList, "bundled");
        assertThat(bundled.size(), is(1));

        File bundledYumPlugin = new File(bundledPluginsDir, "yum.jar");
        FileUtils.deleteQuietly(bundledYumPlugin);

        pluginsList.update();
        assertThat(bundled.isEmpty(), is(true));
    }

    @Test
    public void shouldGetPluginsJSON() throws Exception {
        pluginsList.update();
        assertThat(ReflectionUtil.getField(pluginsList, "pluginsJSON"), is(nullValue()));

        String allPluginsJSON = pluginsList.getPluginsJSON();

        assertThat(ReflectionUtil.getField(pluginsList, "pluginsJSON"), is(notNullValue()));
        assertThat(allPluginsJSON, is("{\"bundled\":[{\"name\":\"yum.jar\",\"md5\":\"LAVBbwaDykricDnAP57klg\\u003d\\u003d\"}],\"external\":[{\"name\":\"external1.jar\",\"md5\":\"+yWDK4+tYQtfqyh3tmT95A\\u003d\\u003d\"},{\"name\":\"external2.jar\",\"md5\":\"DS/Oa0vv5URteXfzSU7mvQ\\u003d\\u003d\"},{\"name\":\"external3.jar\",\"md5\":\"OQV644wsBJgbtQWR+L3UXA\\u003d\\u003d\"}]}"));
    }
}
