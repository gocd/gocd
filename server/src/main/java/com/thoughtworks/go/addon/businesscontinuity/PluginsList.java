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

package com.thoughtworks.go.addon.businesscontinuity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class PluginsList implements PluginChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(PluginsList.class);
    private String pluginsJSON;

    private SystemEnvironment systemEnvironment;

    @Expose
    @SerializedName("bundled")
    private final PluginEntries bundled = new PluginEntries();

    @Expose
    @SerializedName("external")
    private final PluginEntries external = new PluginEntries();

    @Autowired
    public PluginsList(SystemEnvironment systemEnvironment, PluginManager pluginManager) {
        this.systemEnvironment = systemEnvironment;
    }

    void update() {
        try {
            bundled.initialize(new File(systemEnvironment.getBundledPluginAbsolutePath()));
            external.initialize(new File(systemEnvironment.getExternalPluginAbsolutePath()));
            clearPluginsJSON();
        } catch (Exception e) {
            LOG.warn("Error occurred while generating plugin map.", e);
        }
    }

    public String getPluginsJSON() {
        if (pluginsJSON == null) {
            pluginsJSON = JsonHelper.toJsonString(this);
        }
        return pluginsJSON;
    }

    private void clearPluginsJSON() {
        pluginsJSON = null;
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        update();
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        update();
    }

    static class PluginEntries extends ArrayList<PluginEntry> {
        private void initialize(File parent) throws IOException {
            clear();
            File[] pluginJars = parent.exists() ? parent.listFiles() : null;
            if (pluginJars != null && pluginJars.length != 0) {
                for (File pluginJar : pluginJars) {
                    add(new PluginEntry(pluginJar));
                }
            }
            Collections.sort(this);
        }
    }

    static class PluginEntry implements Comparable<PluginEntry> {
        @Expose
        @SerializedName("name")
        private final String name;

        @Expose
        @SerializedName("md5")
        private final String md5;

        private PluginEntry(File pluginJar) throws IOException {
            this.name = pluginJar.getName();
            try (FileInputStream fileInputStream = new FileInputStream(pluginJar)) {
                this.md5 = DigestUtils.md5Hex(fileInputStream);
            }
        }

        @Override
        public int compareTo(PluginEntry other) {
            if (other == null) return 1;
            return this.name.compareTo(other.name);
        }

        protected String getName() {
            return name;
        }

        protected String getMd5() {
            return md5;
        }
    }
}
