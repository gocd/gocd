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

    private final GoFileConfigDataSource dataSource;

    @Autowired
    public CachedFileGoConfig(GoFileConfigDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GoConfigHolder loadFromDisk() throws Exception {
        return dataSource.load();
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

    public String getFileLocation() {
        return dataSource.getFileLocation();
    }

    public synchronized GoConfigHolder save(String configFileContent, boolean shouldMigrate) throws Exception {
        return dataSource.write(configFileContent, shouldMigrate);
    }
}
