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

package com.thoughtworks.go.plugin.infra.commons;

import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.ZipFile;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class PluginsZipTest {
    private SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String expectedZipPath;
    private File externalPluginsDir;
    private PluginManager pluginManager;
    private GoPluginDescriptor bundledTaskPlugin;
    private GoPluginDescriptor bundledAuthPlugin;
    private GoPluginDescriptor bundledSCMPlugin;
    private GoPluginDescriptor externalTaskPlugin;
    private GoPluginDescriptor externalElasticAgentPlugin;
    private GoPluginDescriptor externalSCMPlugin;
    private GoPluginDescriptor bundledPackageMaterialPlugin;
    private GoPluginDescriptor externalPackageMaterialPlugin;

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        temporaryFolder.create();
        systemEnvironment = mock(SystemEnvironment.class);
        File bundledPluginsDir = temporaryFolder.newFolder("plugins-bundled");
        expectedZipPath = temporaryFolder.newFile("go-plugins-all.zip").getAbsolutePath();
        externalPluginsDir = temporaryFolder.newFolder("plugins-external");

        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());
        when(systemEnvironment.get(ALL_PLUGINS_ZIP_PATH)).thenReturn(expectedZipPath);

        pluginsZip = spy(new PluginsZip(systemEnvironment, pluginManager));

        File bundledTask1Jar = new File(bundledPluginsDir, "bundled-task-1.jar");
        FileUtils.writeStringToFile(bundledTask1Jar, "Bundled1");
        File bundledAuth2Jar = new File(bundledPluginsDir, "bundled-auth-2.jar");
        FileUtils.writeStringToFile(bundledAuth2Jar, "Bundled2");
        File bundledscm3Jar = new File(bundledPluginsDir, "bundled-scm-3.jar");
        FileUtils.writeStringToFile(bundledscm3Jar, "Bundled3");
        File bundledPackageMaterialJar = new File(bundledPluginsDir, "bundled-package-material-4.jar");
        FileUtils.writeStringToFile(bundledPackageMaterialJar, "Bundled4");

        File externalTask1Jar = new File(externalPluginsDir, "external-task-1.jar");
        FileUtils.writeStringToFile(externalTask1Jar, "External1");
        File externalElastic1Jar = new File(externalPluginsDir, "external-elastic-agent-2.jar");
        FileUtils.writeStringToFile(externalElastic1Jar, "External2");
        File externalscm3Jar = new File(externalPluginsDir, "external-scm-3.jar");
        FileUtils.writeStringToFile(externalscm3Jar, "External3");
        File externalPackageMaterialJar = new File(externalPluginsDir, "external-package-material-4.jar");
        FileUtils.writeStringToFile(externalPackageMaterialJar, "External3");

        bundledTaskPlugin = new GoPluginDescriptor("bundled-task-1", "1.0", null, bundledTask1Jar.getAbsolutePath(), null, true);
        bundledAuthPlugin = new GoPluginDescriptor("bundled-auth-2", "1.0", null, bundledAuth2Jar.getAbsolutePath(), null, true);
        bundledSCMPlugin = new GoPluginDescriptor("bundled-scm-3", "1.0", null, bundledscm3Jar.getAbsolutePath(), null, true);
        bundledPackageMaterialPlugin = new GoPluginDescriptor("bundled-package-material-4", "1.0", null, bundledPackageMaterialJar.getAbsolutePath(), null, true);


        externalTaskPlugin = new GoPluginDescriptor("external-task-1", "1.0", null, externalTask1Jar.getAbsolutePath(), null, false);
        externalElasticAgentPlugin = new GoPluginDescriptor("external-elastic-agent-2", "1.0", null, externalElastic1Jar.getAbsolutePath(), null, false);
        externalSCMPlugin = new GoPluginDescriptor("external-scm-3", "1.0", null, externalscm3Jar.getAbsolutePath(), null, false);
        externalPackageMaterialPlugin = new GoPluginDescriptor("external-package-material-4", "1.0", null, externalPackageMaterialJar.getAbsolutePath(), null, false);

        when(pluginManager.plugins()).thenReturn(Arrays.asList(
                bundledTaskPlugin,
                bundledAuthPlugin,
                bundledSCMPlugin,
                bundledPackageMaterialPlugin,
                externalTaskPlugin,
                externalElasticAgentPlugin,
                externalSCMPlugin,
                externalPackageMaterialPlugin
        ));

        when(pluginManager.isPluginOfType("task", "bundled-task-1")).thenReturn(true);
        when(pluginManager.isPluginOfType("task", "external-task-1")).thenReturn(true);
        when(pluginManager.isPluginOfType("package-repository", "bundled-package-material-4")).thenReturn(true);
        when(pluginManager.isPluginOfType("scm", "bundled-scm-3")).thenReturn(true);
        when(pluginManager.isPluginOfType("scm", "external-scm-3")).thenReturn(true);
        when(pluginManager.isPluginOfType("package-repository", "external-package-material-4")).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        temporaryFolder.delete();
    }

    @Test
    public void shouldZipTaskPluginsIntoOneZipEveryTime() throws Exception {
        pluginsZip.create();

        assertThat(expectedZipPath + " should exist", new File(expectedZipPath).exists(), is(true));
        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled-task-1.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled-scm-3.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled-package-material-4.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external-task-1.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external-scm-3.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external-package-material-4.jar"), is(notNullValue()));

        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled-auth-2.jar"), is(nullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external-elastic-agent-2.jar"), is(nullValue()));
    }

    @Test
    public void shouldGetChecksumIfFileWasCreated() throws Exception {
        pluginsZip.create();
        String md5 = pluginsZip.md5();
        assertThat(md5, is(notNullValue()));
    }

    @Test
    public void shouldUpdateChecksumIfFileIsReCreated() throws Exception {
        pluginsZip.create();
        String oldMd5 = pluginsZip.md5();
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external-task-1.jar"), UUID.randomUUID().toString());
        pluginsZip.create();
        assertThat(pluginsZip.md5(), is(not(oldMd5)));
    }

    @Test(expected = FileAccessRightsCheckException.class)
    public void shouldFailGracefullyWhenExternalFileCannotBeRead() throws Exception {
        File bundledPluginsDir = temporaryFolder.newFolder("plugins-bundled-ext");
        SystemEnvironment systemEnvironmentFail = mock(SystemEnvironment.class);
        when(systemEnvironmentFail.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironmentFail.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn("");
        when(systemEnvironmentFail.get(ALL_PLUGINS_ZIP_PATH)).thenReturn("");
        FileUtils.writeStringToFile(new File(bundledPluginsDir, "bundled-task-1.jar"), "Bundled1");

        PluginsZip pluginsZipFail = new PluginsZip(systemEnvironmentFail, pluginManager);
        pluginsZipFail.create();
    }

    @Test(expected = FileAccessRightsCheckException.class)
    public void shouldFailGracefullyWhenBundledFileCannotBeRead() throws Exception {
        SystemEnvironment systemEnvironmentFail = mock(SystemEnvironment.class);
        when(systemEnvironmentFail.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn("");
        when(systemEnvironmentFail.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());
        when(systemEnvironmentFail.get(ALL_PLUGINS_ZIP_PATH)).thenReturn("");
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external-task-1.jar"), "External1");

        PluginsZip pluginsZipFail = new PluginsZip(systemEnvironmentFail, pluginManager);
        pluginsZipFail.create();
    }

    @Test
    public void fileAccessErrorShouldContainPathToTheFolderInWhichTheErrorOccurred() throws Exception {
        SystemEnvironment systemEnvironmentFail = mock(SystemEnvironment.class);
        when(systemEnvironmentFail.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn("/dummy");
        when(systemEnvironmentFail.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());
        when(systemEnvironmentFail.get(ALL_PLUGINS_ZIP_PATH)).thenReturn("");
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external-task-1.jar"), "External1");
        expectedException.expect(FileAccessRightsCheckException.class);
        expectedException.expectMessage("dummy");

        PluginsZip pluginsZipFail = new PluginsZip(systemEnvironmentFail, pluginManager);
        pluginsZipFail.create();
    }


    @Test
    public void shouldCreatePluginsWhenTaskPluginsAreAdded() throws Exception {
        GoPluginDescriptor plugin = new GoPluginDescriptor("curl-task-plugin", null, null, null, null, false);
        when(pluginManager.isPluginOfType("task", plugin.id())).thenReturn(true);
        pluginsZip.pluginLoaded(plugin);
        verify(pluginsZip, times(1)).create();
    }

    @Test
    public void shouldCreatePluginsWhenTaskPluginsAreRemoved() throws Exception {
        pluginsZip.pluginUnLoaded(externalTaskPlugin);
        verify(pluginsZip, times(1)).create();
    }

    @Test
    public void shouldDoNothingWhenAPluginThatIsNotATaskOrScmOrPackageMaterialPluginPluginIsAdded() throws Exception {
        pluginsZip.pluginLoaded(externalElasticAgentPlugin);
        verify(pluginsZip, never()).create();
    }

    @Test
    public void shouldDoNothingWhenAPluginThatIsNotATaskOrScmOrPackageMaterialPluginPluginIsRemoved() throws Exception {
        pluginsZip.pluginUnLoaded(externalElasticAgentPlugin);
        verify(pluginsZip, never()).create();
    }
}

