package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;

public interface PartialConfigUpdateCompletedListener {

    void onFailedPartialConfig(ConfigRepoConfig repoConfig, Exception ex);

    void onSuccessPartialConfig(ConfigRepoConfig repoConfig, PartialConfig newPart);


}
