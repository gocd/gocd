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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class RailsAssetsService implements ServletContextAware {
    private static final Pattern MANIFEST_FILE_PATTERN = Pattern.compile("^\\.sprockets-manifest.*\\.json$");
    private static final Logger LOG = LoggerFactory.getLogger(RailsAssetsService.class);
    private RailsAssetsManifest railsAssetsManifest;
    private ServletContext servletContext;
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public RailsAssetsService(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public void initialize() throws IOException {
        if (!systemEnvironment.useCompressedJs()) {
            return;
        }
        String assetsDirPath = servletContext.getRealPath(servletContext.getInitParameter("rails.root") + "/public/assets/");
        File assetsDir = new File(assetsDirPath);
        if (!assetsDir.exists()) {
            throw new RuntimeException(String.format("Assets directory does not exist %s", assetsDirPath));
        }
        Collection files = FileUtils.listFiles(assetsDir, new RegexFileFilter(MANIFEST_FILE_PATTERN), null);
        if (files.isEmpty()) {
            throw new RuntimeException(String.format("Manifest json file was not found at %s", assetsDirPath));
        }

        File manifestFile = (File) files.iterator().next();

        LOG.info("Found rails assets manifest file named {} ", manifestFile.getName());
        String manifest = FileUtils.readFileToString(manifestFile, UTF_8);
        Gson gson = new Gson();
        railsAssetsManifest = gson.fromJson(manifest, RailsAssetsManifest.class);
        LOG.info("Successfully read rails assets manifest file located at {}", manifestFile.getAbsolutePath());
    }

    public String getAssetPath(String asset) {
        String assetFileName = systemEnvironment.useCompressedJs() ? railsAssetsManifest.getAssetWithDigest(asset) : asset;
        return StringUtils.isBlank(assetFileName) ? null : String.format("assets/%s", assetFileName);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    RailsAssetsManifest getRailsAssetsManifest() {
        return railsAssetsManifest;
    }

    class RailsAssetsManifest {
        @SerializedName("assets")
        private HashMap<String, String> assets = new HashMap<>();

        public String getAssetWithDigest(String name) {
            return assets.get(name);
        }
    }
}
