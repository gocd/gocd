package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.validation.ConfigUpdateValidator;

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
        return true;
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
