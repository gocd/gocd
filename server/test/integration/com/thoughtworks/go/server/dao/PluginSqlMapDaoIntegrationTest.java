/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.dao;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PluginSqlMapDaoIntegrationTest {
    @Autowired
    private PluginSqlMapDao pluginSqlMapDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        pluginSqlMapDao.deleteAllPlugins();
    }

    @After
    public void teardown() throws Exception {
        pluginSqlMapDao.deleteAllPlugins();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSavePlugin() throws Exception {
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(0));

        Plugin plugin = savePlugin("plugin-id");

        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(1));
        Plugin pluginInDB = pluginSqlMapDao.getAllPlugins().get(0);
        assertThat(pluginInDB, is(plugin));
    }

    @Test
    public void shouldUpdatePlugin() throws Exception {
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(0));

        Plugin plugin = savePlugin("plugin-id");

        plugin.setConfiguration(getConfigurationJSON("k1", "v1"));
        pluginSqlMapDao.saveOrUpdate(plugin);

        Plugin pluginInDB = pluginSqlMapDao.findPlugin("plugin-id");
        assertThat(pluginInDB, is(plugin));
    }

    @Test
    public void shouldReturnCorrectPluginIfPluginIdExists() throws Exception {
        Plugin plugin = savePlugin("plugin-id");

        Plugin pluginInDB = pluginSqlMapDao.findPlugin("plugin-id");
        assertThat(pluginInDB, is(plugin));
    }

    @Test
    public void shouldReturnNullPluginIfPluginIdDoesNotExist() throws Exception {
        Plugin pluginInDB = pluginSqlMapDao.findPlugin("non-existing-plugin-id");
        assertThat(pluginInDB, is((Plugin) new NullPlugin()));
    }

    @Test
    public void shouldReturnAllPlugins() throws Exception {
        Plugin plugin1 = savePlugin("plugin-id-1");

        List<Plugin> plugins = pluginSqlMapDao.getAllPlugins();
        assertThat(plugins.size(), is(1));
        assertThat(plugins.get(0), is(plugin1));

        Plugin plugin2 = savePlugin("plugin-id-2");

        plugins = pluginSqlMapDao.getAllPlugins();
        assertThat(plugins.size(), is(2));
        assertThat(plugins, containsInAnyOrder(plugin1, plugin2));
    }

    @Test
    public void shouldDeleteAllPlugins() throws Exception {
        savePlugin("plugin-id-1");
        savePlugin("plugin-id-2");

        List<Plugin> plugins = pluginSqlMapDao.getAllPlugins();
        assertThat(plugins.size(), is(2));

        pluginSqlMapDao.deleteAllPlugins();

        plugins = pluginSqlMapDao.getAllPlugins();
        assertThat(plugins.size(), is(0));
    }

    private Plugin savePlugin(String pluginId) {
        Plugin plugin = new Plugin(pluginId, getConfigurationJSON("k1", "v1", "k2", "v2"));
        pluginSqlMapDao.saveOrUpdate(plugin);
        return plugin;
    }

    private String getConfigurationJSON(String... args) {
        assertThat(args.length % 2, is(0));

        Map<String, String> configuration = new HashMap<String, String>();
        for (int i = 0; i < args.length - 2; i = i + 2) {
            configuration.put(args[i], args[i + 1]);
        }
        return new GsonBuilder().create().toJson(configuration);
    }
}