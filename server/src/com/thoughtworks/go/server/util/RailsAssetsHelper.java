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

package com.thoughtworks.go.server.util;

import com.google.gson.Gson;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.mortbay.jetty.handler.ContextHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

public class RailsAssetsHelper {
    private static Pattern MANIFEST_FILE_PATTERN = Pattern.compile("^manifest.*\\.json$");
    private RailsAssetsManifest railsAssetsManifest;

    public RailsAssetsHelper(ContextHandler.SContext context) throws IOException {
        String assetsDirPath = context.getRealPath(context.getInitParameter("rails.root") + "/public/assets/");
        File assetsDir = new File(assetsDirPath);
        if(!assetsDir.exists()) throw new RuntimeException(String.format("Assets directory does not exist %s", assetsDirPath));
        File manifestFile = null;
        Iterator iterator = FileUtils.iterateFiles(assetsDir, new String[]{"json"}, false);
        while (iterator.hasNext()) {
            File next = (File) iterator.next();
            if (MANIFEST_FILE_PATTERN.matcher(next.getName()).matches()) {
                manifestFile = next;
                break;
            }
        }
        if(manifestFile == null) throw new RuntimeException(String.format("Manifest json file was not found at %s", assetsDirPath));
        String manifest = FileUtil.readContentFromFile(manifestFile);
        Gson gson = new Gson();
        railsAssetsManifest = gson.fromJson(manifest, RailsAssetsManifest.class);
    }

    public String getAssetPath(String asset) {
        String assetWithDigest = railsAssetsManifest.getAssetWithDigest(asset);
        return StringUtil.isBlank(assetWithDigest) ? null : String.format("assets/%s", assetWithDigest);
    }

    class RailsAssetsManifest {
        private HashMap<String, String> assets = new HashMap<String, String>();

        public String getAssetWithDigest(String name) {
            return assets.get(name);
        }
    }
}
