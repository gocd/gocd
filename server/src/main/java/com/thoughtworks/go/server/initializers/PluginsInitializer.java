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
package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.plugin.infra.ElasticAgentInformationMigrator;
import com.thoughtworks.go.plugin.infra.PluginExtensionsAndVersionValidator;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginPostLoadHook;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.SystemEnvironment.DEFAULT_PLUGINS_ZIP;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_GO_PROVIDED_PATH;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class PluginsInitializer implements Initializer {
    private final PluginManager pluginManager;
    private final SystemEnvironment systemEnvironment;
    private static final Logger LOG = LoggerFactory.getLogger(PluginsInitializer.class);
    private ZipUtil zipUtil;

    @Autowired
    public PluginsInitializer(PluginManager pluginManager, SystemEnvironment systemEnvironment, ZipUtil zipUtil, PluginExtensionsAndVersionValidator pluginExtensionsAndVersionValidator, ElasticAgentInformationMigrator elasticAgentInformationMigrator) {
        this.pluginManager = pluginManager;
        this.systemEnvironment = systemEnvironment;
        this.zipUtil = zipUtil;

        this.pluginManager.addPluginPostLoadHook(pluginExtensionsAndVersionValidator);
        this.pluginManager.addPluginPostLoadHook(elasticAgentInformationMigrator);
    }

    @Override
    public void initialize() {
        cleanupOldPluginDirectories();
        try {
            File bundledPluginsDirectory = new File(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH));
            if (shouldReplaceBundledPlugins(bundledPluginsDirectory)) {
                FileUtils.cleanDirectory(bundledPluginsDirectory);
                zipUtil.unzip(getPluginsZipStream(), bundledPluginsDirectory);
            }
            pluginManager.startInfrastructure(true);
        } catch (Exception e) {
            LOG.error("Could not extract bundled plugins to default bundled directory", e);
        }
    }

    @Override
    public void startDaemon() {

    }

    ZipInputStream getPluginsZipStream() {
        return new ZipInputStream(this.getClass().getResourceAsStream(systemEnvironment.get(DEFAULT_PLUGINS_ZIP)));
    }

    private boolean shouldReplaceBundledPlugins(File bundledPluginsDirectory) throws IOException {
        File versionFile = new File(bundledPluginsDirectory, "version.txt");
        if (!versionFile.exists()) return true;
        String currentlyInstalledVersion = FileUtils.readFileToString(versionFile, UTF_8);
        String versionNumberInZip = zipUtil.getFileContentInsideZip(getPluginsZipStream(), "version.txt");
        return !currentlyInstalledVersion.equals(versionNumberInZip);
    }

    private void cleanupOldPluginDirectories() {
        FileUtils.deleteQuietly(new File("plugins_bundles"));
        FileUtils.deleteQuietly(new File("plugins-new"));
    }
}
