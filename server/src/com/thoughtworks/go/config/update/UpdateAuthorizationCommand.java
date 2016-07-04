package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.UpdateConfigCommand;

public class UpdateAuthorizationCommand implements UpdateConfigCommand{
    private final String groupName;
    private final Authorization authorization;

    public UpdateAuthorizationCommand(String groupName, Authorization authorization) {
        this.groupName = groupName;
        this.authorization = authorization;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
        PipelineConfigs pipelineConfigs = cruiseConfig.findGroup(groupName);
        pipelineConfigs.setAuthorization(authorization);
        cruiseConfig.updateGroup(pipelineConfigs, groupName);
        return cruiseConfig;
    }
}
