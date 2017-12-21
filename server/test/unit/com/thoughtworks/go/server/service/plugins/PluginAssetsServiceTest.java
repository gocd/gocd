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

package com.thoughtworks.go.server.service.plugins;

import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginAssetsServiceTest {

    public static final String PLUGIN_ID = "plugin_id";
    @Mock
    ServletContext servletContext;
    @Mock
    AnalyticsExtension extension;

    @Mock
    GoPluginDescriptor pluginDescriptor;

    PluginAssetsService pluginAssetsService;
    private File railsRoot;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        pluginAssetsService = new PluginAssetsService(extension);
        pluginAssetsService.setServletContext(servletContext);
    }

    @After
    public void tearDown() throws Exception {
        if (railsRoot != null && railsRoot.exists()) {
            FileUtils.deleteQuietly(railsRoot);
        }
    }

    @Test
    public void onPluginUnLoad_shouldClearExistingCacheAssets() throws Exception {
        railsRoot = FileUtil.createTempFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path path = Paths.get(pluginDirPath.toString(), "foo.txt");
        FileUtil.createParentFolderIfNotExist(path.toFile());
        Files.write(path, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(path.toFile().exists());

        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);

        pluginAssetsService.pluginUnLoaded(pluginDescriptor);

        assertFalse(path.toFile().exists());
        assertFalse(pluginDirPath.toFile().exists());
    }

    @Test
    public void onPluginLoad_shouldClearExistingCacheAssets() throws Exception {
        railsRoot = FileUtil.createTempFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path dirtyPath = Paths.get(pluginDirPath.toString(), "dirty.txt");
        FileUtil.createParentFolderIfNotExist(dirtyPath.toFile());
        Files.write(dirtyPath, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(dirtyPath.toFile().exists());

        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(null);

        pluginAssetsService.pluginLoaded(pluginDescriptor);

        assertFalse(dirtyPath.toFile().exists());
        assertFalse(pluginDirPath.toFile().exists());
    }

    @Test
    public void onPluginLoad_shouldCachePluginStaticAssets() throws Exception {
        railsRoot = FileUtil.createTempFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path dirtyPath = Paths.get(pluginDirPath.toString(), "dirty.txt");
        FileUtil.createParentFolderIfNotExist(dirtyPath.toFile());
        Files.write(dirtyPath, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(dirtyPath.toFile().exists());

        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(testDataZipArchive());

        pluginAssetsService.pluginLoaded(pluginDescriptor);

        assertFalse(dirtyPath.toFile().exists());
        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(Paths.get(pluginAssetsService.assetPathFor(PLUGIN_ID), "test.txt").toFile().exists());
    }

    @Test
    public void onPluginLoad_shouldKnowPluginStaticAssetsPath() throws Exception {
        railsRoot = FileUtil.createTempFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path dirtyPath = Paths.get(pluginDirPath.toString(), "dirty.txt");
        FileUtil.createParentFolderIfNotExist(dirtyPath.toFile());
        Files.write(dirtyPath, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(dirtyPath.toFile().exists());

        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(testDataZipArchive());

        pluginAssetsService.pluginLoaded(pluginDescriptor);
        String shaHashOfZipFile = "6BECE2006E5F4BC8A9FCB5BD53C1E87BEE63B7DA4F3A6E24EF1AD122D47C23D3";
        assertEquals(Paths.get(pluginDirPath.toString(), shaHashOfZipFile).toString(), pluginAssetsService.assetPathFor(PLUGIN_ID));
    }

    private String testDataZipArchive() throws IOException {
        return new String(Base64.getEncoder().encode(IOUtils.toByteArray(getClass().getResourceAsStream("/plugin_cache_test.zip"))));
    }
}
