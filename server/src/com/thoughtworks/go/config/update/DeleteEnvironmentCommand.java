package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.server.domain.Username;

public class DeleteEnvironmentCommand implements UpdateConfigCommand, UserAware {
    private EnvironmentConfig environmentConfig;
    private Username username;

    public DeleteEnvironmentCommand(EnvironmentConfig environmentConfig, Username username) {
        this.environmentConfig = environmentConfig;
        this.username = username;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
        cruiseConfig.getEnvironments().remove(environmentConfig);
        return cruiseConfig;
    }

    @Override
    public ConfigModifyingUser user() {
        return new ConfigModifyingUser(username);
    }
}
