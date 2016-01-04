/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.zip.ZipFile;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginsZipTest {
    private SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String expectedZipPath;
    private File externalPluginsDir;


    @Before
    public void setUp() throws Exception {
        temporaryFolder.create();
        systemEnvironment = mock(SystemEnvironment.class);
        File bundledPluginsDir = temporaryFolder.newFolder("plugins-bundled");
        expectedZipPath = temporaryFolder.newFile("go-plugins-all.zip").getAbsolutePath();
        externalPluginsDir = temporaryFolder.newFolder("plugins-external");

        when(systemEnvironment.get(SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED)).thenReturn(true);
        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());
        when(systemEnvironment.get(ALL_PLUGINS_ZIP_PATH)).thenReturn(expectedZipPath);

        pluginsZip = new PluginsZip(systemEnvironment, new ZipUtil());

        FileUtils.writeStringToFile(new File(bundledPluginsDir, "bundled1.jar"), "Bundled1");
        FileUtils.writeStringToFile(new File(bundledPluginsDir, "bundled2.jar"), "Bundled2");

        FileUtils.writeStringToFile(new File(externalPluginsDir, "external1.jar"), "External1");
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external2.jar"), "External2");
    }

    @After
    public void tearDown() throws Exception {
        temporaryFolder.delete();
    }

    @Test
    public void shouldZipAllPluginsIntoOneZipEveryTime() throws Exception {
        pluginsZip.create();

        assertThat(expectedZipPath + " should exist", new File(expectedZipPath).exists(), is(true));
        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled1.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled2.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external1.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external2.jar"), is(notNullValue()));
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
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external3.jar"), "External3");
        pluginsZip.create();
        assertThat(pluginsZip.md5(), is(not(oldMd5)));
    }

    @Test
    public void shouldGetChecksumOfExistingFile() throws Exception {
        String md5 = pluginsZip.md5();
        assertThat(md5, is(notNullValue()));
    }

    @Test
    public void shouldThrowExceptionWhileRetrievingChecksumOfUnavailableFile() throws Exception {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(containsString("Could not compute md5 of plugins. Exception occurred:"));

        temporaryFolder.delete();
        pluginsZip.md5();
    }

    @Test(expected = FileAccessRightsCheckException.class)
    public void shouldFailGracefullyWhenExternalFileCannotBeRead() throws Exception {
        File bundledPluginsDir = temporaryFolder.newFolder("plugins-bundled-ext");
        SystemEnvironment systemEnvironmentFail = mock(SystemEnvironment.class);
        when(systemEnvironmentFail.get(SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED)).thenReturn(true);
        when(systemEnvironmentFail.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironmentFail.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn("");
        when(systemEnvironmentFail.get(ALL_PLUGINS_ZIP_PATH)).thenReturn("");
        FileUtils.writeStringToFile(new File(bundledPluginsDir, "bundled1.jar"), "Bundled1");

        PluginsZip pluginsZipFail = new PluginsZip(systemEnvironmentFail, new ZipUtil());
        pluginsZipFail.create();
    }

    @Test(expected = FileAccessRightsCheckException.class)
    public void shouldFailGracefullyWhenBundledFileCannotBeRead() throws Exception {
        SystemEnvironment systemEnvironmentFail = mock(SystemEnvironment.class);
        when(systemEnvironmentFail.get(SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED)).thenReturn(true);
        when(systemEnvironmentFail.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn("");
        when(systemEnvironmentFail.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());
        when(systemEnvironmentFail.get(ALL_PLUGINS_ZIP_PATH)).thenReturn("");
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external1.jar"), "External1");

        PluginsZip pluginsZipFail = new PluginsZip(systemEnvironmentFail, new ZipUtil());
        pluginsZipFail.create();
    }

    @Test
    public void fileAccessErrorShouldContainPathToTheFolderInWhichTheErrorOccurred() throws Exception {
        SystemEnvironment systemEnvironmentFail = mock(SystemEnvironment.class);
        when(systemEnvironmentFail.get(SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED)).thenReturn(true);
        when(systemEnvironmentFail.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn("/dummy");
        when(systemEnvironmentFail.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());
        when(systemEnvironmentFail.get(ALL_PLUGINS_ZIP_PATH)).thenReturn("");
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external1.jar"), "External1");
        expectedException.expect(FileAccessRightsCheckException.class);
        expectedException.expectMessage("dummy");

        PluginsZip pluginsZipFail = new PluginsZip(systemEnvironmentFail, new ZipUtil());
        pluginsZipFail.create();

    }
}

