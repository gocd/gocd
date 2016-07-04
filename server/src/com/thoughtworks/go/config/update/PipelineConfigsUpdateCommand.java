package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class PipelineConfigsUpdateCommand implements EntityConfigUpdateCommand<PipelineConfigs> {
    private final GoConfigService goConfigService;
    private final UpdateConfigCommand command;
    private final HttpLocalizedOperationResult result;
    private final Username currentUser;
    private final String groupName;
    private PipelineConfigs updatedConfig;

    public PipelineConfigsUpdateCommand(GoConfigService goConfigService, UpdateConfigCommand command, HttpLocalizedOperationResult result, Username currentUser, String groupName) {
        this.goConfigService = goConfigService;
        this.command = command;
        this.result = result;
        this.currentUser = currentUser;
        this.groupName = groupName;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        command.update(preprocessedConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        updatedConfig = preprocessedConfig.findGroup(groupName);
        updatedConfig.validate(ConfigSaveValidationContext.forChain(preprocessedConfig));
        if(updatedConfig.errors().isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(updatedConfig);
    }

    @Override
    public PipelineConfigs getPreprocessedEntityConfig() {
        return updatedConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return goConfigService.canEditPipelineGroup(groupName, currentUser, result);
    }
}
