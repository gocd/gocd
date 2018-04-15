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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CreatePipelineConfigCommandTest {

    private GoConfigService goConfigService;
    private Username username;
    private LocalizedOperationResult localizedOperationResult;
    private PipelineConfig pipelineConfig;


    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        username = mock(Username.class);
        localizedOperationResult = mock(LocalizedOperationResult.class);
        pipelineConfig = PipelineConfigMother.pipelineConfig("p1");
    }

    @Test
    public void shouldDisallowUpdateIfPipelineEditIsDisAllowed() throws Exception {
        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService, pipelineConfig, username, localizedOperationResult, "group1");

        PipelineGroups groups = mock(PipelineGroups.class);
        when(goConfigService.groups()).thenReturn(groups);
        when(groups.hasGroup("group1")).thenReturn(true);
        when(goConfigService.isUserAdminOfGroup(username.getUsername(), "group1")).thenReturn(false);

        assertFalse(command.canContinue(mock(CruiseConfig.class)));
    }

    @Test
    public void shouldDisallowCreationOfGroupAndPipelineForNonAdmins() {
        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService, pipelineConfig, username, localizedOperationResult, "group1");

        PipelineGroups groups = mock(PipelineGroups.class);
        when(goConfigService.groups()).thenReturn(groups);
        when(groups.hasGroup("group1")).thenReturn(false);
        when(goConfigService.isUserAdmin(username)).thenReturn(false);

        assertFalse(command.canContinue(mock(CruiseConfig.class)));
    }

    @Test
    public void shouldInvokeAddPipelineWithoutValidationMethodOfCruiseConfig() throws Exception {
        CreatePipelineConfigCommand command = new CreatePipelineConfigCommand(goConfigService, pipelineConfig, username, localizedOperationResult, "group1");

        CruiseConfig cruiseConfig = mock(CruiseConfig.class);

        command.update(cruiseConfig);
        verify(cruiseConfig).addPipelineWithoutValidation("group1", pipelineConfig);
    }

    @Test
    public void shouldSetOriginOnGivenPipelineConfig() {
        CreatePipelineConfigCommand createPipelineConfigCommand = new CreatePipelineConfigCommand(goConfigService, pipelineConfig, username, localizedOperationResult, "group1");
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(cruiseConfig.getPipelineConfigByName(pipelineConfig.name())).thenReturn(pipelineConfig);
        createPipelineConfigCommand.postValidationUpdates(cruiseConfig);

        assertThat(pipelineConfig.getOrigin(), is(new FileConfigOrigin()));
    }
}