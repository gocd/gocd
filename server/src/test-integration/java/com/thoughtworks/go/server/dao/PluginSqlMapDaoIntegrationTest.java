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
package com.thoughtworks.go.server.dao;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.server.cache.GoCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PluginSqlMapDaoIntegrationTest {
    @Autowired
    private PluginSqlMapDao pluginSqlMapDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
        pluginSqlMapDao.deleteAllPlugins();
    }

    @AfterEach
    public void teardown() throws Exception {
        pluginSqlMapDao.deleteAllPlugins();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSavePlugin() {
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(0));

        Plugin plugin = savePlugin("plugin-id");

        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(1));
        Plugin pluginInDB = pluginSqlMapDao.getAllPlugins().get(0);
        assertThat(pluginInDB, is(plugin));
    }

    @Test
    public void shouldUpdatePlugin() {
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(0));

        Plugin plugin = savePlugin("plugin-id");

        plugin.setConfiguration(getConfigurationJSON("k1", "v1"));
        pluginSqlMapDao.saveOrUpdate(plugin);

        Plugin pluginInDB = pluginSqlMapDao.findPlugin("plugin-id");
        assertThat(pluginInDB, is(plugin));
    }

    @Test
    public void shouldReturnCorrectPluginIfPluginIdExists() {
        Plugin plugin = savePlugin("plugin-id");

        assertThat(goCache.get(pluginSqlMapDao.cacheKeyForPluginSettings("plugin-id")), is(nullValue()));
        Plugin pluginInDB = pluginSqlMapDao.findPlugin("plugin-id");
        assertThat(pluginInDB, is(plugin));
        assertThat(goCache.get(pluginSqlMapDao.cacheKeyForPluginSettings("plugin-id")), is(pluginInDB));
    }

    @Test
    public void shouldReturnNullPluginIfPluginIdDoesNotExist() {
        Plugin pluginInDB = pluginSqlMapDao.findPlugin("non-existing-plugin-id");
        assertThat(pluginInDB, is(new NullPlugin()));
    }

    @Test
    public void shouldReturnAllPlugins() {
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
    public void shouldDoNothingWhenAPluginToDeleteDoesNotExists() {
        String pluginId = "my.fancy.plugin.id";

        assertThat(goCache.get(pluginSqlMapDao.cacheKeyForPluginSettings(pluginId)), is(nullValue()));
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(0));

        pluginSqlMapDao.deletePluginIfExists(pluginId);

        assertThat(goCache.get(pluginSqlMapDao.cacheKeyForPluginSettings(pluginId)), is(nullValue()));
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(0));
    }

    @Test
    public void shouldDeleteAPlugin() {
        String pluginId = "my.fancy.plugin.id";

        Plugin plugin = savePlugin(pluginId);

        Plugin pluginInDB = pluginSqlMapDao.findPlugin(pluginId);
        assertThat(pluginInDB, is(plugin));

        assertThat(goCache.get(pluginSqlMapDao.cacheKeyForPluginSettings(pluginId)), is(plugin));
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(1));

        pluginSqlMapDao.deletePluginIfExists(pluginId);

        assertThat(goCache.get(pluginSqlMapDao.cacheKeyForPluginSettings(pluginId)), is(nullValue()));
        assertThat(pluginSqlMapDao.getAllPlugins().size(), is(0));
    }

    @Test
    public void shouldDeleteAllPlugins() {
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

        Map<String, String> configuration = new HashMap<>();
        for (int i = 0; i < args.length - 2; i = i + 2) {
            configuration.put(args[i], args[i + 1]);
        }
        return new GsonBuilder().create().toJson(configuration);
    }
}
