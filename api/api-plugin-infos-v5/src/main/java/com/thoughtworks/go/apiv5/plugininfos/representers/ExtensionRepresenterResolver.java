/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv5.plugininfos.representers;

import com.thoughtworks.go.apiv5.plugininfos.representers.extensions.*;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.HashMap;

public class ExtensionRepresenterResolver {
    private static HashMap<String, ExtensionRepresenter> extensionRepresenter;

    static {
        extensionRepresenter = new HashMap<>();
        extensionRepresenter.put(ExtensionType.AUTHORIZATION.getExtensionType(), new AuthorizationExtensionRepresenter());
        extensionRepresenter.put(ExtensionType.SCM.getExtensionType(), new SCMExtensionRepresenter());
        extensionRepresenter.put(ExtensionType.CONFIGREPO.getExtensionType(), new ConfigRepoExtensionRepresenter());
        extensionRepresenter.put(ExtensionType.ELASTICAGENT.getExtensionType(), new ElasticAgentExtensionRepresenter());
        extensionRepresenter.put(ExtensionType.TASK.getExtensionType(), new TaskExtensionRepresenter());
        extensionRepresenter.put(ExtensionType.PACKAGEREPOSITORY.getExtensionType(), new PackageMaterialExtensionRepresenter());
        extensionRepresenter.put(ExtensionType.NOTIFICATION.getExtensionType(), new NotificationPluginInfoRepresenter());
        extensionRepresenter.put(ExtensionType.ANALYTICS.getExtensionType(), new AnalyticsPluginInfoRepresenter());
        extensionRepresenter.put(ExtensionType.ARTIFACT.getExtensionType(), new ArtifactPluginInfoRepresenter());

    }

    static ExtensionRepresenter resolveRepresenterFor(PluginInfo extension) {
        return extensionRepresenter.get(extension.getExtensionName());
    }
}
