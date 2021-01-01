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
package com.thoughtworks.go.server.service.plugins;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataLoader;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataStore;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AnalyticsPluginAssetsServiceTest {
    private static final String PLUGIN_ID = "plugin_id";

    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private ServletContext servletContext;
    @Mock
    private AnalyticsExtension extension;
    @Mock
    private AnalyticsMetadataLoader analyticsMetadataLoader;
    @Mock
    private SystemEnvironment systemEnvironment;

    private AnalyticsPluginAssetsService assetsService;
    private File railsRoot;
    private AnalyticsMetadataStore metadataStore;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        assetsService = new AnalyticsPluginAssetsService(extension, analyticsMetadataLoader, systemEnvironment);
        assetsService.setServletContext(servletContext);
        metadataStore = AnalyticsMetadataStore.instance();

        when(systemEnvironment.get(SystemEnvironment.GO_ANALYTICS_PLUGIN_EXTERNAL_ASSETS)).thenReturn("some-nonexistent-directory");
    }

    @After
    public void tearDown() throws Exception {
        if (railsRoot != null && railsRoot.exists()) {
            FileUtils.deleteQuietly(railsRoot);
        }
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
    public void onPluginMetadataLoad_shouldCopyPluginEndpointJsWhenCachingPluginStaticAssets() throws Exception {
        railsRoot = temporaryFolder.newFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);

        addAnalyticsPluginInfoToStore(PLUGIN_ID);
        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(testDataZipArchive());


        assetsService.onPluginMetadataCreate(PLUGIN_ID);

        Path actualPath = Paths.get(railsRoot.getAbsolutePath(), "public", assetsService.assetPathFor(PLUGIN_ID), "gocd-server-comms.js");

        assertTrue(pluginDirPath.toFile().exists());
        assertTrue(actualPath.toFile().exists());
        byte[] expected = IOUtils.toByteArray(getClass().getResourceAsStream("/plugin-endpoint.js"));
        assertArrayEquals("Content of plugin-endpoint.js should be preserved", expected, Files.readAllBytes(actualPath));
    }

    @Test
    public void onPluginMetadataLoad_shouldCopyExternalAnalyticsPluginAssetsWhenCachingPluginStaticAssets() throws Exception {
        railsRoot = temporaryFolder.newFolder();

        addAnalyticsPluginInfoToStore(PLUGIN_ID);
        when(servletContext.getInitParameter("rails.root")).thenReturn("rails-root");
        when(servletContext.getRealPath("rails-root")).thenReturn(railsRoot.getAbsolutePath());
        when(extension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(extension.getStaticAssets(PLUGIN_ID)).thenReturn(testDataZipArchive());

        File externalAssetsDir = temporaryFolder.newFolder();
        when(systemEnvironment.get(SystemEnvironment.GO_ANALYTICS_PLUGIN_EXTERNAL_ASSETS)).thenReturn(externalAssetsDir.getAbsolutePath());
        Files.write(Paths.get(externalAssetsDir.getAbsolutePath(), "a.js"), "a".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(externalAssetsDir.getAbsolutePath(), "b.js"), "b".getBytes(StandardCharsets.UTF_8));


        assetsService.onPluginMetadataCreate(PLUGIN_ID);

        Path pathForAJS = Paths.get(railsRoot.getAbsolutePath(), "public", assetsService.assetPathFor(PLUGIN_ID), "a.js");
        assertTrue(pathForAJS.toFile().exists());
        assertEquals("a", Files.readString(pathForAJS));

        Path pathForBJS = Paths.get(railsRoot.getAbsolutePath(), "public", assetsService.assetPathFor(PLUGIN_ID), "b.js");
        assertTrue(pathForBJS.toFile().exists());
        assertEquals("b", Files.readString(pathForBJS));
    }

    @Test
    public void onPluginMetadataLoad_shouldUpdateThePluginInfoWithAssetsPath() throws Exception {
        railsRoot = temporaryFolder.newFolder();
        Path pluginDirPath = Paths.get(railsRoot.getAbsolutePath(), "public", "assets", "plugins", PLUGIN_ID);
        GoPluginDescriptor goPluginDescriptor = GoPluginDescriptor.builder().id(PLUGIN_ID).build();
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
        String shaHashOfZipAndPluginScript = "cfbb9309faf81a2b61277abc3b5c31486797d62b24ddfd83a2f871fc56d61ea2";
        assertEquals(Paths.get("assets", "plugins", PLUGIN_ID, shaHashOfZipAndPluginScript).toString(), assetsService.assetPathFor(PLUGIN_ID));
    }

    private String testDataZipArchive() throws IOException {
        return new String(Base64.getEncoder().encode(IOUtils.toByteArray(getClass().getResourceAsStream("/plugin_cache_test.zip"))));
    }

    private void addAnalyticsPluginInfoToStore(String pluginId) {
        GoPluginDescriptor goPluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();
        AnalyticsPluginInfo analyticsPluginInfo = new AnalyticsPluginInfo(goPluginDescriptor, null, null, null);

        metadataStore.setPluginInfo(analyticsPluginInfo);
    }
}
