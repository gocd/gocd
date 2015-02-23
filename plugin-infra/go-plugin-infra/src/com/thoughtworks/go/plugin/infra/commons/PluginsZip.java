/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.infra.commons;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipBuilder;
import com.thoughtworks.go.util.ZipUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.FileDigester.md5DigestOfFile;

@Component
public class PluginsZip {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PluginsZip.class);
    private ZipUtil zipUtil;
    private String md5DigestOfFile;
    private final File destZipFile;
    private final File bundledPlugins;
    private final File externalPlugins;
    private Boolean pluginsEnabled;

    @Autowired
    public PluginsZip(SystemEnvironment systemEnvironment, ZipUtil zipUtil) {
        pluginsEnabled = systemEnvironment.get(SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED);
        destZipFile = new File(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH));
        bundledPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_GO_PROVIDED_PATH));
        externalPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH));
        this.zipUtil = zipUtil;
    }

    public void create() {
        if (!pluginsEnabled) {
            return;
        }
        try {
            ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(destZipFile, true);
            zipBuilder.add("bundled", bundledPlugins).add("external", externalPlugins).done();
            md5DigestOfFile = md5DigestOfFile(destZipFile);
        } catch (Exception e) {
            LOG.error("Could not create zip of plugins for agent to download.", e);
        }
    }

    public String md5() throws IOException {
        if (md5DigestOfFile == null) {
            return md5DigestOfFile(destZipFile);
        }
        return md5DigestOfFile;
    }
}
