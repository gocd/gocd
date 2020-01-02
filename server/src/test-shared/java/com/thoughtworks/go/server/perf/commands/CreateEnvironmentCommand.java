/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.perf.commands;

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import java.util.Optional;

public class CreateEnvironmentCommand extends AgentPerformanceCommand {
    private final GoConfigService goConfigService;
    private final String envName;

    public CreateEnvironmentCommand(GoConfigService goConfigService, String envName) {
        this.goConfigService = goConfigService;
        this.envName = envName;
    }

    @Override
    Optional<String> execute() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        BasicEnvironmentConfig envConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(envName));
        goConfigService.addEnvironment(envConfig);
        if (!result.isSuccessful()) {
            throw new RuntimeException(result.message());
        }
        return Optional.of(envName);
    }
}
