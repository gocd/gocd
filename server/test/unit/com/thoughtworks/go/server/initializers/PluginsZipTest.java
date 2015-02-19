/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.zip.ZipFile;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginsZipTest {
    private SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED)).thenReturn(true);

        pluginsZip = new PluginsZip(systemEnvironment, new ZipUtil());
    }

    @After
    public void tearDown() throws Exception {
        TestFileUtil.cleanTempFiles();
    }

    @Test
    public void shouldZipAllPluginsIntoOneZipEveryTime() throws Exception {
        String expectedZipPath = TestFileUtil.createTempFile("go-plugins-all.zip").getAbsolutePath();
        File bundledPluginsDir = TestFileUtil.createTempFolder("plugins-bundled");
        File externalPluginsDir = TestFileUtil.createTempFolder("plugins-external");

        FileUtils.writeStringToFile(new File(bundledPluginsDir, "bundled1.jar"), "Bundled1");
        FileUtils.writeStringToFile(new File(bundledPluginsDir, "bundled2.jar"), "Bundled2");

        FileUtils.writeStringToFile(new File(externalPluginsDir, "external1.jar"), "External1");
        FileUtils.writeStringToFile(new File(externalPluginsDir, "external2.jar"), "External2");

        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginsDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(externalPluginsDir.getAbsolutePath());
        when(systemEnvironment.get(ALL_PLUGINS_ZIP_PATH)).thenReturn(expectedZipPath);

        pluginsZip.create();

        assertThat(expectedZipPath + " should exist", new File(expectedZipPath).exists(), is(true));
        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled1.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("bundled/bundled2.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external1.jar"), is(notNullValue()));
        assertThat(new ZipFile(expectedZipPath).getEntry("external/external2.jar"), is(notNullValue()));
    }
}
