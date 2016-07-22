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

public class AddEnvironmentCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private BasicEnvironmentConfig environmentConfig;
    private CaseInsensitiveString environmentName;
    private HttpLocalizedOperationResult result;
    private Localizable.CurryableLocalizable actionFailed;

    @Mock
    private EnvironmentConfigService environmentConfigService;

    @Mock
    private GoConfigService goConfigService;


    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        environmentName = new CaseInsensitiveString("Dev");
        environmentConfig = new BasicEnvironmentConfig(environmentName);
        result = new HttpLocalizedOperationResult();
        actionFailed = LocalizedMessage.string("ENV_ADD_FAILED");
    }

    @Test
    public void shouldAddTheSpecifiedEnvironment() throws Exception {
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
    }

    @Test
    public void shouldValidateInvalidAgentUUID() throws Exception {
        environmentConfig.addAgent("Invalid-agent-uuid");
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest(actionFailed.addParam("Environment 'Dev' has an invalid agent uuid 'Invalid-agent-uuid'"));

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateInvalidPipelineName() throws Exception {
        environmentConfig.addPipeline(new CaseInsensitiveString("Invalid-pipeline-name"));
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest(actionFailed.addParam("Environment 'Dev' refers to an unknown pipeline 'Invalid-pipeline-name'."));

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateDuplicateEnvironmentVariables() throws Exception {
        environmentConfig.addEnvironmentVariable("foo", "bar");
        environmentConfig.addEnvironmentVariable("foo", "baz");
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest(actionFailed.addParam("Environment Variable name 'foo' is not unique for environment 'Dev'."));

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnEnvironments() throws Exception {
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("NO_PERMISSION_TO_ADD_ENVIRONMENT", currentUser.getDisplayName()), HealthStateType.unauthorised());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueIfEnvironmentWithSameNameAlreadyExists() throws Exception {
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        when(goConfigService.hasEnvironmentNamed(environmentName)).thenReturn(true);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.conflict(LocalizedMessage.string("CANNOT_ADD_ENV_ALREADY_EXISTS", environmentName));

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }
}