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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMetadataStore;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants;
import com.thoughtworks.go.plugin.access.common.MetadataStore;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMaterialMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.NewSCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang.StringUtils.isBlank;

@Component
public class DefaultPluginInfoFinder {

    private final PluginManager pluginManager;
    private Map<String, MetadataStore> builders = new LinkedHashMap<>();

    @Autowired
    public DefaultPluginInfoFinder(PluginManager pluginManager) {
        this.pluginManager = pluginManager;

        builders.put(PackageRepositoryExtension.EXTENSION_NAME, PackageMaterialMetadataStore.instance());
        builders.put(TaskExtension.TASK_EXTENSION, PluggableTaskMetadataStore.instance());
        builders.put(SCMExtension.EXTENSION_NAME, NewSCMMetadataStore.instance());
        builders.put(AuthenticationExtension.EXTENSION_NAME, AuthenticationMetadataStore.instance());
        builders.put(NotificationExtension.EXTENSION_NAME, NotificationMetadataStore.instance());
        builders.put(ElasticAgentPluginConstants.EXTENSION_NAME, ElasticAgentMetadataStore.instance());
        builders.put(AuthorizationPluginConstants.EXTENSION_NAME, AuthorizationMetadataStore.instance());
        builders.put(ConfigRepoExtension.EXTENSION_NAME, ConfigRepoMetadataStore.instance());
        builders.put(AnalyticsExtension.EXTENSION_NAME, AnalyticsMetadataStore.instance());
    }

    public PluginInfo pluginInfoFor(String pluginId) {
        return builders.values().stream().map(new Function<MetadataStore, PluginInfo>() {
            @Override
            public PluginInfo apply(MetadataStore metadataStore) {
                return metadataStore.getPluginInfo(pluginId);
            }
        }).filter(new Predicate<PluginInfo>() {
            @Override
            public boolean test(PluginInfo obj) {
                return Objects.nonNull(obj);
            }
        }).findFirst().orElse(null);
    }

    public Collection allPluginInfos(String type) {
        if (isBlank(type)) {
            return builders.values().stream().map(new Function<MetadataStore, Collection>() {
                @Override
                public Collection apply(MetadataStore metadataStore) {
                    return metadataStore.allPluginInfos();
                }
            }).flatMap(new Function<Collection, Stream<?>>() {
                @Override
                public Stream<?> apply(Collection pi) {
                    return pi.stream();
                }
            }).collect(Collectors.toList());
        } else if (builders.containsKey(type)) {
            return builders.get(type).allPluginInfos();
        } else {
            throw new InvalidPluginTypeException();
        }
    }

    public Image getImage(String pluginId, String hash) {
        PluginInfo pluginInfo = pluginInfoFor(pluginId);

        Image image = findImage(pluginInfo);
        if (image != null && image.getHash().equals(hash)) {
            return image;
        }
        return null;
    }

    private Image findImage(PluginInfo pluginInfo) {
        if (pluginInfo instanceof com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo) {
            return ((com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo) pluginInfo).getImage();
        } else if (pluginInfo instanceof ElasticAgentPluginInfo) {
            return ((ElasticAgentPluginInfo) pluginInfo).getImage();
        }
        return null;
    }

}
