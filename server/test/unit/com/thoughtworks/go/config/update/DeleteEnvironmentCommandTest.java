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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeleteEnvironmentCommandTest {
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
        cruiseConfig.addEnvironment(environmentConfig);
        actionFailed = LocalizedMessage.string("ENV_DELETE_FAILED", environmentName);
    }

    @Test
    public void shouldDeleteTheSpecifiedEnvironment() throws Exception {
        DeleteEnvironmentCommand command = new DeleteEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        assertTrue(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
        command.update(cruiseConfig);
        assertFalse(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnEnvironments() throws Exception {
        DeleteEnvironmentCommand command = new DeleteEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        assertThat(command.canContinue(cruiseConfig), is(false));
        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(401));
        assertThat(result.toString(), containsString("NO_PERMISSION_TO_DELETE_ENVIRONMENT"));
        assertThat(result.toString(), containsString(currentUser.getUsername().toString()));
    }
}
