package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class UpdatePipelineConfigCommand implements EntityConfigUpdateCommand<PipelineConfig> {
    private final GoConfigService goConfigService;
    private final EntityHashingService entityHashingService;
    private final PipelineConfig pipelineConfig;
    private final Username currentUser;
    private final String md5;
    private final LocalizedOperationResult result;
    public String group;
    private PipelineConfig preprocessedPipelineConfig;

    public UpdatePipelineConfigCommand(GoConfigService goConfigService, EntityHashingService entityHashingService, PipelineConfig pipelineConfig,
                                       Username currentUser, String md5, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.entityHashingService = entityHashingService;
        this.pipelineConfig = pipelineConfig;
        this.currentUser = currentUser;
        this.md5 = md5;
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
        preprocessedPipelineConfig = preprocessedConfig.getPipelineConfigByName(pipelineConfig.name());
        boolean isValid = preprocessedPipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(false, getPipelineGroup(), preprocessedConfig, preprocessedPipelineConfig));
        if (!isValid) BasicCruiseConfig.copyErrors(preprocessedPipelineConfig, pipelineConfig);
        return isValid;
    }

    @Override
    public PipelineConfig getPreprocessedEntityConfig() {
        return preprocessedPipelineConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return canEditPipeline() && isRequestFresh();
    }

    private boolean canEditPipeline() {
        return goConfigService.canEditPipeline(pipelineConfig.name().toString(), currentUser, result, getPipelineGroup());
    }

    private boolean isRequestFresh() {
        boolean freshRequest = entityHashingService.md5ForPipelineConfig(pipelineConfig.name().toString()).equals(md5);

        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_PIPELINE_CONFIG", pipelineConfig.name().toString()));
        }

        return freshRequest;
    }
}

