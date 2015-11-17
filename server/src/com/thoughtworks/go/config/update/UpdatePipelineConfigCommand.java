package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class UpdatePipelineConfigCommand implements EntityConfigUpdateCommand<PipelineConfig> {
    private final GoConfigService goConfigService;
    private final PipelineConfig pipelineConfig;
    private final Username currentUser;
    private final LocalizedOperationResult result;
    public String group;

    public UpdatePipelineConfigCommand(GoConfigService goConfigService, PipelineConfig pipelineConfig, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.pipelineConfig = pipelineConfig;
        this.currentUser = currentUser;
        this.result = result;
    }

    private String getPipelineGroup() {
        if (group == null) {
            this.group = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
        }
        return group;
    }

    @Override
    public void update(CruiseConfig cruiseConfig) throws Exception {
        cruiseConfig.update(getPipelineGroup(), pipelineConfig.name().toString(), pipelineConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        PipelineConfig preprocessedPipelineConfig = preprocessedConfig.getPipelineConfigByName(pipelineConfig.name());
        boolean isValid = preprocessedPipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(false, getPipelineGroup(), preprocessedConfig, preprocessedPipelineConfig));
        if (!isValid) BasicCruiseConfig.copyErrors(preprocessedPipelineConfig, pipelineConfig);
        return isValid;
    }

    @Override
    public PipelineConfig getEntityConfig() {
        return pipelineConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return goConfigService.canEditPipeline(pipelineConfig.name().toString(), currentUser, result, getPipelineGroup());
    }
}

