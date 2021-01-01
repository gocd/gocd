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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipBuilder;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.agent.launcher.DownloadableFile.AGENT_PLUGINS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/* Some parts are mocked, as in AgentPluginsInitializerTest, but the file system (through ZipUtil) is not. */
@EnableRuleMigrationSupport
public class AgentPluginsInitializerIntegrationTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    private PluginManager pluginManager;
    @Mock
    private DefaultPluginJarLocationMonitor pluginJarLocationMonitor;
    @Mock
    private SystemEnvironment systemEnvironment;

    private File directoryForUnzippedPlugins;
    private AgentPluginsInitializer agentPluginsInitializer;

    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);
        agentPluginsInitializer = new AgentPluginsInitializer(pluginManager, pluginJarLocationMonitor, new ZipUtil(), systemEnvironment);

        directoryForUnzippedPlugins = setupUnzippedPluginsDirectoryStructure();
        when(systemEnvironment.get(SystemEnvironment.AGENT_PLUGINS_PATH)).thenReturn(directoryForUnzippedPlugins.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanupAgentPluginsFile();
    }

    @Test
    void shouldRemoveExistingBundledPluginsBeforeInitializingNewPlugins() throws Exception {
        File existingBundledPlugin = new File(directoryForUnzippedPlugins, "bundled/existing-plugin-1.jar");

        setupAgentsPluginFile().withBundledPlugin("new-plugin-1.jar", "SOME-PLUGIN-CONTENT").done();
        FileUtils.writeStringToFile(existingBundledPlugin, "OLD-CONTENT", UTF_8);

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(existingBundledPlugin.exists()).isFalse();
        assertThat(new File(directoryForUnzippedPlugins, "bundled/new-plugin-1.jar").exists()).isTrue();
    }

    @Test
    void shouldReplaceExistingBundledPluginsWithNewPluginsOfSameName() throws Exception {
        File bundledPlugin = new File(directoryForUnzippedPlugins, "bundled/plugin-1.jar");

        setupAgentsPluginFile().withBundledPlugin("plugin-1.jar", "SOME-NEW-CONTENT").done();
        FileUtils.writeStringToFile(bundledPlugin, "OLD-CONTENT", UTF_8);

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(bundledPlugin.exists()).isTrue();
        assertThat(FileUtils.readFileToString(bundledPlugin, UTF_8)).isEqualTo("SOME-NEW-CONTENT");
    }

    @Test
    void shouldRemoveExistingExternalPluginsBeforeInitializingNewPlugins() throws Exception {
        File existingExternalPlugin = new File(directoryForUnzippedPlugins, "external/existing-plugin-1.jar");

        setupAgentsPluginFile().withExternalPlugin("new-plugin-1.jar", "SOME-PLUGIN-CONTENT").done();
        FileUtils.writeStringToFile(existingExternalPlugin, "OLD-CONTENT", UTF_8);

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(existingExternalPlugin.exists()).isFalse();
        assertThat(new File(directoryForUnzippedPlugins, "external/new-plugin-1.jar").exists()).isTrue();
    }

    @Test
    void shouldReplaceExistingExternalPluginsWithNewPluginsOfSameName() throws Exception {
        File externalPlugin = new File(directoryForUnzippedPlugins, "external/plugin-1.jar");

        setupAgentsPluginFile().withExternalPlugin("plugin-1.jar", "SOME-NEW-CONTENT").done();
        FileUtils.writeStringToFile(externalPlugin, "OLD-CONTENT", UTF_8);

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(externalPlugin.exists()).isTrue();
        assertThat(FileUtils.readFileToString(externalPlugin, UTF_8)).isEqualTo("SOME-NEW-CONTENT");
    }

    @Test
    void shouldRemoveAnExistingPluginWhenItHasBeenRemovedFromTheServerSide() throws Exception {
        File existingExternalPlugin = new File(directoryForUnzippedPlugins, "external/plugin-1.jar");

        setupAgentsPluginFile().done();
        FileUtils.writeStringToFile(existingExternalPlugin, "OLD-CONTENT", UTF_8);

        agentPluginsInitializer.onApplicationEvent(null);

        assertThat(existingExternalPlugin.exists()).isFalse();
    }

    private File setupUnzippedPluginsDirectoryStructure() throws IOException {
        File dir = temporaryFolder.newFolder("unzipped-plugins");
        FileUtils.forceMkdir(new File(dir, "bundled"));
        FileUtils.forceMkdir(new File(dir, "external"));
        return dir;
    }

    private SetupOfAgentPluginsFile setupAgentsPluginFile() throws IOException {
        return new SetupOfAgentPluginsFile(AGENT_PLUGINS.getLocalFile());
    }

    private void cleanupAgentPluginsFile() throws IOException {
        FileUtils.deleteQuietly(AGENT_PLUGINS.getLocalFile());
    }

    private class SetupOfAgentPluginsFile {
        private final File bundledPluginsDir;
        private final File externalPluginsDir;
        private final ZipUtil zipUtil;
        private final File dummyFileSoZipFileIsNotEmpty;
        private File pluginsZipFile;

        SetupOfAgentPluginsFile(File pluginsZipFile) throws IOException {
            this.pluginsZipFile = pluginsZipFile;
            this.bundledPluginsDir = temporaryFolder.newFolder("bundled");
            this.externalPluginsDir = temporaryFolder.newFolder("external");
            this.dummyFileSoZipFileIsNotEmpty = temporaryFolder.newFile("dummy.txt");
            this.zipUtil = new ZipUtil();
        }

        SetupOfAgentPluginsFile withBundledPlugin(String pluginFileName, String pluginFileContent) throws IOException {
            FileUtils.writeStringToFile(new File(bundledPluginsDir, pluginFileName), pluginFileContent, UTF_8);
            return this;
        }

        SetupOfAgentPluginsFile withExternalPlugin(String pluginFileName, String pluginFileContent) throws IOException {
            FileUtils.writeStringToFile(new File(externalPluginsDir, pluginFileName), pluginFileContent, UTF_8);
            return this;
        }

        File done() throws IOException {
            ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(pluginsZipFile, true);
            zipBuilder.add("bundled", bundledPluginsDir).add("external", externalPluginsDir).add("dummy.txt", dummyFileSoZipFileIsNotEmpty).done();
            return pluginsZipFile;
        }
    }
}
