/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.helpers.FileSystemUtils;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class PluginsInitializerTest {

    private SystemEnvironment systemEnvironment;
    private File goPluginsDir;
    private PluginsInitializer pluginsInitializer;
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        goPluginsDir = FileSystemUtils.createDirectory("go-plugins");
        when(systemEnvironment.get(SystemEnvironment.PLUGIN_GO_PROVIDED_PATH)).thenReturn(goPluginsDir.getAbsolutePath());
        pluginManager = mock(PluginManager.class);
        pluginsInitializer = new PluginsInitializer(pluginManager, systemEnvironment, new ZipUtil()) {
            @Override
            ZipInputStream getPluginsZipStream() {
                try {
                    return new ZipInputStream(new FileInputStream(new File("test/data/dummy-plugins.zip")));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.forceDelete(goPluginsDir);
    }

    @Test
    public void shouldUnzipPluginsAndRegisterZipUpdaterBeforeStartingPluginsFramework() throws IOException {
        ZipUtil zipUtil = mock(ZipUtil.class);
        pluginsInitializer = new PluginsInitializer(pluginManager, systemEnvironment, zipUtil) {
            @Override
            ZipInputStream getPluginsZipStream() {
                try {
                    return new ZipInputStream(new FileInputStream(new File("test/data/dummy-plugins.zip")));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        pluginsInitializer.initialize();

        InOrder inOrder = inOrder(zipUtil, pluginManager);

        inOrder.verify(zipUtil).unzip(Matchers.<ZipInputStream>any(), Matchers.<File>any());
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
        File versionFile = FileSystemUtils.createFile("version.txt", goPluginsDir);
        FileUtils.writeStringToFile(versionFile, version);
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir, null, true);
        assertThat(collection.size(), is(1));
        assertThat(collection.contains(versionFile), is(true));
    }

    @Test
    public void shouldReplacePluginsIfTheDifferentVersionOfPluginsAvailable() throws IOException {
        File versionFile = FileSystemUtils.createFile("version.txt", goPluginsDir);
        FileUtils.writeStringToFile(versionFile, "13.2.0(17222-4c7fabcb9c9e9c)");
        pluginsInitializer.initialize();
        Collection collection = FileUtils.listFiles(goPluginsDir, null, true);
        assertThat(collection.size(), is(2));
    }

    @Test
    public void shouldDeleteAllExistingPluginsWhileExplodingPluginsZip() throws IOException {
        File versionFile = FileSystemUtils.createFile("version.txt", goPluginsDir);
        File oldPlugin = FileSystemUtils.createFile("old-plugin.jar", goPluginsDir);
        FileUtils.writeStringToFile(versionFile, "13.2.0(17222-4c7fabcb9c9e9c)");
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
}
