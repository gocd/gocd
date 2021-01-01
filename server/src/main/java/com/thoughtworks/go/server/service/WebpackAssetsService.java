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
package com.thoughtworks.go.server.service;

import com.google.gson.Gson;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class WebpackAssetsService implements ServletContextAware {

    private final SystemEnvironment systemEnvironment;
    private ServletContext servletContext;
    private Map manifest;

    @Autowired
    public WebpackAssetsService(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public List<String> getAssetPaths(String assetName) throws IOException {
        Map entrypoints = getManifest();

        Map entrypointForAsset = (Map) entrypoints.get(assetName);
        if (entrypointForAsset == null) {
            throw new RuntimeException(String.format("Can't find entry point '%s' in webpack manifest", assetName));
        }

        List<String> assets = (List<String>) entrypointForAsset.get("assets");

        List<String> result = new ArrayList<>();
        for (String asset : assets) {
            String format = String.format("/go/assets/webpack/%s", asset);
            if (!format.endsWith(".map")) {
                result.add(format);
            }
        }

        return result;
    }

    public Set<String> getAssetPathsFor(String... assetNames) throws IOException {
        Set<String> result = new LinkedHashSet<>();

        for (String asset : assetNames) {
            result.addAll(getAssetPaths(asset));
        }

        return result;
    }

    public Set<String> getJSAssetPathsFor(String... assetNames) throws IOException {
        return getAssetPathsFor(assetNames).stream()
                .filter(assetName -> assetName.endsWith(".js"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getCSSAssetPathsFor(String... assetNames) throws IOException {
        return getAssetPathsFor(assetNames).stream()
                .filter(assetName -> assetName.endsWith(".css"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map getManifest() throws IOException {
        if (systemEnvironment.useCompressedJs()) {
            if (this.manifest == null) {
                this.manifest = loadManifest();
            }
            return this.manifest;
        } else {
            return loadManifest();
        }
    }

    Map loadManifest() throws IOException {
        File manifestFile = new File(servletContext.getRealPath(servletContext.getInitParameter("rails.root") + "/public/assets/webpack/manifest.json"));

        if (!manifestFile.exists()) {
            throw new RuntimeException("Could not load compiled manifest from 'webpack/manifest.json' - have you run `rake webpack:compile`?");
        }

        Gson gson = new Gson();
        Map manifest = gson.fromJson(FileUtils.readFileToString(manifestFile, UTF_8), Map.class);

        if (manifest.containsKey("errors") && !((List) manifest.get("errors")).isEmpty()) {
            throw new RuntimeException("There were errors in manifest.json file");
        }

        Map entrypoints = (Map) manifest.get("entrypoints");
        if (entrypoints == null) {
            throw new RuntimeException("Could not find any entrypoints in the manifest.json file.");
        }
        return entrypoints;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
