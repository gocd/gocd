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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;

public class AuthPluginInfoViewModel {
    private AuthorizationPluginInfo pluginInfo;

    public AuthPluginInfoViewModel(AuthorizationPluginInfo pluginInfo) {
        this.pluginInfo = pluginInfo;
    }

    public String name() {
        return pluginInfo.getDescriptor().about().name();
    }

    public String imageUrl() {
        return String.format("/go/api/plugin_images/%s/%s", pluginId(), pluginInfo.getImage().getHash());
    }

    public String pluginId() {
        return pluginInfo.getDescriptor().id();
    }
}

