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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AdminsConfigUpdateCommandTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private LocalizedOperationResult result;

    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        entityHashingService = mock(EntityHashingService.class);
    }

    @Test
    public void shouldUpdateThePreprocessedCruiseConfigAdminsConfig() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("user")));

        AdminsConfigUpdateCommand command = new AdminsConfigUpdateCommand(goConfigService, adminsConfig, currentUser, result, entityHashingService, null);

        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().security().adminsConfig(), is(adminsConfig));
    }

    @Test
    public void shouldNotContinueIfUserIsNotAnAdmin() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        AdminsConfigUpdateCommand command = new AdminsConfigUpdateCommand(goConfigService, new AdminsConfig(), currentUser, result, entityHashingService, null);

        assertFalse(command.canContinue(cruiseConfig));
        assertThat(result.httpCode(), is(403));
    }

    @Test
    public void shouldNotContinueIfRequestIsStale() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("user")));
        String digest = "digest";

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(entityHashingService.hashForEntity(cruiseConfig.server().security().adminsConfig())).thenReturn(digest);

        AdminsConfigUpdateCommand command = new AdminsConfigUpdateCommand(goConfigService, adminsConfig, currentUser, result, entityHashingService, "stale_digest");

        assertFalse(command.canContinue(cruiseConfig));
        assertThat(result.httpCode(), is(412));
    }

    @Test
    public void canContinue_adminUserShouldBeAbleToUpdateAFreshRequest() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("user")));
        String digest = "digest";

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(entityHashingService.hashForEntity(cruiseConfig.server().security().adminsConfig())).thenReturn(digest);

        AdminsConfigUpdateCommand command = new AdminsConfigUpdateCommand(goConfigService, adminsConfig, currentUser, result, entityHashingService, "digest");

        assertTrue(command.canContinue(cruiseConfig));
    }
    @Test
    public void isValid_shouldValidateTheAdminsConfig() {
        AdminsConfig adminsConfig = mock(AdminsConfig.class);
        cruiseConfig.server().security().setAdminsConfig(adminsConfig);
        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);

        AdminsConfigUpdateCommand command = new AdminsConfigUpdateCommand(goConfigService, null, currentUser, result, entityHashingService, "digest");
        command.isValid(cruiseConfig);

        verify(adminsConfig).validateTree(validationContext);
    }

    @Test
    public void isValid_shouldEnsureTheAdminsConfigHasErrorsIfValidationFails() {
        AdminsConfig adminsInPreprocessedConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("")));
        AdminsConfig adminsConfigRequest = new AdminsConfig(new AdminUser(new CaseInsensitiveString("")));
        cruiseConfig.server().security().setAdminsConfig(adminsInPreprocessedConfig);

        AdminsConfigUpdateCommand command = new AdminsConfigUpdateCommand(goConfigService, adminsConfigRequest, currentUser, result, entityHashingService, "digest");

        assertFalse(command.isValid(cruiseConfig));
        TestCase.assertTrue(adminsConfigRequest.hasErrors());
        Assert.assertThat(adminsConfigRequest.errors().on("users"), is("User cannot be blank."));
    }

}
