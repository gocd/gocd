/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.update.AdminsConfigUpdateCommand;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AdminsConfigServiceTest {

    @Mock
    private GoConfigService goConfigService;

    private BasicCruiseConfig cruiseConfig;

    private AdminsConfigService adminsConfigService;

    @Mock
    private EntityHashingService entityHashingService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        adminsConfigService = new AdminsConfigService(goConfigService, entityHashingService);
    }

    @Test
    public void update_shouldAddAdminsToConfig() {
        AdminsConfig config = new AdminsConfig();
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        adminsConfigService.update(admin, config, "md5", result);

        verify(goConfigService).updateConfig(any(AdminsConfigUpdateCommand.class), eq(admin));
    }

    @Test
    public void shouldBulkUpdateToAddAdminsToConfig() {
        AdminsConfig config = new AdminsConfig(new AdminUser("existingAdminUser"), new AdminRole("existingAdminRole"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.server().security().setAdminsConfig(config);
        when(goConfigService.serverConfig()).thenReturn(cruiseConfig.server());

        Username user = new Username("user");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        adminsConfigService.bulkUpdate(user, singletonList("newUser1"), emptyList(), singletonList("newRole1"), emptyList(), "md5", result);

        ArgumentCaptor<AdminsConfigUpdateCommand> captor = ArgumentCaptor.forClass(AdminsConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(captor.capture(), eq(user));
        AdminsConfigUpdateCommand updateCommand = captor.getValue();

        AdminsConfig adminsConfig = updateCommand.getPreprocessedEntityConfig();
        assertThat(adminsConfig.getUsers(), hasSize(2));
        assertThat(adminsConfig.getUsers(), hasItems(new AdminUser("existingAdminUser"), new AdminUser("newUser1")));
        assertThat(adminsConfig.getRoles(), hasSize(2));
        assertThat(adminsConfig.getRoles(), hasItems(new AdminRole("existingAdminRole"), new AdminRole("newRole1")));
    }

    @Test
    public void shouldBulkUpdateToRemoveAdminsFromConfig() {
        AdminsConfig config = new AdminsConfig(new AdminUser("adminUser1"), new AdminUser("adminUser2"),
                new AdminRole("adminRole1"), new AdminRole("adminRole2"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.server().security().setAdminsConfig(config);
        when(goConfigService.serverConfig()).thenReturn(cruiseConfig.server());

        Username user = new Username("user");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        adminsConfigService.bulkUpdate(user, emptyList(), singletonList("adminUser1"), emptyList(), singletonList("adminRole1"), "md5", result);

        ArgumentCaptor<AdminsConfigUpdateCommand> captor = ArgumentCaptor.forClass(AdminsConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(captor.capture(), eq(user));
        AdminsConfigUpdateCommand updateCommand = captor.getValue();

        AdminsConfig adminsConfig = updateCommand.getPreprocessedEntityConfig();
        assertThat(adminsConfig.getUsers(), hasSize(1));
        assertThat(adminsConfig.getUsers(), hasItems(new AdminUser("adminUser2")));
        assertThat(adminsConfig.getRoles(), hasSize(1));
        assertThat(adminsConfig.getRoles(), hasItems(new AdminRole("adminRole2")));
    }

    @Test
    public void bulkUpdate_shouldValidateThatRoleIsAnAdminWhenTryingToRemove() {
        AdminsConfig config = new AdminsConfig(new AdminUser("adminUser1"), new AdminUser("adminUser2"),
                new AdminRole("adminRole1"), new AdminRole("adminRole2"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.server().security().setAdminsConfig(config);
        when(goConfigService.serverConfig()).thenReturn(cruiseConfig.server());

        Username user = new Username("user");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        adminsConfigService.bulkUpdate(user, emptyList(), emptyList(), emptyList(), singletonList("someOtherRole"), "md5", result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(404));

        verify(goConfigService, times(0)).updateConfig(any(), any());
    }

    @Test
    public void bulkUpdate_shouldValidateThatUserIsAnAdminWhenTryingToRemove() {
        AdminsConfig config = new AdminsConfig(new AdminUser("adminUser1"), new AdminUser("adminUser2"),
                new AdminRole("adminRole1"), new AdminRole("adminRole2"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.server().security().setAdminsConfig(config);
        when(goConfigService.serverConfig()).thenReturn(cruiseConfig.server());

        Username user = new Username("user");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        adminsConfigService.bulkUpdate(user, emptyList(), singletonList("someOtherUser"), emptyList(), emptyList(), "md5", result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(404));

        verify(goConfigService, times(0)).updateConfig(any(), any());
    }
}
