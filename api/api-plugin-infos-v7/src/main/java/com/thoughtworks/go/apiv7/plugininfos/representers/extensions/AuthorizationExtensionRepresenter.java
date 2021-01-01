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
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class AuthorizationExtensionRepresenter extends ExtensionRepresenter {

    @Override
    public void toJSON(OutputWriter extensionWriter, PluginInfo extension) {
        super.toJSON(extensionWriter, extension);

        AuthorizationPluginInfo authorizationExtension = (AuthorizationPluginInfo) extension;
        PluggableInstanceSettings authConfigSettings = authorizationExtension.getAuthConfigSettings();
        PluggableInstanceSettings roleConfigSettings = authorizationExtension.getRoleSettings();

        extensionWriter.addChild("auth_config_settings", authConfigWriter -> PluggableInstanceSettingsRepresenter.toJSON(authConfigWriter, authConfigSettings));

        if (roleConfigSettings != null) {
            extensionWriter.addChild("role_settings", roleConfigWriter -> PluggableInstanceSettingsRepresenter.toJSON(roleConfigWriter, roleConfigSettings));
        }

        extensionWriter.addChild("capabilities", capabilitiesWriter ->
                capabilitiesWriter.add("can_search", authorizationExtension.getCapabilities().canSearch())
                        .add("supported_auth_type", authorizationExtension.getCapabilities().getSupportedAuthType().toString())
                        .add("can_authorize", authorizationExtension.getCapabilities().canAuthorize())
        );
    }
}
