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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.config.policy.SupportedAction.ADMINISTER;
import static com.thoughtworks.go.config.policy.SupportedAction.VIEW;
import static com.thoughtworks.go.config.policy.SupportedEntity.ENVIRONMENT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnvironmentPermissionTest {
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
        permission = new EnvironmentPermission(goConfigService, securityService);

        lenient().when(goConfigService.getMergedConfigForEditing()).thenReturn(cruiseConfig);
        SessionUtils.setCurrentUser(new GoUserPrinciple(username.getUsername().toString(), username.getUsername().toString(), GoAuthority.ROLE_ANONYMOUS.asAuthority()));
    }

    @Test
    void shouldGetNameOfThePermission() {
        assertThat(permission.name()).isEqualTo("environment");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPermissionsOnEnvironments() {
        cruiseConfig.addEnvironment("QA");
        cruiseConfig.addEnvironment("UAT");
        cruiseConfig.addEnvironment("Prod");

        Map<String, Object> permissions = permission.permissions(username);

        Map<String, Object> environments = new LinkedHashMap<>();
        environments.put("view", Collections.emptyList());
        environments.put("administer", Collections.emptyList());

        assertThat(permissions).isEqualTo(environments);
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

        Map<String, Object> permissions = permission.permissions(username);

        Map<String, Object> environments = new LinkedHashMap<>();
        environments.put("view", asList("QA", "UAT"));
        environments.put("administer", asList("QA"));

        assertThat(permissions).isEqualTo(environments);
    }
}
