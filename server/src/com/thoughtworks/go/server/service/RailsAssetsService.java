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

package com.thoughtworks.go.server.service;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

@Service
public class RailsAssetsService {
    private static final Pattern MANIFEST_FILE_PATTERN = Pattern.compile("^manifest.*\\.json$");
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(RailsAssetsService.class);
    private RailsAssetsManifest railsAssetsManifest;
    private final ServletContext servletContext;
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public RailsAssetsService(ServletContext servletContext, SystemEnvironment systemEnvironment) {
        this.servletContext = servletContext;
        this.systemEnvironment = systemEnvironment;
    }

    public void initialize() throws IOException {
        if (!(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS) && systemEnvironment.useCompressedJs())) {
            return;
        }
        String assetsDirPath = servletContext.getRealPath(servletContext.getInitParameter("rails.root") + "/public/assets/");
        File assetsDir = new File(assetsDirPath);
        if (!assetsDir.exists()) {
            throw new RuntimeException(String.format("Assets directory does not exist %s", assetsDirPath));
        }
        File manifestFile = null;
        Iterator iterator = FileUtils.iterateFiles(assetsDir, new String[]{"json"}, false);
        while (iterator.hasNext()) {
            File next = (File) iterator.next();
            if (MANIFEST_FILE_PATTERN.matcher(next.getName()).matches()) {
                manifestFile = next;
                break;
            }
        }
        if (manifestFile == null) {
            throw new RuntimeException(String.format("Manifest json file was not found at %s", assetsDirPath));
        }

        LOG.info(String.format("Found rails assets manifest file named %s ", manifestFile.getName()));
        String manifest = FileUtil.readContentFromFile(manifestFile);
        Gson gson = new Gson();
        railsAssetsManifest = gson.fromJson(manifest, RailsAssetsManifest.class);
        LOG.info(String.format("Successfully read rails assets manifest file located at %s", manifestFile.getAbsolutePath()));
    }

    public String getAssetPath(String asset) {
        if (!systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)) {
            throw new RuntimeException("This service does not cater to Rails2");
        }
        String assetFileName = systemEnvironment.useCompressedJs() ? railsAssetsManifest.getAssetWithDigest(asset) : asset;
        return StringUtil.isBlank(assetFileName) ? null : String.format("assets/%s", assetFileName);
    }
    class RailsAssetsManifest {
        @SerializedName("assets")
        private HashMap<String, String> assets = new HashMap<String, String>();

        public String getAssetWithDigest(String name) {
            return assets.get(name);
        }
    }
}