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
package com.thoughtworks.go.plugin.domain.authorization;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class AuthorizationPluginInfo extends PluginInfo {
    private final PluggableInstanceSettings authConfigSettings;
    private final PluggableInstanceSettings roleSettings;
    private final Capabilities capabilities;

    public AuthorizationPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings authConfigSettings,
                                   PluggableInstanceSettings roleSettings, Image image, Capabilities capabilities) {
        super(descriptor, PluginConstants.AUTHORIZATION_EXTENSION, null, image);
        this.authConfigSettings = authConfigSettings;
        this.roleSettings = roleSettings;
        this.capabilities = capabilities;
    }

    public PluggableInstanceSettings getAuthConfigSettings() {
        return authConfigSettings;
    }

    public PluggableInstanceSettings getRoleSettings() {
        return roleSettings;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthorizationPluginInfo that = (AuthorizationPluginInfo) o;

        if (authConfigSettings != null ? !authConfigSettings.equals(that.authConfigSettings) : that.authConfigSettings != null)
            return false;
        if (roleSettings != null ? !roleSettings.equals(that.roleSettings) : that.roleSettings != null) return false;
        if (image != null ? !image.equals(that.image) : that.image != null) return false;
        if (capabilities != null ? !capabilities.equals(that.capabilities) : that.capabilities != null) return false;
        return super.equals(that);
    }

    @Override
    public int hashCode() {
        int result = authConfigSettings != null ? authConfigSettings.hashCode() : 0;
        result = 31 * result + (roleSettings != null ? roleSettings.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (capabilities != null ? capabilities.hashCode() : 0);
        return result;
    }


}
