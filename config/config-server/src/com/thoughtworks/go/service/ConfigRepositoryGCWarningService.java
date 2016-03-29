/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.service;

import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigRepositoryGCWarningService {
    private final ConfigRepository configRepository;
    private final ServerHealthService serverHealthService;
    private final SystemEnvironment systemEnvironment;
    private static final String SCOPE = "GC";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepositoryGCWarningService.class.getName());

    @Autowired
    public ConfigRepositoryGCWarningService(ConfigRepository configRepository, ServerHealthService serverHealthService, SystemEnvironment systemEnvironment) {
        this.configRepository = configRepository;
        this.serverHealthService = serverHealthService;
        this.systemEnvironment = systemEnvironment;
    }

    public void checkRepoAndAddWarningIfRequired() {
        try {
            if (configRepository.getLooseObjectCount() >= systemEnvironment.get(SystemEnvironment.GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD)) {
                String message = "Action required: Run 'git gc' on config.git repo";
                String description = "Number of loose objects in your Configuration repository(config.git) has grown beyond " +
                        "the configured threshold. As the size of config repo increases, the config save operations tend to slow down " +
                        "drastically. It is recommended that you run 'git gc' from " +
                        "'&lt;go server installation directory&gt;/db/config.git/' to address this problem. Go can do this " +
                        "automatically on a periodic basis if you enable automatic GC. <a target='_blank' href='http://www.go.cd/documentation/user/current/advanced_usage/config_repo.html'>read more...</a>";

                serverHealthService.update(ServerHealthState.warningWithHtml(message, description, HealthStateType.general(HealthStateScope.forConfigRepo(SCOPE))));
                LOGGER.warn("{}:{}", message, description);
            } else {
                serverHealthService.removeByScope(HealthStateScope.forConfigRepo(SCOPE));

            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
