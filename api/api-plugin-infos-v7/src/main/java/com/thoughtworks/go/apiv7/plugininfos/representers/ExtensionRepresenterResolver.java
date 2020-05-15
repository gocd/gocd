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
package com.thoughtworks.go.apiv7.plugininfos.representers;

import com.thoughtworks.go.apiv7.plugininfos.representers.extensions.*;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.HashMap;

public class ExtensionRepresenterResolver {
    private static final HashMap<String, ExtensionRepresenter> extensionRepresenter;

    static {
        extensionRepresenter = new HashMap<>();
        extensionRepresenter.put("authorization", new AuthorizationExtensionRepresenter());
        extensionRepresenter.put("scm", new SCMExtensionRepresenter());
        extensionRepresenter.put("configrepo", new ConfigRepoExtensionRepresenter());
        extensionRepresenter.put("elastic-agent", new ElasticAgentExtensionRepresenter());
        extensionRepresenter.put("task", new TaskExtensionRepresenter());
        extensionRepresenter.put("package-repository", new PackageMaterialExtensionRepresenter());
        extensionRepresenter.put("notification", new NotificationPluginInfoRepresenter());
        extensionRepresenter.put("analytics", new AnalyticsPluginInfoRepresenter());
        extensionRepresenter.put("artifact", new ArtifactPluginInfoRepresenter());
        extensionRepresenter.put("secrets", new SecretsExtensionRepresenter());
    }

    static ExtensionRepresenter resolveRepresenterFor(PluginInfo extension) {
        return extensionRepresenter.get(extension.getExtensionName());
    }
}
