/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebpackAssetsServiceTest {

    @Mock
    private ServletContext context;
    @Mock
    private SystemEnvironment systemEnvironment;

    private WebpackAssetsService webpackAssetsService;
    private Path manifestFile;


    @BeforeEach
    public void setUp(@TempDir Path assetsDir) {
        manifestFile = assetsDir.resolve("public/assets/webpack/manifest.json");
        manifestFile.getParent().toFile().mkdirs();
        webpackAssetsService = spy(new WebpackAssetsService(systemEnvironment));
        webpackAssetsService.setServletContext(context);

        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath("/public/assets/webpack/manifest.json")).thenReturn(manifestFile.toAbsolutePath().toString());
    }

    @Test
    public void shouldGetAssetPathFromManifestJson() throws IOException {
        TestFileUtil.resourceToPath("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json", manifestFile);

        Set<String> assetPaths = webpackAssetsService.getJSAssetPathsFor("single_page_apps/agents");
        assertThat(assetPaths).contains("/go/assets/webpack/vendor-and-helpers.chunk.js", "/go/assets/webpack/single_page_apps/agents.js");
    }

    @Test
    public void shouldGetJSAssetPathsFromManifestJson() throws IOException {
        TestFileUtil.resourceToPath("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json", manifestFile);

        Set<String> assetPaths = webpackAssetsService.getJSAssetPathsFor("single_page_apps/agents", "single_page_apps/new_dashboard");
        assertThat(assetPaths.size()).isEqualTo(3);
        assertThat(assetPaths).contains("/go/assets/webpack/vendor-and-helpers.chunk.js",
                "/go/assets/webpack/single_page_apps/agents.js",
                "/go/assets/webpack/single_page_apps/new_dashboard.js");
    }

    @Test
    public void shouldGetCSSAssetPathsFromManifestJson() throws IOException {
        TestFileUtil.resourceToPath("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json", manifestFile);

        Set<String> assetPaths = webpackAssetsService.getCSSAssetPathsFor("single_page_apps/agents", "single_page_apps/new_dashboard");
        assertThat(assetPaths.size()).isEqualTo(2);
        assertThat(assetPaths).contains("/go/assets/webpack/single_page_apps/agents.css",
                "/go/assets/webpack/single_page_apps/new_dashboard.css");
    }

    @Test
    public void shouldBlowUpIfAssetPathIsNotFound() throws IOException {
        TestFileUtil.resourceToPath("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json", manifestFile);

        assertThatThrownBy(() -> webpackAssetsService.getJSAssetPathsFor("junk"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Can't find entry point 'junk' in webpack manifest");
    }

    @Test
    public void shouldBlowUpIfManifestDoesNotContainEntrypoints() throws IOException {
        TestFileUtil.resourceToPath("/com/thoughtworks/go/server/service/webpackassetstest/manifest-without-entrypoints.json", manifestFile);

        assertThatThrownBy(() -> webpackAssetsService.getJSAssetPathsFor("junk"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Could not find any entrypoints in the manifest.json file.");
    }

    @Test
    public void shouldBlowUpIfManifestIsNotFound() {
        assertThatThrownBy(() -> webpackAssetsService.getJSAssetPathsFor("junk"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not load compiled manifest from 'webpack/manifest.json' - have you run");
    }

    @Test
    public void shouldServeManifestFromCacheInProductionMode() throws IOException {
        TestFileUtil.resourceToPath("/com/thoughtworks/go/server/service/webpackassetstest/good-manifest.json", manifestFile);

        when(systemEnvironment.useCompressedJs()).thenReturn(true);

        webpackAssetsService.getJSAssetPathsFor("single_page_apps/agents");
        webpackAssetsService.getJSAssetPathsFor("single_page_apps/agents");
        verify(webpackAssetsService, times(1)).loadManifest();
    }
}


