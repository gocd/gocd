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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.*;

import static com.thoughtworks.go.config.policy.SupportedAction.ADMINISTER;
import static com.thoughtworks.go.config.policy.SupportedAction.VIEW;
import static com.thoughtworks.go.config.policy.SupportedEntity.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PermissionsServiceTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;

    private Username username;
    private PermissionsService service;
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    void setUp() {
        initMocks(this);

        cruiseConfig = new BasicCruiseConfig();
        username = new Username("Bob" + UUID.randomUUID());
        service = new PermissionsService(goConfigService, securityService);

        when(goConfigService.getMergedConfigForEditing()).thenReturn(cruiseConfig);

        SessionUtils.setCurrentUser(new GoUserPrinciple(username.getUsername().toString(), username.getUsername().toString(), GoAuthority.ROLE_ANONYMOUS.asAuthority()));
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPermissionsOnEnvironments() {
        cruiseConfig.addEnvironment("QA");
        cruiseConfig.addEnvironment("UAT");
        cruiseConfig.addEnvironment("Prod");

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> environments = new LinkedHashMap<>();
        environments.put("view", Collections.emptyList());
        environments.put("administer", Collections.emptyList());

        assertThat(permissions.get("environment")).isEqualTo(environments);
    }

    @Test
    void shouldReturnUserPermissibleEnvironments() {
        cruiseConfig.addEnvironment("QA");
        cruiseConfig.addEnvironment("UAT");
        cruiseConfig.addEnvironment("Prod");

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, ENVIRONMENT, "QA", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, ENVIRONMENT, "UAT", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, ENVIRONMENT, "Prod", null)).thenReturn(false);

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, ENVIRONMENT, "QA", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, ENVIRONMENT, "UAT", null)).thenReturn(false);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, ENVIRONMENT, "Prod", null)).thenReturn(false);

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> environments = new LinkedHashMap<>();
        environments.put("view", asList("QA", "UAT"));
        environments.put("administer", asList("QA"));

        assertThat(permissions.get("environment")).isEqualTo(environments);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPermissionsOnConfigRepositories() {
        ConfigRepoConfig repo1 = new ConfigRepoConfig();
        repo1.setId("repo1");
        ConfigRepoConfig repo2 = new ConfigRepoConfig();
        repo2.setId("repo2");
        cruiseConfig.getConfigRepos().addAll(Arrays.asList(repo1, repo2));

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", Collections.emptyList());
        configRepo.put("administer", Collections.emptyList());

        assertThat(permissions.get("config_repo")).isEqualTo(configRepo);
    }

    @Test
    void shouldReturnUserPermissibleConfigRepos() {
        ConfigRepoConfig repo1 = new ConfigRepoConfig();
        repo1.setId("repo1");
        ConfigRepoConfig repo2 = new ConfigRepoConfig();
        repo2.setId("repo2");
        cruiseConfig.getConfigRepos().addAll(Arrays.asList(repo1, repo2));

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, CONFIG_REPO, "repo1", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, CONFIG_REPO, "repo2", null)).thenReturn(true);

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, CONFIG_REPO, "repo1", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, CONFIG_REPO, "repo2", null)).thenReturn(false);

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", Arrays.asList("repo1", "repo2"));
        configRepo.put("administer", Arrays.asList("repo1"));

        assertThat(permissions.get("config_repo")).isEqualTo(configRepo);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPermissionsOnClusterProfiles() {
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("dev_cluster", "ecs"));
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod_cluster", "ecs"));

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", Collections.emptyList());
        configRepo.put("administer", Collections.emptyList());

        assertThat(permissions.get("cluster_profile")).isEqualTo(configRepo);
    }

    @Test
    void shouldReturnUserPermissibleClusterProfiles() {
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("dev_cluster", "ecs"));
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod_cluster", "ecs"));

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, CLUSTER_PROFILE, "dev_cluster", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, CLUSTER_PROFILE, "prod_cluster", null)).thenReturn(true);

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, CLUSTER_PROFILE, "dev_cluster", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, CLUSTER_PROFILE, "prod_cluster", null)).thenReturn(false);

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", Arrays.asList("dev_cluster", "prod_cluster"));
        configRepo.put("administer", Arrays.asList("dev_cluster"));

        assertThat(permissions.get("cluster_profile")).isEqualTo(configRepo);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPermissionsOnElasticAgentProfiles() {
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("dev_cluster", "ecs"));
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod_cluster", "ecs"));

        cruiseConfig.getElasticConfig().getProfiles().add(new ElasticProfile("build-agent", "dev_cluster"));
        cruiseConfig.getElasticConfig().getProfiles().add(new ElasticProfile("deploy-agent", "prod_cluster"));

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", Collections.emptyList());
        configRepo.put("administer", Collections.emptyList());

        assertThat(permissions.get("elastic_agent_profile")).isEqualTo(configRepo);
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

        Map<String, Object> permissions = service.getPermissionsForCurrentUser();

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", Arrays.asList("build-agent", "deploy-agent"));
        configRepo.put("administer", Arrays.asList("build-agent"));

        assertThat(permissions.get("elastic_agent_profile")).isEqualTo(configRepo);
    }
}
