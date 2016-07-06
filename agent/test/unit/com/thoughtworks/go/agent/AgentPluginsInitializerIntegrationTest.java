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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipBuilder;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.agent.launcher.DownloadableFile.AGENT_PLUGINS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/* Some parts are mocked, as in AgentPluginsInitializerTest, but the file system (through ZipUtil) is not. */
@RunWith(MockitoJUnitRunner.class)
public class AgentPluginsInitializerIntegrationTest {
    @Mock
    private PluginManager pluginManager;
    @Mock
    private DefaultPluginJarLocationMonitor pluginJarLocationMonitor;
    @Mock
    private SystemEnvironment systemEnvironment;

    private File directoryForUnzippedPlugins;
    private AgentPluginsInitializer agentPluginsInitializer;

    @Before
    public void setUp() throws Exception {
        agentPluginsInitializer = new AgentPluginsInitializer(pluginManager, pluginJarLocationMonitor, new ZipUtil(), systemEnvironment);

        directoryForUnzippedPlugins = setupUnzippedPluginsDirectoryStructure();
        when(systemEnvironment.get(SystemEnvironment.AGENT_PLUGINS_PATH)).thenReturn(directoryForUnzippedPlugins.getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {
        cleanupAgentPluginsFile();
        TestFileUtil.cleanTempFiles();
    }

    @Test
    public void shouldRemoveExistingBundledPluginsBeforeInitializingNewPlugins() throws Exception {
        File existingBundledPlugin = new File(directoryForUnzippedPlugins, "bundled/existing-plugin-1.jar");

        setupAgentsPluginFile().withBundledPlugin("new-plugin-1.jar", "SOME-PLUGIN-CONTENT").done();
        FileUtils.writeStringToFile(existingBundledPlugin, "OLD-CONTENT");

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(existingBundledPlugin.exists(), is(false));
        assertThat(new File(directoryForUnzippedPlugins, "bundled/new-plugin-1.jar").exists(), is(true));
    }

    @Test
    public void shouldReplaceExistingBundledPluginsWithNewPluginsOfSameName() throws Exception {
        File bundledPlugin = new File(directoryForUnzippedPlugins, "bundled/plugin-1.jar");

        setupAgentsPluginFile().withBundledPlugin("plugin-1.jar", "SOME-NEW-CONTENT").done();
        FileUtils.writeStringToFile(bundledPlugin, "OLD-CONTENT");

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(bundledPlugin.exists(), is(true));
        assertThat(FileUtils.readFileToString(bundledPlugin), is("SOME-NEW-CONTENT"));
    }

    @Test
    public void shouldRemoveExistingExternalPluginsBeforeInitializingNewPlugins() throws Exception {
        File existingExternalPlugin = new File(directoryForUnzippedPlugins, "external/existing-plugin-1.jar");

        setupAgentsPluginFile().withExternalPlugin("new-plugin-1.jar", "SOME-PLUGIN-CONTENT").done();
        FileUtils.writeStringToFile(existingExternalPlugin, "OLD-CONTENT");

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(existingExternalPlugin.exists(), is(false));
        assertThat(new File(directoryForUnzippedPlugins, "external/new-plugin-1.jar").exists(), is(true));
    }

    @Test
    public void shouldReplaceExistingExternalPluginsWithNewPluginsOfSameName() throws Exception {
        File externalPlugin = new File(directoryForUnzippedPlugins, "external/plugin-1.jar");

        setupAgentsPluginFile().withExternalPlugin("plugin-1.jar", "SOME-NEW-CONTENT").done();
        FileUtils.writeStringToFile(externalPlugin, "OLD-CONTENT");

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(externalPlugin.exists(), is(true));
        assertThat(FileUtils.readFileToString(externalPlugin), is("SOME-NEW-CONTENT"));
    }

    @Test
    public void shouldRemoveAnExistingPluginWhenItHasBeenRemovedFromTheServerSide() throws Exception {
        File existingExternalPlugin = new File(directoryForUnzippedPlugins, "external/plugin-1.jar");

        setupAgentsPluginFile().done();
        FileUtils.writeStringToFile(existingExternalPlugin, "OLD-CONTENT");

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(existingExternalPlugin.exists(), is(false));
    }

    private File setupUnzippedPluginsDirectoryStructure() throws IOException {
        File dir = TestFileUtil.createTempFolder("unzipped-plugins");
        FileUtils.forceMkdir(new File(dir, "bundled"));
        FileUtils.forceMkdir(new File(dir, "external"));
        return dir;
    }

    private SetupOfAgentPluginsFile setupAgentsPluginFile() throws IOException {
        return new SetupOfAgentPluginsFile(new File(AGENT_PLUGINS.getLocalFileName()));
    }

    private void cleanupAgentPluginsFile() throws IOException {
        File pluginsFile = new File(AGENT_PLUGINS.getLocalFileName());
        FileUtils.deleteQuietly(pluginsFile);
    }

    private class SetupOfAgentPluginsFile {
        private final File bundledPluginsDir;
        private final File externalPluginsDir;
        private final ZipUtil zipUtil;
        private final File dummyFileSoZipFileIsNotEmpty;
        private File pluginsZipFile;

        public SetupOfAgentPluginsFile(File pluginsZipFile) throws IOException {
            this.pluginsZipFile = pluginsZipFile;
            this.bundledPluginsDir = TestFileUtil.createTempFolder("bundled");
            this.externalPluginsDir = TestFileUtil.createTempFolder("external");
            this.dummyFileSoZipFileIsNotEmpty = TestFileUtil.createTempFile("dummy.txt");
            this.zipUtil = new ZipUtil();
        }

        public SetupOfAgentPluginsFile withBundledPlugin(String pluginFileName, String pluginFileContent) throws IOException {
            FileUtils.writeStringToFile(new File(bundledPluginsDir, pluginFileName), pluginFileContent);
            return this;
        }

        public SetupOfAgentPluginsFile withExternalPlugin(String pluginFileName, String pluginFileContent) throws IOException {
            FileUtils.writeStringToFile(new File(externalPluginsDir, pluginFileName), pluginFileContent);
            return this;
        }

        public File done() throws IOException {
            ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(pluginsZipFile, true);
            zipBuilder.add("bundled", bundledPluginsDir).add("external", externalPluginsDir).add("dummy.txt", dummyFileSoZipFileIsNotEmpty).done();
            return pluginsZipFile;
        }
    }
}