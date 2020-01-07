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
package com.thoughtworks.go.config.policy;

import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public enum SupportedEntity {
    CLUSTER_PROFILE("cluster_profile", ClusterProfile.class),
    ELASTIC_AGENT_PROFILE("elastic_agent_profile", ElasticProfile.class),
    ENVIRONMENT("environment", EnvironmentConfig.class),
    CONFIG_REPO("config_repo", ConfigRepoConfig.class),
    UNKNOWN(null, null);

    private final String type;
    private final Class<? extends Validatable> entityType;

    SupportedEntity(String type, Class<? extends Validatable> entityClass) {
        this.type = type;
        this.entityType = entityClass;
    }

    public String getType() {
        return type;
    }

    public Class<? extends Validatable> getEntityType() {
        return entityType;
    }

    public static SupportedEntity fromString(String type) {
        return Arrays.stream(values()).filter(t -> equalsIgnoreCase(t.type, type))
                .findFirst().orElse(UNKNOWN);
    }

    public static List<String> unmodifiableListOf(SupportedEntity... supportedEntities) {
        return unmodifiableList(Arrays.stream(supportedEntities)
                .map(SupportedEntity::getType)
                .collect(Collectors.toList()));
    }
}
