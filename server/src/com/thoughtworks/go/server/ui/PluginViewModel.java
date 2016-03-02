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

package com.thoughtworks.go.server.ui;

public abstract class PluginViewModel {
    private String pluginId;
    private String version;
    private String message;

    protected PluginViewModel(){}

    protected PluginViewModel(String pluginId, String version, String message) {
        this.pluginId = pluginId;
        this.version = version;
        this.message = message;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getVersion() {
        return version;
    }

    public String getMessage() {
        return message;
    }

    public abstract String getType();

    public abstract Boolean hasPlugin(String pluginId);
}
