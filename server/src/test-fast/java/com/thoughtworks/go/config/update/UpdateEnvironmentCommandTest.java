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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.AllConfigErrors;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
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
    private String actionFailed;
    private String digest;

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private EntityHashingService entityHashingService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        oldEnvironmentName = new CaseInsensitiveString("Dev");
        newEnvironmentName = new CaseInsensitiveString("Test");
        oldEnvironmentConfig = new BasicEnvironmentConfig(oldEnvironmentName);
        newEnvironmentConfig = new BasicEnvironmentConfig(newEnvironmentName);
        result = new HttpLocalizedOperationResult();
        digest = "digest";
        cruiseConfig.addEnvironment(oldEnvironmentConfig);
        actionFailed = "Could not update environment '" + oldEnvironmentConfig.name() + "'.";
    }

    @Test
    public void shouldUpdateTheSpecifiedEnvironment() throws Exception {
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig.name().toString(), newEnvironmentConfig, currentUser, actionFailed, digest, entityHashingService, result);

        assertFalse(cruiseConfig.getEnvironments().hasEnvironmentNamed(newEnvironmentName));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().hasEnvironmentNamed(newEnvironmentName));
    }

    @Test
    public void shouldValidateInvalidPipelines() throws Exception {
        newEnvironmentConfig.addPipeline(new CaseInsensitiveString("Invalid-pipeline-name"));
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig.name().toString(), newEnvironmentConfig, currentUser, actionFailed, digest, entityHashingService, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.unprocessableEntity(actionFailed + " Environment 'Test' refers to an unknown pipeline 'Invalid-pipeline-name'.");

        assertThat(command.isValid(cruiseConfig), is(false));
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldValidateDuplicateEnvironmentVariables() throws Exception {
        newEnvironmentConfig.addEnvironmentVariable("foo", "bar");
        newEnvironmentConfig.addEnvironmentVariable("foo", "baz");
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig.name().toString(), newEnvironmentConfig, currentUser, actionFailed, digest, entityHashingService, result);
        command.update(cruiseConfig);

        assertThat(command.isValid(cruiseConfig), is(false));

        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        String allErrors = new AllConfigErrors(cruiseConfig.getAllErrors()).asString();
        expectResult.unprocessableEntity(actionFailed + " " + allErrors);

        assertThat(result, is(expectResult));

    }

    @Test
    public void shouldNotContinueIfTheUserSubmittedStaleEtag() throws Exception {
        UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig.name().toString(), newEnvironmentConfig, currentUser, actionFailed, digest, entityHashingService, result);
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
        when(entityHashingService.hashForEntity(oldEnvironmentConfig)).thenReturn("foo");
        assertThat(command.canContinue(cruiseConfig), is(false));
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.stale(EntityType.Environment.staleConfig(oldEnvironmentName));

        assertThat(result, is(expectResult));
    }
}
