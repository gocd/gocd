/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.domain.pluggabletask;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.Objects;

public class PluggableTaskPluginInfo extends PluginInfo {

    private final String displayName;
    private final PluggableInstanceSettings taskSettings;

    public PluggableTaskPluginInfo(PluginDescriptor descriptor, String displayName, PluggableInstanceSettings taskSettings) {
        super(descriptor, PluginConstants.PLUGGABLE_TASK_EXTENSION, null, null);
        this.displayName = displayName;
        this.taskSettings = taskSettings;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PluggableInstanceSettings getTaskSettings() {
        return taskSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PluggableTaskPluginInfo that = (PluggableTaskPluginInfo) o;

        if (!Objects.equals(displayName, that.displayName)) {
            return false;
        }
        return Objects.equals(taskSettings, that.taskSettings);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (taskSettings != null ? taskSettings.hashCode() : 0);
        return result;
    }
}
