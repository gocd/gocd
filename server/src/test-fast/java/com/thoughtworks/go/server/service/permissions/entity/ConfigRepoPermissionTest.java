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
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.permissions.PermissionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.config.policy.SupportedAction.ADMINISTER;
import static com.thoughtworks.go.config.policy.SupportedAction.VIEW;
import static com.thoughtworks.go.config.policy.SupportedEntity.CONFIG_REPO;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigRepoPermissionTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;

    private Username username;
    private PermissionProvider permission;
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    void setUp() {
        initMocks(this);

        cruiseConfig = new BasicCruiseConfig();
        username = new Username("Bob" + UUID.randomUUID());
        permission = new ConfigRepoPermission(goConfigService, securityService);

        when(goConfigService.getMergedConfigForEditing()).thenReturn(cruiseConfig);
        SessionUtils.setCurrentUser(new GoUserPrinciple(username.getUsername().toString(), username.getUsername().toString(), GoAuthority.ROLE_ANONYMOUS.asAuthority()));
    }

    @Test
    void shouldGetNameOfThePermission() {
        assertThat(permission.name()).isEqualTo("config_repo");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPermissionsOnConfigRepositories() {
        ConfigRepoConfig repo1 = new ConfigRepoConfig();
        repo1.setId("repo1");
        ConfigRepoConfig repo2 = new ConfigRepoConfig();
        repo2.setId("repo2");
        cruiseConfig.getConfigRepos().addAll(asList(repo1, repo2));

        Map<String, Object> permissions = permission.permissions(username);

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", Collections.emptyList());
        configRepo.put("administer", Collections.emptyList());

        assertThat(permissions).isEqualTo(configRepo);
    }

    @Test
    void shouldReturnUserPermissibleConfigRepos() {
        ConfigRepoConfig repo1 = new ConfigRepoConfig();
        repo1.setId("repo1");
        ConfigRepoConfig repo2 = new ConfigRepoConfig();
        repo2.setId("repo2");
        cruiseConfig.getConfigRepos().addAll(asList(repo1, repo2));

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, CONFIG_REPO, "repo1", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), VIEW, CONFIG_REPO, "repo2", null)).thenReturn(true);

        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, CONFIG_REPO, "repo1", null)).thenReturn(true);
        when(securityService.doesUserHasPermissions(SessionUtils.currentUsername(), ADMINISTER, CONFIG_REPO, "repo2", null)).thenReturn(false);

        Map<String, Object> permissions = permission.permissions(username);

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", asList("repo1", "repo2"));
        configRepo.put("administer", asList("repo1"));

        assertThat(permissions).isEqualTo(configRepo);
    }
}
