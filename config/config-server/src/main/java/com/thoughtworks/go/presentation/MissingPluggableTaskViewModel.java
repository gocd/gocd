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
package com.thoughtworks.go.presentation;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;

public class MissingPluggableTaskViewModel extends PluggableTaskViewModel {

    private final PluggableTask pluggableTask;

    public MissingPluggableTaskViewModel(PluggableTask pluggableTask, String templatePathForPluggableTaskContainer) {
        super(pluggableTask, templatePathForPluggableTaskContainer, null, null);
        this.pluggableTask = pluggableTask;
    }

    @Override
    protected String getTemplate() {
        return String.format("Associated plugin '%s' not found. Please contact the Go admin to install the plugin.", pluggableTask.getPluginConfiguration().getId());
    }

    @Override
    public String getTypeForDisplay() {
        return pluggableTask.getPluginConfiguration().getId();
    }
}
