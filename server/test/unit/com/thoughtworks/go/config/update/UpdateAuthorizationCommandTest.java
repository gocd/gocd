package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateAuthorizationCommandTest {
    @Test
    public void shouldUpdateAuthorizationForGroup() throws Exception {
        PipelineConfigs pipelineConfigs = mock(PipelineConfigs.class);
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        String groupName = "groupName";
        Authorization authorization = mock(Authorization.class);
        UpdateAuthorizationCommand updateAuthorizationCommand = new UpdateAuthorizationCommand(groupName, authorization);
        when(cruiseConfig.findGroup(groupName)).thenReturn(pipelineConfigs);

        updateAuthorizationCommand.update(cruiseConfig);
        verify(pipelineConfigs).setAuthorization(authorization);
        verify(cruiseConfig).updateGroup(pipelineConfigs, groupName);
    }
}