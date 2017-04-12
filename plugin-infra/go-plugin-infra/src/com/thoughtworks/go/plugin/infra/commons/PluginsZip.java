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
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.thoughtworks.go.util.FileDigester.md5DigestOfFolderContent;

@Component
public class PluginsZip {
    private static final Logger LOG = LoggerFactory.getLogger(PluginsZip.class);
    private ZipUtil zipUtil;
    private String md5DigestOfPlugins;
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
        checkFilesAccessibility(bundledPlugins, externalPlugins);
        try {
            ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(destZipFile, true);
            zipBuilder.add("bundled", bundledPlugins).add("external", externalPlugins).done();
            md5DigestOfPlugins = computeMd5DigestOfPlugins();
        } catch (Exception e) {
            LOG.error("Could not create zip of plugins for agent to download.", e);
        }
    }

    public String md5() {
        if (md5DigestOfPlugins == null) {
            return computeMd5DigestOfPlugins();
        }
        return md5DigestOfPlugins;
    }

    private String computeMd5DigestOfPlugins() {
        try {
            String digestOfBundledFolder = md5DigestOfFolderContent(bundledPlugins);
            String digestOfExternalFolder = md5DigestOfFolderContent(externalPlugins);
            String digestOfPlugins = digestOfBundledFolder + digestOfExternalFolder;
            return DigestUtils.md5Hex(digestOfPlugins);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not compute md5 of plugins. Exception occurred: %s", e.getStackTrace()));
        }
    }

    private void checkFilesAccessibility(File bundledPlugins, File externalPlugins) {
        boolean bundled = bundledPlugins.canRead();
        boolean external = externalPlugins.canRead();
        if (!bundled || !external) {
            String folder = bundled ? externalPlugins.getAbsolutePath() : bundledPlugins.getAbsolutePath();
            LOG.error("Could not read plugins. Please check access rights on files in folder: {}.", folder);
            throw new FileAccessRightsCheckException(String.format("Could not read plugins. Please check access rights in folder: %s", folder));
        }
    }
}
