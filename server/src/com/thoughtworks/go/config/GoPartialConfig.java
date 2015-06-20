package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * @understands current state of configuration part.
 *
 * Provides partial configurations.
 */
@Component
public class GoPartialConfig implements PartialConfigUpdateCompletedListener, ChangedRepoConfigWatchListListener {

    private static final Logger LOGGER = Logger.getLogger(GoPartialConfig.class);

    private GoRepoConfigDataSource repoConfigDataSource;
    private GoConfigWatchList configWatchList;

    private List<PartialConfigChangedListener> listeners = new ArrayList<PartialConfigChangedListener>();
    // last, ready partial configs
    private Map<String,PartialConfig> fingerprintToLatestValidConfigMap = new ConcurrentHashMap<String,PartialConfig>();

    @Autowired public  GoPartialConfig(GoRepoConfigDataSource repoConfigDataSource,
                                       GoConfigWatchList configWatchList) {
        this.repoConfigDataSource = repoConfigDataSource;
        this.configWatchList = configWatchList;

        this.configWatchList.registerListener(this);
        this.repoConfigDataSource.registerListener(this);
    }

    public void registerListener(PartialConfigChangedListener listener) {
        this.listeners.add(listener);
    }

    public boolean hasListener(PartialConfigChangedListener listener) {
        return this.listeners.contains(listener);
    }

    private void notifyListeners() {
        PartialConfig[] partials = this.lastPartials();
        for(PartialConfigChangedListener listener : listeners)
        {
            listener.onPartialConfigChanged(partials);
        }
    }

    public PartialConfig[] lastPartials() {
        return fingerprintToLatestValidConfigMap.values().toArray(new PartialConfig[0]);
    }


    @Override
    public void onFailedPartialConfig(ConfigRepoConfig repoConfig, Exception ex) {
        // do nothing here, we keep previous version of part.
        // As an addition we should stop scheduling pipelines defined in that old part.
    }

    @Override
    public synchronized void onSuccessPartialConfig(ConfigRepoConfig repoConfig, PartialConfig newPart) {
        String fingerprint = repoConfig.getMaterialConfig().getFingerprint();
        if(this.configWatchList.hasConfigRepoWithFingerprint(fingerprint))
        {
            //TODO maybe validate new part without context of other partials or main config

            // put latest valid
            fingerprintToLatestValidConfigMap.put(fingerprint, newPart);

            notifyListeners();
        }
    }

    @Override
    public synchronized void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos) {
        boolean removedAny = false;
        // remove partial configs from map which are no longer on the list
        for(String fingerprint : this.fingerprintToLatestValidConfigMap.keySet())
        {
            if(!newConfigRepos.hasMaterialWithFingerprint(fingerprint))
            {
                this.fingerprintToLatestValidConfigMap.remove(fingerprint);
                removedAny = true;
            }
        }
        //fire event about changed partials collection
        this.notifyListeners();
    }

}
