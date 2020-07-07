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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.util.SystemEnvironment;

/**
 * @understands: ensures serverId is never changed
 */
public class ServerIdImmutabilityValidator implements GoConfigValidator {
    private String serverId;
    private SystemEnvironment env;

    public ServerIdImmutabilityValidator() {
        env = new SystemEnvironment();
    }

    @Override
    public void validate(CruiseConfig cruiseConfig) {
        ServerConfig server = cruiseConfig.server();
        String newServerId = server.getServerId();
        if (serverId == null) {
            serverId = newServerId;
        }

        if (serverId == null || serverId.equals(newServerId) || ! env.enforceServerImmutability()) {
            return;
        }

        throw new RuntimeException("The value of 'serverId' uniquely identifies a Go server instance. This field cannot be modified.");
    }

    public String getServerId() {
        return serverId;
    }
}
