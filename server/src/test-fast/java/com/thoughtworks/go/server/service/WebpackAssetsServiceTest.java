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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class WebpackAssetsServiceTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private File assetsDir;
    private WebpackAssetsService webpackAssetsService;
    @Mock
    private ServletContext context;
    @Mock
    private SystemEnvironment systemEnvironment;
    private File manifestFile;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        assetsDir = tempFolder.newFolder("assets-" + UUID.randomUUID().toString());
        manifestFile = new File(assetsDir, "public/assets/webpack/manifest.json");
        manifestFile.getParentFile().mkdirs();
        webpackAssetsService = spy(new WebpackAssetsService(systemEnvironment));
        webpackAssetsService.setServletContext(context);

        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath("/public/assets/webpack/manifest.json")).thenReturn(manifestFile.getAbsolutePath());
    }

    @Test
    public void shouldGetAssetPathFromManifestJson() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        List<String> assetPaths = webpackAssetsService.getAssetPaths("single_page_apps/agents");
        assertThat(assetPaths, hasItems("/go/assets/webpack/vendor-and-helpers.chunk.js", "/go/assets/webpack/single_page_apps/agents.js"));
    }

    @Test
    public void shouldGetAllAssetPathsFromManifestJson() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        Set<String> assetPaths = webpackAssetsService.getAssetPathsFor("single_page_apps/agents", "single_page_apps/new_dashboard");
        assertThat(assetPaths.size(), is(5));
        assertThat(assetPaths, hasItems("/go/assets/webpack/vendor-and-helpers.chunk.js",
                "/go/assets/webpack/single_page_apps/agents.js",
                "/go/assets/webpack/single_page_apps/agents.css",
                "/go/assets/webpack/single_page_apps/new_dashboard.js",
                "/go/assets/webpack/single_page_apps/new_dashboard.css"));
    }

    @Test
    public void shouldGetJSAssetPathsFromManifestJson() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        Set<String> assetPaths = webpackAssetsService.getJSAssetPathsFor("single_page_apps/agents", "single_page_apps/new_dashboard");
        assertThat(assetPaths.size(), is(3));
        assertThat(assetPaths, hasItems("/go/assets/webpack/vendor-and-helpers.chunk.js",
                "/go/assets/webpack/single_page_apps/agents.js",
                "/go/assets/webpack/single_page_apps/new_dashboard.js"));
    }

    @Test
    public void shouldGetCSSAssetPathsFromManifestJson() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        Set<String> assetPaths = webpackAssetsService.getCSSAssetPathsFor("single_page_apps/agents", "single_page_apps/new_dashboard");
        assertThat(assetPaths.size(), is(2));
        assertThat(assetPaths, hasItems("/go/assets/webpack/single_page_apps/agents.css",
                "/go/assets/webpack/single_page_apps/new_dashboard.css"));
    }

    @Test
    public void shouldBlowUpIfAssetPathIsNotFound() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Can't find entry point 'junk' in webpack manifest");

        webpackAssetsService.getAssetPaths("junk");
    }

    @Test
    public void shouldBlowUpIfManifestDoesNotContainEntrypoints() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/manifest-without-entrypoints.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Could not find any entrypoints in the manifest.json file.");

        webpackAssetsService.getAssetPaths("junk");
    }

    @Test
    public void shouldBlowUpWhenManifestHasErrors() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/manifest-with-errors.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("There were errors in manifest.json file");

        webpackAssetsService.getAssetPaths("anything");
    }

    @Test
    public void shouldBlowUpIfManifestIsNotFound() throws IOException {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Could not load compiled manifest from 'webpack/manifest.json' - have you run `rake webpack:compile`?");
        webpackAssetsService.getAssetPaths("junk");
    }

    @Test
    public void shouldServeManifestFromCacheInProductionMode() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json")) {
            FileUtils.copyInputStreamToFile(is, manifestFile);
        }

        when(systemEnvironment.useCompressedJs()).thenReturn(true);

        webpackAssetsService.getAssetPaths("single_page_apps/agents");
        webpackAssetsService.getAssetPaths("single_page_apps/agents");
        verify(webpackAssetsService, times(1)).loadManifest();
    }
}


