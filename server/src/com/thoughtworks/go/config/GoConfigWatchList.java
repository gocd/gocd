package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a list of configuration repositories.
 */
@Component
public class GoConfigWatchList implements ConfigChangedListener {
    private static final Logger LOGGER = Logger.getLogger(GoConfigWatchList.class);

    private CachedFileGoConfig fileGoConfig;

    private List<ChangedRepoConfigWatchListListener> listeners = new ArrayList<ChangedRepoConfigWatchListListener>();
    private ConfigReposConfig reposConfig;

    @Autowired public GoConfigWatchList(CachedFileGoConfig fileGoConfig) {

        this.fileGoConfig = fileGoConfig;
        this.reposConfig  = fileGoConfig.currentConfig().getConfigRepos();
    }

    public ConfigReposConfig getCurrentConfigRepos()
    {
        return reposConfig;
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        ConfigReposConfig partSources = newCruiseConfig.getConfigRepos();

        this.reposConfig = partSources;
        notifyListeners(partSources);
    }
    private synchronized void notifyListeners(ConfigReposConfig configRepoConfigs) {
        for (ChangedRepoConfigWatchListListener listener : listeners) {
            try {
                listener.onChangedRepoConfigWatchList(configRepoConfigs);
            } catch (Exception e) {
                LOGGER.error("failed to fire config repos list changed event for listener: " + listener, e);
            }
        }
    }

    public boolean hasConfigRepoWithFingerprint(String fingerprint) {
        return this.reposConfig.hasMaterialWithFingerprint(fingerprint);
    }

    public ConfigRepoConfig getConfigRepoForMaterial(MaterialConfig material) {
        ConfigRepoConfig repo = this.reposConfig.getConfigRepo(material);
        return  repo;
    }

    public void registerListener(ChangedRepoConfigWatchListListener listener) {
        listeners.add(listener);
    }
}
