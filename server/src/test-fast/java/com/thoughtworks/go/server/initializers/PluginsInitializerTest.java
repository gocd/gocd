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
package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.plugin.infra.ElasticAgentInformationMigrator;
import com.thoughtworks.go.plugin.infra.PluginExtensionsAndVersionValidator;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PluginsInitializerTest {

    private SystemEnvironment systemEnvironment;
    @TempDir
    Path goPluginsDir;
    private PluginsInitializer pluginsInitializer;
    private PluginManager pluginManager;
    private PluginExtensionsAndVersionValidator pluginExtensionsAndVersionValidator;
    private ElasticAgentInformationMigrator elasticAgentInformationMigrator;

    @BeforeEach
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.PLUGIN_GO_PROVIDED_PATH)).thenReturn(goPluginsDir.toFile().getAbsolutePath());
        pluginManager = mock(PluginManager.class);
        pluginExtensionsAndVersionValidator = mock(PluginExtensionsAndVersionValidator.class);
        elasticAgentInformationMigrator = mock(ElasticAgentInformationMigrator.class);
        pluginsInitializer = new PluginsInitializer(pluginManager, systemEnvironment, new ZipUtil(), pluginExtensionsAndVersionValidator, elasticAgentInformationMigrator) {
            @Override
            public void startDaemon() {

            }

            @Override
            ZipInputStream getPluginsZipStream() {
                return new ZipInputStream(getClass().getResourceAsStream("/dummy-plugins.zip"));
            }
        };
    }

    @Test
    public void shouldRegisterPluginExtensionValidatorWithPluginManager() {
        verify(pluginManager, times(1)).addPluginPostLoadHook(pluginExtensionsAndVersionValidator);
    }

    @Test
    public void shouldUnzipPluginsAndRegisterZipUpdaterBeforeStartingPluginsFramework() throws IOException {
        ZipUtil zipUtil = mock(ZipUtil.class);
        pluginsInitializer = new PluginsInitializer(pluginManager, systemEnvironment, zipUtil, pluginExtensionsAndVersionValidator, elasticAgentInformationMigrator) {
            @Override
            public void startDaemon() {

            }

            @Override
            ZipInputStream getPluginsZipStream() {
                return new ZipInputStream(getClass().getResourceAsStream("/dummy-plugins.zip"));
            }
        };
        pluginsInitializer.initialize();

        InOrder inOrder = inOrder(zipUtil, pluginManager);

        inOrder.verify(zipUtil).unzip(any(ZipInputStream.class), any(File.class));
        inOrder.verify(pluginManager, times(1)).startInfrastructure(true);
    }

    @Test
    public void shouldUnzipPluginsZipToPluginsPath() throws IOException {
        pluginsInitializer.initialize();
        assertThat(FileUtils.listFiles(goPluginsDir.toFile(), null, true).size(), is(2));
    }

    @Test
    public void shouldNotReplacePluginsIfTheSameVersionWasAlreadyExploded() throws IOException {
        File versionFile = Files.writeString(goPluginsDir.resolve("version.txt"), "13.3.0(17222-4c7fabcb9c9e9c)", UTF_8).toFile();
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir.toFile(), null, true);
        assertThat(collection.size(), is(1));
        assertThat(collection.contains(versionFile), is(true));
    }

    @Test
    public void shouldReplacePluginsIfTheDifferentVersionOfPluginsAvailable() throws IOException {
        Files.writeString(goPluginsDir.resolve("version.txt"), "13.2.0(17222-4c7fabcb9c9e9c)", UTF_8).toFile();
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir.toFile(), null, true);
        assertThat(collection.size(), is(2));
    }

    @Test
    public void shouldDeleteAllExistingPluginsWhileExplodingPluginsZip() throws IOException {
        File oldPlugin = Files.createFile(goPluginsDir.resolve("old-plugin.jar")).toFile();
        Files.writeString(goPluginsDir.resolve("version.txt"), "13.2.0(17222-4c7fabcb9c9e9c)", UTF_8).toFile();
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir.toFile(), null, true);
        assertThat(collection.size(), is(2));
        assertThat(collection.contains(oldPlugin), is(false));
    }

    @Test
    public void shouldDeleteOldPluginDirectories() throws Exception {
        File pluginsBundles = new File("plugins_bundles");
        File pluginsNew = new File("plugins-new");
        try {
            FileUtils.forceMkdir(pluginsBundles);
            FileUtils.forceMkdir(pluginsNew);
            pluginsInitializer.initialize();
            assertThat("should have cleaned up  plugins_bundles folder", pluginsBundles.exists(), is(false));
            assertThat("should have cleaned up  plugins-new folder", pluginsNew.exists(), is(false));
        } finally {
            FileUtils.deleteQuietly(pluginsBundles);
            FileUtils.deleteQuietly(pluginsNew);
        }
    }

    @Test
    public void shouldSetElasticAgentInformationMigratorOnPluginManager() {
        verify(pluginManager, times(1)).addPluginPostLoadHook(elasticAgentInformationMigrator);
    }
}
