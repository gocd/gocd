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

import com.thoughtworks.go.plugin.infra.*;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PluginsInitializerTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private SystemEnvironment systemEnvironment;
    private File goPluginsDir;
    private PluginsInitializer pluginsInitializer;
    private PluginManager pluginManager;
    private PluginExtensionsAndVersionValidator pluginExtensionsAndVersionValidator;
    private ElasticAgentInformationMigrator elasticAgentInformationMigrator;
    private ConfigRepositoryInitializer configRepositoryInitializer;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        goPluginsDir = temporaryFolder.newFolder("go-plugins");
        when(systemEnvironment.get(SystemEnvironment.PLUGIN_GO_PROVIDED_PATH)).thenReturn(goPluginsDir.getAbsolutePath());
        pluginManager = mock(PluginManager.class);
        pluginExtensionsAndVersionValidator = mock(PluginExtensionsAndVersionValidator.class);
        elasticAgentInformationMigrator = mock(ElasticAgentInformationMigrator.class);
        configRepositoryInitializer = mock(ConfigRepositoryInitializer.class);
        pluginsInitializer = new PluginsInitializer(pluginManager, systemEnvironment, new ZipUtil(), pluginExtensionsAndVersionValidator, elasticAgentInformationMigrator, configRepositoryInitializer) {
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
        pluginsInitializer = new PluginsInitializer(pluginManager, systemEnvironment, zipUtil, pluginExtensionsAndVersionValidator, elasticAgentInformationMigrator, configRepositoryInitializer) {
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
        assertThat(FileUtils.listFiles(goPluginsDir, null, true).size(), is(2));
    }

    @Test
    public void shouldNotReplacePluginsIfTheSameVersionWasAlreadyExploded() throws IOException {
        String version = "13.3.0(17222-4c7fabcb9c9e9c)";
        File versionFile = temporaryFolder.newFile("go-plugins/version.txt");
        FileUtils.writeStringToFile(versionFile, version, UTF_8);
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir, null, true);
        assertThat(collection.size(), is(1));
        assertThat(collection.contains(versionFile), is(true));
    }

    @Test
    public void shouldReplacePluginsIfTheDifferentVersionOfPluginsAvailable() throws IOException {
        FileUtils.writeStringToFile(temporaryFolder.newFile("go-plugins/version.txt"), "13.2.0(17222-4c7fabcb9c9e9c)", UTF_8);
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir, null, true);
        assertThat(collection.size(), is(2));
    }

    @Test
    public void shouldDeleteAllExistingPluginsWhileExplodingPluginsZip() throws IOException {
        File oldPlugin = temporaryFolder.newFile("go-plugins/old-plugin.jar");
        FileUtils.writeStringToFile(temporaryFolder.newFile("go-plugins/version.txt"), "13.2.0(17222-4c7fabcb9c9e9c)", UTF_8);
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir, null, true);
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
