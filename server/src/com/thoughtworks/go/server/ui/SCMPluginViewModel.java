/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;

import java.util.List;

public class SCMPluginViewModel implements PluginViewModel {
    private String pluginId;
    private String version;
    private SCMConfigurations scmConfigurations;
    public static final String TYPE = SCMExtension.EXTENSION_NAME;
    private String message;

    public SCMPluginViewModel() {
    }

    public SCMPluginViewModel(String pluginId, String version, SCMConfigurations scmConfigurations) {
        this.pluginId = pluginId;
        this.version = version;
        this.scmConfigurations = scmConfigurations;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public List<SCMConfiguration> getConfigurations() {
        return scmConfigurations.list();
    }

    @Override
    public void setViewModel(String id, String version,String message) {
        this.pluginId = id;
        this.version = version;
        this.scmConfigurations = SCMMetadataStore.getInstance().getConfigurationMetadata(id);
        this.message = message;
    }
}
