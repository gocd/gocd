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
package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataStore;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.common.MetadataStore;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.notification.NotificationMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMaterialMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskMetadataStore;
import com.thoughtworks.go.plugin.access.scm.NewSCMMetadataStore;
import com.thoughtworks.go.plugin.access.secrets.SecretsMetadataStore;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.*;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class DefaultPluginInfoFinder {
    private Map<String, MetadataStore> builders = new LinkedHashMap<>();

    @Autowired
    public DefaultPluginInfoFinder(PluginManager pluginManager) {
        builders.put(PACKAGE_MATERIAL_EXTENSION, PackageMaterialMetadataStore.instance());
        builders.put(PLUGGABLE_TASK_EXTENSION, PluggableTaskMetadataStore.instance());
        builders.put(SCM_EXTENSION, NewSCMMetadataStore.instance());
        builders.put(NOTIFICATION_EXTENSION, NotificationMetadataStore.instance());
        builders.put(ELASTIC_AGENT_EXTENSION, ElasticAgentMetadataStore.instance());
        builders.put(AUTHORIZATION_EXTENSION, AuthorizationMetadataStore.instance());
        builders.put(ANALYTICS_EXTENSION, AnalyticsMetadataStore.instance());
        builders.put(CONFIG_REPO_EXTENSION, ConfigRepoMetadataStore.instance());
        builders.put(ARTIFACT_EXTENSION, ArtifactMetadataStore.instance());
        builders.put(SECRETS_EXTENSION, SecretsMetadataStore.instance());
    }

    public CombinedPluginInfo pluginInfoFor(String pluginId) {
        CombinedPluginInfo result = new CombinedPluginInfo();

        List<PluginInfo> allPluginInfosForPluginID = builders.values().stream()
                .map(metadataStore -> metadataStore.getPluginInfo(pluginId)).filter(Objects::nonNull).collect(toList());

        if (allPluginInfosForPluginID.isEmpty()) {
            return null;
        }
        result.addAll(allPluginInfosForPluginID);
        return result;
    }

    public Collection<CombinedPluginInfo> allPluginInfos(String type) {
        if (isBlank(type)) {
            return builders.values().stream()
                    .map((Function<MetadataStore, Collection<? extends PluginInfo>>) MetadataStore::allPluginInfos)
                    .flatMap((Function<Collection<? extends PluginInfo>, Stream<? extends PluginInfo>>) Collection::stream)
                    .collect(Collectors.groupingBy(pluginID(), toCollection(CombinedPluginInfo::new)))
                    .values();
        } else if (builders.containsKey(type)) {
            Collection<PluginInfo> pluginInfosForType = builders.get(type).allPluginInfos();
            return pluginInfosForType.stream()
                    .map(CombinedPluginInfo::new).collect(toList());
        } else {
            throw new InvalidPluginTypeException();
        }
    }

    public Map<String, String> pluginDisplayNameToPluginId(String extensionType) {
        Map<String, String> pluginDisplayNameToId = new HashMap<>();
        Collection<CombinedPluginInfo> combinedPluginInfos = allPluginInfos(extensionType);
        for (CombinedPluginInfo combinedPluginInfo : combinedPluginInfos) {
            pluginDisplayNameToId.put(combinedPluginInfo.getDescriptor().about().name(), combinedPluginInfo.getDescriptor().id());
        }
        return pluginDisplayNameToId;
    }

    private Function<PluginInfo, String> pluginID() {
        return pluginInfo -> pluginInfo.getDescriptor().id();
    }
}
