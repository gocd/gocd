package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigReposConfig;

/**
 * Listens for changed list of configuration repositories
 */
public interface ChangedRepoConfigWatchListListener {
    void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos);
}
