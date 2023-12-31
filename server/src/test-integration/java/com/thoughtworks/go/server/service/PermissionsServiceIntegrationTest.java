/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.policy.Allow;
import com.thoughtworks.go.config.policy.Policy;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.permissions.PermissionsService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PermissionsServiceIntegrationTest {
    @Autowired
    private PermissionsService permissionsService;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;

    private GoConfigFileHelper configHelper;

    @BeforeEach
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldReturnAllTheEntityNamesThatSupportsPermission() {
        assertThat(permissionsService.allEntitiesSupportsPermission()).containsExactlyInAnyOrder(
                "environment",
                "config_repo",
                "elastic_agent_profile",
                "cluster_profile"
        );
    }

    @Test
    public void shouldShowUserSpecificEnvironments() {
        goConfigService.updateConfig(cruiseConfig -> {
            defineSecurity(cruiseConfig);
            definePolicy(cruiseConfig, "environment", "prod", "dev*");

            cruiseConfig.getEnvironments().add(new BasicEnvironmentConfig(new CaseInsensitiveString("prod")));
            cruiseConfig.getEnvironments().add(new BasicEnvironmentConfig(new CaseInsensitiveString("dev1")));
            cruiseConfig.getEnvironments().add(new BasicEnvironmentConfig(new CaseInsensitiveString("dev2")));

            return cruiseConfig;
        });

        Map<String, Object> permissions = permissionsService.getPermissions(List.of("environment"));

        Map<String, Object> expectedEnvs = new LinkedHashMap<>();
        expectedEnvs.put("view", List.of("prod", "dev1", "dev2"));
        expectedEnvs.put("administer", List.of("dev1", "dev2"));

        assertThat(permissions.get("environment")).isEqualTo(expectedEnvs);
    }

    @Test
    public void shouldShowUserSpecificConfigRepos() {
        goConfigService.updateConfig(cruiseConfig -> {
            defineSecurity(cruiseConfig);
            definePolicy(cruiseConfig, "config_repo", "repo1", "repo2");

            ConfigRepoConfig repo1 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.git("git-url"), "pluginid", "repo1");
            ConfigRepoConfig repo2 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.git("git-url2"), "pluginid", "repo2");
            cruiseConfig.getConfigRepos().addAll(List.of(repo1, repo2));

            return cruiseConfig;
        });

        Map<String, Object> permissions = permissionsService.getPermissions(List.of("config_repo"));

        Map<String, Object> expectedCR = new LinkedHashMap<>();
        expectedCR.put("view", List.of("repo1", "repo2"));
        expectedCR.put("administer", List.of("repo2"));

        assertThat(permissions.get("config_repo")).isEqualTo(expectedCR);
    }

    @Test
    public void shouldShowUserSpecificClusterProfiles() {
        goConfigService.updateConfig(cruiseConfig -> {
            defineSecurity(cruiseConfig);
            definePolicy(cruiseConfig, "cluster_profile", "prod*", "dev*");

            ClusterProfile devCluster = new ClusterProfile("dev-cluster", "ecs");
            ClusterProfile prodCluster = new ClusterProfile("prod-cluster", "ecs");
            cruiseConfig.getElasticConfig().getClusterProfiles().addAll(List.of(devCluster, prodCluster));
            return cruiseConfig;
        });

        Map<String, Object> permissions = permissionsService.getPermissions(List.of("cluster_profile"));

        Map<String, Object> expectedClusterProfiles = new LinkedHashMap<>();
        expectedClusterProfiles.put("view", List.of("dev-cluster", "prod-cluster"));
        expectedClusterProfiles.put("administer", List.of("dev-cluster"));

        assertThat(permissions.get("cluster_profile")).isEqualTo(expectedClusterProfiles);
    }

    @Test
    public void shouldShowUserSpecificElasticAgentProfiles() {
        goConfigService.updateConfig(cruiseConfig -> {
            defineSecurity(cruiseConfig);
            definePolicy(cruiseConfig, "cluster_profile", "prod*", "dev*");

            ClusterProfile devCluster = new ClusterProfile("dev-cluster", "ecs");
            ClusterProfile prodCluster = new ClusterProfile("prod-cluster", "ecs");
            cruiseConfig.getElasticConfig().getClusterProfiles().addAll(List.of(devCluster, prodCluster));

            ElasticProfile buildAgent = new ElasticProfile("build-agent", "dev-cluster");
            ElasticProfile deployAgent = new ElasticProfile("deploy-agent", "prod-cluster");
            cruiseConfig.getElasticConfig().getProfiles().addAll(List.of(buildAgent, deployAgent));

            return cruiseConfig;
        });

        Map<String, Object> permissions = permissionsService.getPermissions(List.of("elastic_agent_profile"));

        Map<String, Object> expectedElasticProfiles = new LinkedHashMap<>();
        expectedElasticProfiles.put("view", List.of("build-agent", "deploy-agent"));
        expectedElasticProfiles.put("administer", List.of("build-agent"));

        assertThat(permissions.get("elastic_agent_profile")).isEqualTo(expectedElasticProfiles);
    }


    private void definePolicy(CruiseConfig cruiseConfig, String type, String resource1, String resource2) {
        Role role = new RoleConfig("gocd", new RoleUser(SessionUtils.currentUsername().getUsername()));
        Policy policy = new Policy();
        policy.add(new Allow("view", type, resource1));
        policy.add(new Allow("administer", type, resource2));
        role.setPolicy(policy);
        cruiseConfig.server().security().getRoles().add(role);
    }

    private void defineSecurity(CruiseConfig cruiseConfig) {
        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig("filebased", "filebased"));
        cruiseConfig.server().security().adminsConfig().add(new AdminUser("admin"));
    }
}
