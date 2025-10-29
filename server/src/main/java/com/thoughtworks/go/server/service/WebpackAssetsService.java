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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.json.JsonHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class WebpackAssetsService implements ServletContextAware {

    private final SystemEnvironment systemEnvironment;
    private final AtomicReference<WebpackManifest> cachedManifest = new AtomicReference<>(null);
    private ServletContext servletContext;

    @Autowired
    public WebpackAssetsService(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public Set<String> getJSAssetPathsFor(String... assetNames) {
        return Arrays.stream(assetNames)
            .flatMap(assetName -> getManifest().entrypointFor(assetName).assets().js().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getCSSAssetPathsFor(String... assetNames) {
        return Arrays.stream(assetNames)
            .flatMap(assetName -> getManifest().entrypointFor(assetName).assets().css().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private WebpackManifest getManifest() {
        if (systemEnvironment.useCompressedJs()) {
            return this.cachedManifest.updateAndGet(existing -> existing == null ? loadManifest() : existing);
        } else {
            return loadManifest();
        }
    }

    @NotNull WebpackManifest loadManifest() {
        File manifestFile = new File(servletContext.getRealPath(servletContext.getInitParameter("rails.root") + "/public/assets/webpack/manifest.json"));

        if (!manifestFile.exists()) {
            throw new RuntimeException("Could not load compiled manifest from 'webpack/manifest.json' - have you run `./gradlew prepare` OR `./gradlew compileAssetsWebpackDev` since last clean?");
        }

        try {
            return JsonHelper
                .fromJsonExposeOnly(Files.readString(manifestFile.toPath(), UTF_8), WebpackManifest.class)
                .validated();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private record WebpackManifest(@Expose Map<String, WebpackEntrypoint> entrypoints) {
        WebpackManifest validated() {
            if (entrypoints == null || entrypoints.isEmpty()) {
                throw new RuntimeException("Could not find any entrypoints in the manifest.json file.");
            }
            return this;
        }

        public @NotNull WebpackEntrypoint entrypointFor(String assetName) {
            return entrypoints.computeIfAbsent(assetName, k -> { throw new RuntimeException(String.format("Can't find entry point '%s' in webpack manifest", assetName)); });
        }
    }

    private record WebpackEntrypoint(@Expose WebpackAssets assets) {}

    private record WebpackAssets(
        @Expose List<String> css,
        @Expose @SerializedName("css.map") List<String> cssMap,
        @Expose List<String> js,
        @Expose @SerializedName("js.map") List<String> jsMap) {
        
        @Override
        public List<String> css() {
            return css == null ? Collections.emptyList() : css;
        }

        @Override
        public List<String> cssMap() {
            return cssMap == null ? Collections.emptyList() : cssMap;
        }

        @Override
        public List<String> js() {
            return js == null ? Collections.emptyList() : js;
        }

        @Override
        public List<String> jsMap() {
            return jsMap == null ? Collections.emptyList() : jsMap;
        }
    }
}
