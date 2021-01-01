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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.service.ConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigRepositoryValidatorService implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigRepositoryValidatorService.class);
    private ConfigRepository configRepository;

    @Autowired
    public ConfigRepositoryValidatorService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public void afterPropertiesSet() {
        if (configRepository.isRepositoryCorrupted()) {
            LOG.error("[FAILURE] Go Server failed to start as its configuration history store is corrupt. Please contact support@thoughtworks.com");
            shutDownServer();
        }
    }

    void shutDownServer() {
        new Thread(() -> System.exit(1)).start();
    }

    public void destroy() {

    }
}
