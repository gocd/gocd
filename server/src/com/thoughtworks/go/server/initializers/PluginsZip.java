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

package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipBuilder;
import com.thoughtworks.go.util.ZipUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class PluginsZip {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PluginsZip.class);
    private final SystemEnvironment systemEnvironment;
    private ZipUtil zipUtil;

    @Autowired
    public PluginsZip(SystemEnvironment systemEnvironment, ZipUtil zipUtil) {
        this.systemEnvironment = systemEnvironment;
        this.zipUtil = zipUtil;
    }

    public void create() {
        if (!systemEnvironment.get(SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED)) {
            return;
        }
        try {
            File bundledPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_GO_PROVIDED_PATH));
            File externalPlugins = new File(systemEnvironment.get(SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH));
            ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(new File(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH)), true);
            zipBuilder.add("bundled", bundledPlugins).add("external", externalPlugins).done();
        } catch (Exception e) {
            LOG.error("Could not create zip of plugins for agent to download.", e);
        }
    }

}
