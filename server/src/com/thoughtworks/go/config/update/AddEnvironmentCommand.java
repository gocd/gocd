package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.server.domain.Username;

public class AddEnvironmentCommand implements UpdateConfigCommand, UserAware {
    private final BasicEnvironmentConfig environmentConfig;
    private Username user;

    public AddEnvironmentCommand(BasicEnvironmentConfig environmentConfig, Username user) {
        this.environmentConfig = environmentConfig;
        this.user = user;
    }

    public CruiseConfig update(CruiseConfig cruiseConfig) {
        cruiseConfig.addEnvironment(environmentConfig);
        return cruiseConfig;
    }

    @Override
    public ConfigModifyingUser user() {
        return new ConfigModifyingUser(user);
    }
}