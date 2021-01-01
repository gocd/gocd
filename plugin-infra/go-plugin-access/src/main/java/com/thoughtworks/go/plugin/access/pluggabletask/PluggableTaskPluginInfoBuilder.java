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
package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class PluggableTaskPluginInfoBuilder implements PluginInfoBuilder<PluggableTaskPluginInfo> {

    private TaskExtension extension;

    @Autowired
    public PluggableTaskPluginInfoBuilder(TaskExtension extension) {
        this.extension = extension;
    }

    @Override
    public PluggableTaskPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        final TaskPreference[] tp = {null};
        extension.doOnTask(descriptor.id(), (task, pluginDescriptor) -> tp[0] = new TaskPreference(task));

        TaskConfig config = tp[0].getConfig();
        TaskView view = tp[0].getView();
        if (config == null) {
            throw new RuntimeException(format("Plugin[%s] returned null task configuration", descriptor.id()));
        }
        if (view == null) {
            throw new RuntimeException(format("Plugin[%s] returned null task view", descriptor.id()));
        }
        String displayName = view.displayValue();

        PluggableInstanceSettings taskSettings = new PluggableInstanceSettings(configurations(config), new PluginView(view.template()));
        return new PluggableTaskPluginInfo(descriptor, displayName, taskSettings);
    }

}

