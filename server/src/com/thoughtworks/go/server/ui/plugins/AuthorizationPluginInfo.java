/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.ui.plugins;

import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;

public class AuthorizationPluginInfo extends NewPluginInfo {
    private final PluggableInstanceSettings authConfigSettings;
    private final PluggableInstanceSettings roleSettings;
    private final Image image;

    public AuthorizationPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings authConfigSettings, PluggableInstanceSettings roleSettings, Image image) {
        super(descriptor, AuthorizationPluginConstants.EXTENSION_NAME);
        this.authConfigSettings = authConfigSettings;
        this.roleSettings = roleSettings;
        this.image = image;
    }

    public PluggableInstanceSettings getAuthConfigSettings() {
        return authConfigSettings;
    }

    public PluggableInstanceSettings getRoleSettings() {
        return roleSettings;
    }

    public Image getImage() {
        return image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AuthorizationPluginInfo that = (AuthorizationPluginInfo) o;

        if (authConfigSettings != null ? !authConfigSettings.equals(that.authConfigSettings) : that.authConfigSettings != null)
            return false;
        if (roleSettings != null ? !roleSettings.equals(that.roleSettings) : that.roleSettings != null) return false;
        return image != null ? image.equals(that.image) : that.image == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (authConfigSettings != null ? authConfigSettings.hashCode() : 0);
        result = 31 * result + (roleSettings != null ? roleSettings.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        return result;
    }

}
