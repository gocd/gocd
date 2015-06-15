package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.materials.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses partial configurations and exposes latest configurations as soon as possible.
 */
@Component
public class GoRepoConfigDataSource implements ChangedRepoConfigWatchListListener {
    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;

    private Map<String,PartialConfig> fingerprintLatestConfigMap = new HashMap<String,PartialConfig>();

    @Autowired public GoRepoConfigDataSource(GoConfigWatchList configWatchList,GoConfigPluginService configPluginService)
    {
        this.configPluginService = configPluginService;
        this.configWatchList = configWatchList;
    }

    @Override
    public void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos)
    {
        // TODO remove partial configs from map which are no longer on the list

    }

    public void onPolledMaterial(Material material) {
        // called when pipelines/flyweight/[flyweight] has a clean checkout of latest material

        /* if this is material listed in config-repos
           Then ask for config plugin implementation
           Give it the directory and store partial config
           post event about completed (successful or not) parsing
         */

        String fingerprint = material.getFingerprint();
        if(this.configWatchList.hasConfigRepoWithFingerprint(fingerprint))
        {
            ConfigRepoConfig repoConfig = configWatchList.getConfigRepoForMaterial(material);
            PartialConfigProvider plugin = this.configPluginService.partialConfigProviderFor(repoConfig);

        }
    }
}
