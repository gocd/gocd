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
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataLoader;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataStore;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AnalyticsPluginAssetsServiceTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    ServletContext servletContext;
    @Mock
    AnalyticsExtension extension;
    @Mock
    AnalyticsMetadataLoader analyticsMetadataLoader;
    AnalyticsPluginAssetsService assetsService;

    private File railsRoot;
    public static final String PLUGIN_ID = "plugin_id";
    private AnalyticsMetadataStore metadataStore;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        assetsService = new AnalyticsPluginAssetsService(extension, analyticsMetadataLoader);
        assetsService.setServletContext(servletContext);
        metadataStore = AnalyticsMetadataStore.instance();
    }

    @After
    public void tearDown() throws Exception {
        if (railsRoot != null && railsRoot.exists()) {
            FileUtils.deleteQuietly(railsRoot);
        }
        metadataStore.clear();
    }

    @Test
    public void shouldBeAPluginMetadataChangeListener() throws Exception {
        verify(analyticsMetadataLoader).registerListeners(assetsService);
    }

    @Test
    public void onPluginMetadataUnLoad_shouldClearExistingCacheAssets() throws Exception {
        railsRoot = temporaryFolder.newFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path path = Paths.get(pluginDirPath.toString(), "foo.txt");
        FileUtils.forceMkdirParent(path.toFile());
        Files.write(path, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(path.toFile().exists());

        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);

        assetsService.onPluginMetadataRemove(PLUGIN_ID);

        assertFalse(path.toFile().exists());
        assertFalse(pluginDirPath.toFile().exists());
    }

    @Test
    public void onPluginMetadataLoad_shouldClearExistingCacheAssets() throws Exception {
        railsRoot = temporaryFolder.newFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path dirtyPath = Paths.get(pluginDirPath.toString(), "dirty.txt");
        FileUtils.forceMkdirParent(dirtyPath.toFile());
        Files.write(dirtyPath, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(dirtyPath.toFile().exists());

        addAnalyticsPluginInfoToStore(PLUGIN_ID);
        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(null);

        assetsService.onPluginMetadataCreate(PLUGIN_ID);

        assertFalse(dirtyPath.toFile().exists());
        assertFalse(pluginDirPath.toFile().exists());
    }

    @Test
    public void onPluginMetadataLoad_shouldCachePluginStaticAssets() throws Exception {
        railsRoot = temporaryFolder.newFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path dirtyPath = Paths.get(pluginDirPath.toString(), "dirty.txt");
        FileUtils.forceMkdirParent(dirtyPath.toFile());
        Files.write(dirtyPath, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(dirtyPath.toFile().exists());

        addAnalyticsPluginInfoToStore(PLUGIN_ID);
        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(testDataZipArchive());

        assetsService.onPluginMetadataCreate(PLUGIN_ID);

        assertFalse(dirtyPath.toFile().exists());
        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(Paths.get(railsRoot.getAbsolutePath(), "public", assetsService.assetPathFor(PLUGIN_ID), "test.txt").toFile().exists());
    }

    @Test
    public void onPluginMetadataLoad_shouldUpdateThePluginInfoWithAssetsPath() throws Exception {
        railsRoot = temporaryFolder.newFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);
        GoPluginDescriptor goPluginDescriptor = new GoPluginDescriptor(PLUGIN_ID, null, null, null, null, false);
        AnalyticsPluginInfo analyticsPluginInfo = new AnalyticsPluginInfo(goPluginDescriptor, null, null, null);

        metadataStore.setPluginInfo(analyticsPluginInfo);

        Path dirtyPath = Paths.get(pluginDirPath.toString(), "dirty.txt");
        FileUtils.forceMkdirParent(dirtyPath.toFile());
        Files.write(dirtyPath, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(dirtyPath.toFile().exists());

        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(testDataZipArchive());

        assetsService.onPluginMetadataCreate(PLUGIN_ID);

        assertThat(analyticsPluginInfo.getStaticAssetsPath(), is(assetsService.assetPathFor(PLUGIN_ID)));
    }

    @Test
    public void onPluginMetadataLoad_shouldKnowPluginStaticAssetsPath() throws Exception {
        railsRoot = temporaryFolder.newFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        Path dirtyPath = Paths.get(pluginDirPath.toString(), "dirty.txt");
        FileUtils.forceMkdirParent(dirtyPath.toFile());
        Files.write(dirtyPath, "hello".getBytes());

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(dirtyPath.toFile().exists());

        addAnalyticsPluginInfoToStore(PLUGIN_ID);
        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(testDataZipArchive());

        assetsService.onPluginMetadataCreate(PLUGIN_ID);
        String shaHashOfZipFile = "6BECE2006E5F4BC8A9FCB5BD53C1E87BEE63B7DA4F3A6E24EF1AD122D47C23D3";
        assertEquals(Paths.get("assets", "plugins", PLUGIN_ID, shaHashOfZipFile).toString(), assetsService.assetPathFor(PLUGIN_ID));
    }

    private String testDataZipArchive() throws IOException {
        return new String(Base64.getEncoder().encode(IOUtils.toByteArray(getClass().getResourceAsStream("/plugin_cache_test.zip"))));
    }

    private void addAnalyticsPluginInfoToStore(String pluginId) {
        GoPluginDescriptor goPluginDescriptor = new GoPluginDescriptor(pluginId, null, null, null, null, false);
        AnalyticsPluginInfo analyticsPluginInfo = new AnalyticsPluginInfo(goPluginDescriptor, null, null, null);

        metadataStore.setPluginInfo(analyticsPluginInfo);
    }
}
