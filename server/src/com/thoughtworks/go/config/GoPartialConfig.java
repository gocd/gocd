package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.materials.*;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
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
public class GoPartialConfig implements PartialConfigUpdateCompletedListener {

    private static final Logger LOGGER = Logger.getLogger(GoPartialConfig.class);

    private GoRepoConfigDataSource repoConfigDataSource;
    private GoConfigWatchList configWatchList;

    private List<PartialConfigChangedListener> listeners = new ArrayList<PartialConfigChangedListener>();
    // last, ready partial configs
    private Map<String,PartialConfig> fingerprintLatestConfigMap = new ConcurrentHashMap<String,PartialConfig>();

    @Autowired public  GoPartialConfig(GoRepoConfigDataSource repoConfigDataSource,
                                       GoConfigWatchList configWatchList) {
        this.repoConfigDataSource = repoConfigDataSource;
        this.configWatchList = configWatchList;
    }

    public PartialConfig[] lastPartials() {
        return fingerprintLatestConfigMap.values().toArray(new PartialConfig[0]);
    }


    @Override
    public void onFailedPartialConfig(ConfigRepoConfig repoConfig, Exception ex) {
        // do nothing here, we keep previous version of part.
        // As an addition we should stop scheduling pipelines defined in that old part.
    }

    @Override
    public void onSuccessPartialConfig(ConfigRepoConfig repoConfig, PartialConfig newPart) {
        String fingerprint = repoConfig.getMaterialConfig().getFingerprint();
        if(this.configWatchList.hasConfigRepoWithFingerprint(fingerprint))
        {
            //TODO maybe validate new part without context of other partials or main config

            // put latest valid
            fingerprintLatestConfigMap.put(fingerprint,newPart);

            for(PartialConfigChangedListener listener : listeners)
            {
                listener.onPartialConfigChanged(this.lastPartials());
            }
        }
    }
}
