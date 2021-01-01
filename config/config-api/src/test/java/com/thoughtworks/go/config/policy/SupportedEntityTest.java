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
package com.thoughtworks.go.config.policy;

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.config.policy.SupportedEntity.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SupportedEntityTest {
    @Test
    void shouldSupportEnvironment() {
        assertThat(ENVIRONMENT.getType()).isEqualTo("environment");
        assertThat(ENVIRONMENT.getEntityType()).isEqualTo(EnvironmentConfig.class);

        assertThat(ENVIRONMENT.getEntityType().isAssignableFrom(BasicEnvironmentConfig.class)).isTrue();
        assertThat(ENVIRONMENT.getEntityType().isAssignableFrom(MergeEnvironmentConfig.class)).isTrue();
    }

    @Test
    void shouldSupportConfigRepo() {
        assertThat(CONFIG_REPO.getType()).isEqualTo("config_repo");
        assertThat(CONFIG_REPO.getEntityType()).isEqualTo(ConfigRepoConfig.class);
    }

    @Test
    void shouldSupportElasticAgentProfile() {
        assertThat(ELASTIC_AGENT_PROFILE.getType()).isEqualTo("elastic_agent_profile");
        assertThat(ELASTIC_AGENT_PROFILE.getEntityType()).isEqualTo(ElasticProfile.class);
    }

    @Test
    void shouldSupportClusterProfile() {
        assertThat(CLUSTER_PROFILE.getType()).isEqualTo("cluster_profile");
        assertThat(CLUSTER_PROFILE.getEntityType()).isEqualTo(ClusterProfile.class);
    }

    @Test
    void shouldReturnUnmodifiableListOfTheSupportedEntities() {
        List<String> entities = unmodifiableListOf(ENVIRONMENT);

        assertThat(entities).hasSize(1).contains(ENVIRONMENT.getType());
        assertThatCode(() -> entities.add("foo"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
