package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PipelineConfigsUpdateCommandTest {

    GoConfigService goConfigService;
    UpdateAuthorizationCommand updateAuthorizationCommand;
    HttpLocalizedOperationResult result;
    Username currentUser;
    String groupName;
    PipelineConfigsUpdateCommand pipelineConfigsUpdateCommand;
    CruiseConfig cruiseConfig;
    ConfigErrors configErrors;

    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        updateAuthorizationCommand = mock(UpdateAuthorizationCommand.class);
        result = mock(HttpLocalizedOperationResult.class);
        currentUser = new Username(new CaseInsensitiveString("userName"));
        groupName = "groupName";
        pipelineConfigsUpdateCommand = new PipelineConfigsUpdateCommand(goConfigService, updateAuthorizationCommand, result, currentUser, groupName);
        cruiseConfig = mock(CruiseConfig.class);
        configErrors = new ConfigErrors();
    }

    @Test
    public void shouldReturnTrueIfGroupExistsAndUserIsAdmin() throws Exception {
        when(goConfigService.canEditPipelineGroup(groupName, currentUser, result)).thenReturn(true);
        assertThat(true, is(pipelineConfigsUpdateCommand.canContinue(cruiseConfig)));
    }

    @Test
    public void shouldInvokeUpdateMethodOfUpdateAuthorizationCommand() throws Exception {
        pipelineConfigsUpdateCommand.update(cruiseConfig);
        verify(updateAuthorizationCommand).update(cruiseConfig);
    }

    @Test
    public void shouldReturnTrueIfConfigIsValid() throws Exception {
        PipelineConfigs pipelineConfigs = mock(PipelineConfigs.class);
        when(cruiseConfig.findGroup(groupName)).thenReturn(pipelineConfigs);
        when(pipelineConfigs.errors()).thenReturn(configErrors);
        assertThat(true, is(pipelineConfigsUpdateCommand.isValid(cruiseConfig)));
    }

    @Test
    public void shouldReturnUpdatedPipelineConfigs() throws Exception {
        PipelineConfigs pipelineConfigs = mock(PipelineConfigs.class);
        when(cruiseConfig.findGroup(groupName)).thenReturn(pipelineConfigs);
        when(pipelineConfigs.errors()).thenReturn(configErrors);
        pipelineConfigsUpdateCommand.isValid(cruiseConfig);
        assertThat(pipelineConfigs, is(pipelineConfigsUpdateCommand.getPreprocessedEntityConfig()));
    }

    @Test
    public void shouldReturnFalseIfConfigIsInvalid() throws Exception {
        PipelineConfigs pipelineConfigs = mock(PipelineConfigs.class);
        when(cruiseConfig.findGroup(groupName)).thenReturn(pipelineConfigs);
        configErrors.add(groupName, "error");
        when(pipelineConfigs.errors()).thenReturn(configErrors);
        assertThat(false, is(pipelineConfigsUpdateCommand.isValid(cruiseConfig)));
    }
}