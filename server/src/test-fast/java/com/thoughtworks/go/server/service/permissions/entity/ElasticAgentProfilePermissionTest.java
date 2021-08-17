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

package com.thoughtworks.go.server.service.permissions.entity;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.permissions.PermissionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.thoughtworks.go.config.policy.SupportedAction.ADMINISTER;
import static com.thoughtworks.go.config.policy.SupportedAction.VIEW;
import static com.thoughtworks.go.config.policy.SupportedEntity.ELASTIC_AGENT_PROFILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ElasticAgentProfilePermissionTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;

    private Username username;
    private PermissionProvider permission;
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    void setUp() {

        cruiseConfig = new BasicCruiseConfig();
        username = new Username("Bob" + UUID.randomUUID());
        permission = new ElasticAgentProfilePermission(goConfigService, securityService);

        lenient().when(goConfigService.getMergedConfigForEditing()).thenReturn(cruiseConfig);
        SessionUtils.setCurrentUser(new GoUserPrinciple(username.getUsername().toString(), username.getUsername().toString(), GoAuthority.ROLE_ANONYMOUS.asAuthority()));
    }

    @Test
    void shouldGetNameOfThePermission() {
        assertThat(permission.name()).isEqualTo("elastic_agent_profile");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPermissionsOnElasticAgentProfiles() {
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("dev_cluster", "ecs"));
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod_cluster", "ecs"));

        cruiseConfig.getElasticConfig().getProfiles().add(new ElasticProfile("build-agent", "dev_cluster"));
        cruiseConfig.getElasticConfig().getProfiles().add(new ElasticProfile("deploy-agent", "prod_cluster"));

        Map<String, Object> permissions = permission.permissions(username);

        Map<String, Object> elasticAgentProfile = new LinkedHashMap<>();
        elasticAgentProfile.put("view", Collections.emptyList());
        elasticAgentProfile.put("administer", Collections.emptyList());

        assertThat(permissions).isEqualTo(elasticAgentProfile);
    }

    @Test
    void shouldReturnUserPermissibleElasticAgentProfiles() {
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("dev_cluster", "ecs"));
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod_cluster", "ecs"));

        cruiseConfig.getElasticConfig().getProfiles().add(new ElasticProfile("build-agent", "dev_cluster"));
        cruiseConfig.getElasticConfig().getProfiles().add(new ElasticProfile("deploy-agent", "prod_cluster"));

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, ELASTIC_AGENT_PROFILE, "build-agent", "dev_cluster")).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, ELASTIC_AGENT_PROFILE, "deploy-agent", "prod_cluster")).thenReturn(true);

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, ELASTIC_AGENT_PROFILE, "build-agent", "dev_cluster")).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, ELASTIC_AGENT_PROFILE, "deploy-agent", "prod_cluster")).thenReturn(false);

        Map<String, Object> permissions = permission.permissions(username);

        Map<String, Object> elasticAgentProfile = new LinkedHashMap<>();
        elasticAgentProfile.put("view", Arrays.asList("build-agent", "deploy-agent"));
        elasticAgentProfile.put("administer", Arrays.asList("build-agent"));

        assertThat(permissions).isEqualTo(elasticAgentProfile);
    }
}

