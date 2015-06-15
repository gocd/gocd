package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.materials.*;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.BuildType;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.NoCompatibleUpstreamRevisionsException;
import com.thoughtworks.go.server.service.NoModificationsPresentForDependentMaterialException;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Provides partial configurations.
 */
@Component
public class GoPartialConfig implements GoMessageListener<MaterialUpdateCompletedMessage> {

    private static final Logger LOGGER = Logger.getLogger(GoPartialConfig.class);

    private GoRepoConfigDataSource repoConfigDataSource;
    // we will use that to wait for all config repos to finish updates
    private MaterialUpdateStatusNotifier materialUpdateStatusNotifier;
    private MaterialConfigConverter materialConfigConverter;

    @Autowired public  GoPartialConfig(GoRepoConfigDataSource repoConfigDataSource,MaterialUpdateStatusNotifier materialUpdateStatusNotifier,
                                       MaterialConfigConverter materialConfigConverter,MaterialUpdateCompletedTopic topic) {
        this.repoConfigDataSource = repoConfigDataSource;
        this.materialUpdateStatusNotifier = materialUpdateStatusNotifier;
        this.materialConfigConverter = materialConfigConverter;

        topic.addListener(this);
    }



    @Override
    public void onMessage(MaterialUpdateCompletedMessage message)
    {
        message.getMaterial();
    }

}
