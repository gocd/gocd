package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.materials.Material;
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
    private static final Logger LOGGER = Logger.getLogger(CachedGoConfig.class);

    private CachedFileGoConfig fileGoConfig;

    private List<ChangedRepoConfigWatchListListener> listeners = new ArrayList<ChangedRepoConfigWatchListListener>();

    @Autowired public GoConfigWatchList(CachedFileGoConfig fileGoConfig) {

        this.fileGoConfig = fileGoConfig;
    }

    public ConfigReposConfig getCurrentConfigRepos()
    {
        return  null;
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        ConfigReposConfig partSources = null;//TODO

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
        return false;
    }

    public ConfigRepoConfig getConfigRepoForMaterial(Material material) {
        return null;
    }

    public void registerListener(ChangedRepoConfigWatchListListener listener) {
        listeners.add(listener);
    }
}
