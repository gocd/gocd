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

public class SCMPluginViewModel extends PluginViewModel {

    private SCMConfigurations scmConfigurations;
    public static final String TYPE = SCMExtension.EXTENSION_NAME;


    public SCMPluginViewModel() {
        super();
    }

    public SCMPluginViewModel(String pluginId, String version, String message) {
        super(pluginId, version, message);
    }

    public SCMPluginViewModel(String pluginId, String version, String message, SCMConfigurations scmConfigurations) {
        super(pluginId, version, message);
        this.scmConfigurations = scmConfigurations;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public List<SCMConfiguration> getConfigurations() {
        if (scmConfigurations == null) {
            this.scmConfigurations = SCMMetadataStore.getInstance().getConfigurationMetadata(getPluginId());
        }
        return scmConfigurations.list();
    }

    public Boolean hasPlugin(String pluginId){
        return SCMMetadataStore.getInstance().hasPlugin(pluginId);
    }
}
