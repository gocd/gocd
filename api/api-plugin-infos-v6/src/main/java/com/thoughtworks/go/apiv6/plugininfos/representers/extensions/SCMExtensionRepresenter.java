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
package com.thoughtworks.go.apiv6.plugininfos.representers.extensions;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv6.plugininfos.representers.PluggableInstanceSettingsRepresenter;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo;

public class SCMExtensionRepresenter extends ExtensionRepresenter {
    @Override
    public void toJSON(OutputWriter extensionWriter, PluginInfo extension) {
        super.toJSON(extensionWriter, extension);

        SCMPluginInfo scmPluginInfo = (SCMPluginInfo) extension;

        extensionWriter.add("display_name", scmPluginInfo.getDisplayName())
                .addChild("scm_settings", scmSettingsWriter -> PluggableInstanceSettingsRepresenter.toJSON(scmSettingsWriter, scmPluginInfo.getScmSettings()));
    }
}
