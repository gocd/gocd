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
package com.thoughtworks.go.apiv7.plugininfos.representers.extensions;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv7.plugininfos.representers.PluggableInstanceSettingsRepresenter;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.domain.secrets.SecretsPluginInfo;

public class SecretsExtensionRepresenter extends ExtensionRepresenter {
    @Override
    public void toJSON(OutputWriter extensionWriter, PluginInfo pluginInfo) {
        super.toJSON(extensionWriter, pluginInfo);

        SecretsPluginInfo secretsPluginInfo = (SecretsPluginInfo) pluginInfo;
        PluggableInstanceSettings secretsConfigSettings = secretsPluginInfo.getSecretsConfigSettings();
        if (secretsConfigSettings != null) {
            extensionWriter.addChild("secret_config_settings", authConfigWriter -> PluggableInstanceSettingsRepresenter.toJSON(authConfigWriter, secretsConfigSettings));
        }
    }
}
