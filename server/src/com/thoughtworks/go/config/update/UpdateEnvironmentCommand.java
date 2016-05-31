package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.server.domain.Username;

public class UpdateEnvironmentCommand implements UpdateConfigCommand, UserAware {

    private final String environmentToUpdate;
    private final EnvironmentConfig newEnvironmentConfig;
    private Username username;

    public UpdateEnvironmentCommand(String environmentToUpdate, EnvironmentConfig newEnvironmentConfig, Username username) {
        this.environmentToUpdate = environmentToUpdate;
        this.newEnvironmentConfig = newEnvironmentConfig;
        this.username = username;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
        EnvironmentsConfig environments = cruiseConfig.getEnvironments();
        EnvironmentConfig oldConfig = environments.named(new CaseInsensitiveString(environmentToUpdate));
        int index = environments.indexOf(oldConfig);
        environments.remove(index);
        environments.add(index, newEnvironmentConfig);
        return cruiseConfig;
    }

    @Override
    public ConfigModifyingUser user() {
        return new ConfigModifyingUser(username);
    }
}

