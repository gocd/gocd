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

import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants;
import com.thoughtworks.go.plugin.access.elastic.ElasticPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException;
import com.thoughtworks.go.server.ui.plugins.AuthorizationPluginInfo;
import com.thoughtworks.go.server.ui.plugins.ElasticPluginInfo;
import com.thoughtworks.go.server.ui.plugins.NewPluginInfo;
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
public class DefaultPluginInfoBuilder {

    private final PluginManager pluginManager;
    private Map<String, NewPluginInfoBuilder> builders = new LinkedHashMap<>();

    @Autowired
    public DefaultPluginInfoBuilder(AuthenticationPluginRegistry authenticationPluginRegistry,
                                    NotificationPluginRegistry notificationPluginRegistry,
                                    ElasticPluginConfigMetadataStore elasticPluginConfigMetadataStore,
                                    AuthorizationPluginConfigMetadataStore authorizationPluginConfigMetadataStore,
                                    PluginManager pluginManager) {
        this.pluginManager = pluginManager;

        builders.put(AuthenticationExtension.EXTENSION_NAME, new AuthenticationPluginInfoBuilder(pluginManager, authenticationPluginRegistry));
        builders.put(NotificationExtension.EXTENSION_NAME, new NotificationPluginInfoBuilder(pluginManager, notificationPluginRegistry));
        builders.put(PackageRepositoryExtension.EXTENSION_NAME, new PackageRepositoryPluginInfoBuilder(pluginManager, PackageMetadataStore.getInstance(), RepositoryMetadataStore.getInstance()));
        builders.put(TaskExtension.TASK_EXTENSION, new PluggableTaskPluginInfoBuilder(pluginManager, PluggableTaskConfigStore.store()));
        builders.put(SCMExtension.EXTENSION_NAME, new SCMPluginInfoBuilder(pluginManager, SCMMetadataStore.getInstance()));
        builders.put(ElasticAgentPluginConstants.EXTENSION_NAME, new ElasticAgentPluginInfoBuilder(elasticPluginConfigMetadataStore));
        builders.put(AuthorizationPluginConstants.EXTENSION_NAME, new AuthorizationPluginInfoBuilder(authorizationPluginConfigMetadataStore));
    }

    public NewPluginInfo pluginInfoFor(String pluginId) {
        return builders.values().stream().map(new Function<NewPluginInfoBuilder, NewPluginInfo>() {
            @Override
            public NewPluginInfo apply(NewPluginInfoBuilder builder) {
                return builder.pluginInfoFor(pluginId);
            }
        }).filter(new Predicate<NewPluginInfo>() {
            @Override
            public boolean test(NewPluginInfo obj) {
                return Objects.nonNull(obj);
            }
        }).findFirst().orElse(null);
    }

    public Collection allPluginInfos(String type) {
        if (isBlank(type)) {
            return builders.values().stream().map(new Function<NewPluginInfoBuilder, Collection>() {
                @Override
                public Collection apply(NewPluginInfoBuilder builder) {
                    return builder.allPluginInfos();
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
        NewPluginInfo pluginInfo = pluginInfoFor(pluginId);

        Image image = findImage(pluginInfo);
        if (image != null && image.getHash().equals(hash)) {
            return image;
        }
        return null;
    }

    private Image findImage(NewPluginInfo pluginInfo) {
        if (pluginInfo instanceof AuthorizationPluginInfo) {
            return ((AuthorizationPluginInfo) pluginInfo).getImage();
        } else if (pluginInfo instanceof ElasticPluginInfo) {
            return ((ElasticPluginInfo) pluginInfo).getImage();
        }
        return null;
    }
}
