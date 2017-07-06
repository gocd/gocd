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

package com.thoughtworks.go.plugin.domain.pluggabletask;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class PluggableTaskPluginInfo extends PluginInfo {

    private final String displayName;
    private final PluggableInstanceSettings taskSettings;

    public PluggableTaskPluginInfo(PluginDescriptor descriptor, String displayName, PluggableInstanceSettings taskSettings) {
        super(descriptor, PluginConstants.PLUGGABLE_TASK_EXTENSION);
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
    public PluggableInstanceSettings getPluginSettings() {
        return null;
    }
}
