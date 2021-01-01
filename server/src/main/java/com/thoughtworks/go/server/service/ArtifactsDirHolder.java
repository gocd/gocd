/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ArtifactsDirHolder implements ConfigChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsDirHolder.class);
    private final ServerHealthService serverHealthService;
    private GoConfigService goConfigService;
    private File artifactsDir;
    private File backupsDir;
    public static final String ARTIFACTS_ROOT_CHANGED_MESSAGE = "The change in the artifacts directory location will not take effect until the Go Server is restarted";
    private static final String ARTIFACTS_ROOT_CHANGED_DESC = "";
    static final HealthStateType ARTIFACTS_ROOT_CHANGE_HEALTH_STATE_TYPE = HealthStateType.artifactsDirChanged();

    @Autowired
    public ArtifactsDirHolder(ServerHealthService serverHealthService, GoConfigService goConfigService) {
        this.serverHealthService = serverHealthService;
        this.goConfigService = goConfigService;
    }

    public void initialize() {
        this.artifactsDir = goConfigService.artifactsDir();
        this.backupsDir = new File(artifactsDir, ServerConfig.SERVER_BACKUPS);
        goConfigService.register(this);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        ServerHealthState serverHealthState;
        if (isArtifactsDirChanged(newCruiseConfig)) {
            serverHealthState = ServerHealthState.warning(ARTIFACTS_ROOT_CHANGED_MESSAGE, ARTIFACTS_ROOT_CHANGED_DESC,
                    ARTIFACTS_ROOT_CHANGE_HEALTH_STATE_TYPE);
            LOGGER.info("[Configuration Changed] Artifacts directory was changed.");
        } else {
            serverHealthState = ServerHealthState.success(ARTIFACTS_ROOT_CHANGE_HEALTH_STATE_TYPE);
        }
        serverHealthService.update(serverHealthState);
    }

    private boolean isArtifactsDirChanged(CruiseConfig newCruiseConfig) {
        return !new File(newCruiseConfig.server().artifactsDir()).equals(artifactsDir);
    }

    public File getArtifactsDir() {
        return artifactsDir;
    }

    public File getBackupsDir() {
        return backupsDir;
    }
}
