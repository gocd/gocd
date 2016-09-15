/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateEnvironmentCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private BasicEnvironmentConfig oldEnvironmentConfig;
    private BasicEnvironmentConfig newEnvironmentConfig;
    private CaseInsensitiveString oldEnvironmentName;
    private CaseInsensitiveString newEnvironmentName;
    private HttpLocalizedOperationResult result;
    private Localizable.CurryableLocalizable actionFailed;
    private String md5;

    @Mock
    private EnvironmentConfigService environmentConfigService;

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private EntityHashingService entityHashingService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        oldEnvironmentName = new CaseInsensitiveString("Dev");
        newEnvironmentName = new CaseInsensitiveString("Test");
        oldEnvironmentConfig = new BasicEnvironmentConfig(oldEnvironmentName);
        newEnvironmentConfig = new BasicEnvironmentConfig(newEnvironmentName);
        result = new HttpLocalizedOperationResult();
        md5 = "md5";
        cruiseConfig.addEnvironment(oldEnvironmentConfig);
        actionFailed = LocalizedMessage.string("ENV_UPDATE_FAILED", oldEnvironmentConfig.name());
    }

    @Test
    public void shouldUpdateTheSpecifiedEnvironment() throws Exception {
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig, newEnvironmentConfig, currentUser, actionFailed, md5, entityHashingService, result);

        assertFalse(cruiseConfig.getEnvironments().hasEnvironmentNamed(newEnvironmentName));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().hasEnvironmentNamed(newEnvironmentName));
    }

    @Test
    public void shouldValidateInvalidAgentUUID() throws Exception {
        newEnvironmentConfig.addAgent("Invalid-agent-uuid");
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig, newEnvironmentConfig, currentUser, actionFailed, md5, entityHashingService, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.badRequest(actionFailed.addParam("Environment 'Test' has an invalid agent uuid 'Invalid-agent-uuid'"));

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldValidateInvalidPipelines() throws Exception {
        newEnvironmentConfig.addPipeline(new CaseInsensitiveString("Invalid-pipeline-name"));
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig, newEnvironmentConfig, currentUser, actionFailed, md5, entityHashingService, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.badRequest(actionFailed.addParam("Environment 'Test' refers to an unknown pipeline 'Invalid-pipeline-name'."));

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldValidateDuplicateEnvironmentVariables() throws Exception {
        newEnvironmentConfig.addEnvironmentVariable("foo", "bar");
        newEnvironmentConfig.addEnvironmentVariable("foo", "baz");
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig, newEnvironmentConfig, currentUser, actionFailed, md5, entityHashingService, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.badRequest(actionFailed.addParam("Environment Variable name 'foo' is not unique for environment 'Test'."));

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(result, is(expectResult));

    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnEnvironments() throws Exception {
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig, newEnvironmentConfig, currentUser, actionFailed, md5, entityHashingService, result);
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(false);
        assertThat(command.canContinue(cruiseConfig), is(false));
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        Localizable noPermission = LocalizedMessage.string("NO_PERMISSION_TO_UPDATE_ENVIRONMENT", oldEnvironmentConfig.name().toString(), currentUser.getDisplayName());
        expectResult.unauthorized(noPermission, HealthStateType.unauthorised());

        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldNotContinueIfTheUserSubmittedStaleEtag() throws Exception {
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig, newEnvironmentConfig, currentUser, actionFailed, md5, entityHashingService, result);
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
        when(entityHashingService.md5ForEntity(oldEnvironmentConfig)).thenReturn("foo");
        assertThat(command.canContinue(cruiseConfig), is(false));
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Environment", oldEnvironmentConfig.name()));

        assertThat(result, is(expectResult));
    }
}
