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
package com.thoughtworks.go.plugin.domain.scm;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class SCMPluginInfo extends PluginInfo {

    private final String displayName;
    private final PluggableInstanceSettings scmSettings;

    public SCMPluginInfo(PluginDescriptor descriptor, String displayName, PluggableInstanceSettings scmSettings, PluggableInstanceSettings pluginSettings) {
        super(descriptor, PluginConstants.SCM_EXTENSION, pluginSettings, null);
        this.displayName = displayName;
        this.scmSettings = scmSettings;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PluggableInstanceSettings getScmSettings() {
        return scmSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SCMPluginInfo that = (SCMPluginInfo) o;

        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        return scmSettings != null ? scmSettings.equals(that.scmSettings) : that.scmSettings == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (scmSettings != null ? scmSettings.hashCode() : 0);
        return result;
    }
}
