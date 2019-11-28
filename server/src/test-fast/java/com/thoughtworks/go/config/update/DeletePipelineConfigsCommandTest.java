/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeletePipelineConfigsCommandTest {

    @Mock
    private SecurityService securityService;

    private PipelineConfigs pipelineConfigs;
    private Username user;
    private BasicCruiseConfig cruiseConfig;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);
        pipelineConfigs = new BasicPipelineConfigs("group", new Authorization());
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.setGroup(new PipelineGroups(pipelineConfigs));
        user = new Username(new CaseInsensitiveString("user"));
    }

    @Test
    public void commandShouldDeletePipelineGroupWhenEmpty() throws Exception {
        DeletePipelineConfigsCommand command = new DeletePipelineConfigsCommand(pipelineConfigs, new HttpLocalizedOperationResult(), user, securityService);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.hasPipelineGroup(pipelineConfigs.getGroup()), is(false));
    }

    @Test
    public void commandShouldNotContinue_whenDeletingNonEmptyPipelineGroup() throws Exception {
        pipelineConfigs.add(new PipelineConfig());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        DeletePipelineConfigsCommand command = new DeletePipelineConfigsCommand(pipelineConfigs, result, user, securityService);

        command.canContinue(cruiseConfig);

        assertThat(result.httpCode(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(result.message(), is("Failed to delete group group because it was not empty."));
    }

    @Test
    public void commandShouldBeValid() {
        DeletePipelineConfigsCommand command = new DeletePipelineConfigsCommand(pipelineConfigs, new HttpLocalizedOperationResult(), user, securityService);
        assertThat(command.isValid(cruiseConfig), is(true));
    }

    @Test
    public void commandShouldContinue_whenUserIsAuthorized() {
        when(securityService.isUserAdminOfGroup(user, "group")).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        DeletePipelineConfigsCommand command = new DeletePipelineConfigsCommand(pipelineConfigs, result, user, securityService);

        assertTrue(command.canContinue(cruiseConfig));
    }

    @Test
    public void commandShouldNotContinue_whenUserUnauthorized() {
        when(securityService.isUserAdminOfGroup(user, "group")).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        DeletePipelineConfigsCommand command = new DeletePipelineConfigsCommand(pipelineConfigs, result, user, securityService);

        assertFalse(command.canContinue(cruiseConfig));
        assertThat(result.httpCode(), is(HttpStatus.SC_FORBIDDEN));
    }
}
