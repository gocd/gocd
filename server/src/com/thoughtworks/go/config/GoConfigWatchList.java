package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.listener.ConfigChangedListener;
import org.springframework.stereotype.Component;

/**
 * Provides a list of configuration repositories.
 */
@Component
public class GoConfigWatchList implements ConfigChangedListener {



    public ConfigReposConfig getCurrentConfigRepos()
    {
        return  null;
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        ConfigReposConfig partSources;

    }

    public boolean hasConfigRepoWithFingerprint(String fingerprint) {
        return false;
    }

    public ConfigRepoConfig getConfigRepoForMaterial(Material material) {
        return null;
    }
}
