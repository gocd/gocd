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

package com.thoughtworks.go.server.ui.plugins;

import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;

public class AuthorizationPluginInfo extends PluginInfo {

    private final PluggableInstanceSettings roleSettings;

    public AuthorizationPluginInfo(PluginDescriptor descriptor, String type, String displayName, PluggableInstanceSettings settings, PluggableInstanceSettings roleSettings, Image icon) {
        super(descriptor, type, displayName, settings, icon);
        this.roleSettings = roleSettings;
    }

    public PluggableInstanceSettings getRoleSettings() {
        return roleSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AuthorizationPluginInfo that = (AuthorizationPluginInfo) o;

        return roleSettings != null ? roleSettings.equals(that.roleSettings) : that.roleSettings == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (roleSettings != null ? roleSettings.hashCode() : 0);
        return result;
    }
}
