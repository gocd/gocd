package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class UpdatePipelineConfigCommandTest {

    private EntityHashingService entityHashingService;
    private GoConfigService goConfigService;
    private Username username;
    private LocalizedOperationResult localizedOperationResult;
    private PipelineConfig pipelineConfig;


    @Before
    public void setUp() throws Exception {
        entityHashingService = mock(EntityHashingService.class);
        goConfigService = mock(GoConfigService.class);
        username = mock(Username.class);
        localizedOperationResult = mock(LocalizedOperationResult.class);
        pipelineConfig = PipelineConfigMother.pipelineConfig("p1");
    }

    @Test
    public void shouldDisallowStaleRequest() {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, entityHashingService,
                pipelineConfig, username, "stale_md5", localizedOperationResult);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(pipelineConfig.name().toString(), username, localizedOperationResult, "group1")).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineConfig)).thenReturn("latest_md5");

        BasicCruiseConfig basicCruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig));
        assertFalse(command.canContinue(basicCruiseConfig));
    }

    @Test
    public void shouldDisallowUpdateIfPipelineEditIsDisAllowed() throws Exception {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, username, "stale_md5", localizedOperationResult);

        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");
        when(goConfigService.canEditPipeline(pipelineConfig.name().toString(),username,localizedOperationResult,"group1")).thenReturn(false);
        assertFalse(command.canContinue(mock(CruiseConfig.class)));
    }

    @Test
    public void shouldInvokeUpdateMethodOfCruiseConfig() throws Exception {
        UpdatePipelineConfigCommand command = new UpdatePipelineConfigCommand(goConfigService, null,
                pipelineConfig, username, "stale_md5", localizedOperationResult);

        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(goConfigService.findGroupNameByPipeline(pipelineConfig.name())).thenReturn("group1");

        command.update(cruiseConfig);
        verify(cruiseConfig).update("group1", pipelineConfig.name().toString(),pipelineConfig);
    }
}
