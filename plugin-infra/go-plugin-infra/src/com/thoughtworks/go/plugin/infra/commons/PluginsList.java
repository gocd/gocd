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

import com.google.gson.Gson;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.comparator.NameFileComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.FileDigester.md5DigestOfFile;

@Component
public class PluginsList {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PluginsList.class);
    private String pluginsJSON;

    private SystemEnvironment systemEnvironment;
    private final Map<String, String> bundledPluginsMap;
    private final Map<String, String> externalPluginsMap;

    @Autowired
    public PluginsList(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        bundledPluginsMap = new LinkedHashMap<String, String>();
        externalPluginsMap = new LinkedHashMap<String, String>();
    }

    public void updatePluginsList() {
        try {
            File bundledPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_GO_PROVIDED_PATH));
            fillDetailsOfPluginsInDirectory(bundledPlugins, bundledPluginsMap);

            File externalPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH));
            fillDetailsOfPluginsInDirectory(externalPlugins, externalPluginsMap);

            clearPluginsJSON();
        } catch (Exception e) {
            LOG.warn("Error occurred while generating plugin map.", e);
        }
    }

    void fillDetailsOfPluginsInDirectory(File directory, Map<String, String> pluginsMap) throws Exception {
        pluginsMap.clear();
        File[] pluginJars = directory.exists() ? directory.listFiles() : null;
        if (pluginJars != null && pluginJars.length != 0) {
            Arrays.sort(pluginJars, NameFileComparator.NAME_COMPARATOR);
            for (File pluginJar : pluginJars) {
                pluginsMap.put(pluginJar.getName(), md5DigestOfFile(pluginJar));
            }
        }
    }

    public String getPluginsJSON() throws IOException {
        if (pluginsJSON == null) {
            createPluginsJSON();
        }
        return pluginsJSON;
    }

    private void createPluginsJSON() throws IOException {
        Map<String, Map<String, String>> allPluginsMap = new LinkedHashMap<String, Map<String, String>>();
        allPluginsMap.put("bundled", bundledPluginsMap);
        allPluginsMap.put("external", externalPluginsMap);
        pluginsJSON = new Gson().toJson(allPluginsMap);
    }

    private void clearPluginsJSON() {
        pluginsJSON = null;
    }
}