/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.server.service.GoConfigService.INVALID_CRUISE_CONFIG_XML;

/**
 * @understands when to reload the config file
 */
@Component
public class CachedFileGoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedFileGoConfig.class);

    private final GoFileConfigDataSource dataSource;
    private final ServerHealthService serverHealthService;
    private volatile Exception lastException;

    @Autowired public CachedFileGoConfig(GoFileConfigDataSource dataSource, ServerHealthService serverHealthService) {
        this.dataSource = dataSource;
        this.serverHealthService = serverHealthService;
    }

    public GoConfigHolder loadFromDisk() {
        try {
            return dataSource.load();
        } catch (Exception e) {
            LOGGER.warn("Error loading cruise-config.xml from disk, keeping previous one", e);
            saveConfigError(e);
            return null;
        }
    }

    static class PipelineConfigSaveResult {
        private PipelineConfig pipelineConfig;
        private String group;
        private GoConfigHolder configHolder;

        public PipelineConfigSaveResult(PipelineConfig pipelineConfig, String group, GoConfigHolder configHolder) {
            this.pipelineConfig = pipelineConfig;
            this.group = group;
            this.configHolder = configHolder;
        }

        public PipelineConfig getPipelineConfig() {
            return pipelineConfig;
        }

        public String getGroup() {
            return group;
        }

        public GoConfigHolder getConfigHolder() {
            return configHolder;
        }
    }

    public synchronized GoFileConfigDataSource.GoConfigSaveResult writeWithLock(UpdateConfigCommand updateConfigCommand, GoConfigHolder holder) {
        return dataSource.writeWithLock(updateConfigCommand, holder);
    }

    public synchronized PipelineConfigSaveResult writePipelineWithLock(PipelineConfig pipelineConfig, GoConfigHolder serverCopy, PipelineConfigService.SaveCommand saveCommand, Username currentUser) {
        return dataSource.writePipelineWithLock(pipelineConfig, serverCopy, saveCommand, currentUser);
    }

    private synchronized void saveConfigError(Exception e) {
        this.lastException = e;
        ServerHealthState state = ServerHealthState.error(INVALID_CRUISE_CONFIG_XML, GoConfigValidity.invalid(e).errorMessage(), invalidConfigType());
        serverHealthService.update(state);
    }

    private static HealthStateType invalidConfigType() {
        return HealthStateType.invalidConfig();
    }

    public String getFileLocation() {
        return dataSource.getFileLocation();
    }

    public synchronized GoConfigHolder saveNew(String configFileContent, boolean shouldMigrate) throws Exception {
        return dataSource.write(configFileContent, shouldMigrate);
    }

    public GoConfigValidity checkConfigFileValid() {
        Exception ex = lastException;
        if (ex != null) {
            return GoConfigValidity.invalid(ex);
        }
        return GoConfigValidity.valid();
    }
}
