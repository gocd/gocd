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

package com.thoughtworks.go.config.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentAgentConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.ListUtil;

/**
 * @understands ensuring environment-agents refer to existing, enabled agents
 */
public class EnvironmentAgentValidator implements GoConfigValidator {
    public void validate(CruiseConfig cruiseConfig) throws Exception {
        List<ConfigErrors> errors = validateConfig(cruiseConfig);
        List<String> errorMessages = new ArrayList<>();
        for (ConfigErrors error : errors) {
            errorMessages.addAll(error.getAll());
        }
        if (!errors.isEmpty()) throw new RuntimeException(ListUtil.join(errorMessages));
    }

    public List<ConfigErrors> validateConfig(CruiseConfig cruiseConfig) {
        List<ConfigErrors> errors = new ArrayList<>();
        Set<String> uuids = cruiseConfig.agents().acceptedUuids();
        if (!cruiseConfig.getEnvironments().validateContainOnlyUuids(uuids)) {
            for (EnvironmentConfig environmentConfig : cruiseConfig.getEnvironments()) {
                for (EnvironmentAgentConfig environmentAgentConfig : environmentConfig.getAgents()) {
                    if (!environmentAgentConfig.errors().isEmpty()) {
                        errors.add(environmentAgentConfig.errors());
                    }
                }
            }
        }
        return errors;
    }
}
