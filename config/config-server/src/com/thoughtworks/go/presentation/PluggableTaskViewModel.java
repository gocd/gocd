/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.presentation;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;

public class PluggableTaskViewModel implements PluggableViewModel<PluggableTask> {
    private PluggableTask pluggableTask;
    private final String templatePathForPluggableTaskContainer;
    private final String renderer;
    private final String typeForDisplay;
    private String templateForPluginTaskContents;
    private Gson gson;

    public PluggableTaskViewModel(PluggableTask pluggableTask, String templatePathForPluggableTaskContainer, String renderer, String typeForDisplay, String templateForPluginTaskContents) {
        this.pluggableTask = pluggableTask;
        this.templatePathForPluggableTaskContainer = templatePathForPluggableTaskContainer;
        this.renderer = renderer;
        this.typeForDisplay = typeForDisplay;
        this.templateForPluginTaskContents = templateForPluginTaskContents;
        this.gson = new Gson();
    }

    @Override
    public String getRenderingFramework() {
        return renderer;
    }

    @Override
    public String getTemplatePath() {
        return templatePathForPluggableTaskContainer;
    }

    @Override
    public Map<String, Object> getParameters() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("data", gson.toJson(pluggableTask.configAsMap()));
        map.put("template", getTemplate());
        return map;
    }

    protected String getTemplate() {
        return templateForPluginTaskContents;
    }

    @Override
    public PluggableTask getModel() {
        return pluggableTask;
    }

    @Override
    public void setModel(PluggableTask model) {
        this.pluggableTask = model;
    }

    @Override
    public String getTypeForDisplay() {
        return typeForDisplay;
    }

    @Override
    public String getTaskType() {
        return pluggableTask.getTaskType();
    }
}
