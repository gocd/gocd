/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.util.SystemEnvironment;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Understands: ensures serverId is never changed
 */
public class ServerIdImmutabilityValidator implements GoConfigValidator {
    private final SystemEnvironment env;
    private final AtomicReference<String> initialIdHolder = new AtomicReference<>();

    public ServerIdImmutabilityValidator() {
        env = new SystemEnvironment();
    }

    @Override
    public void validate(CruiseConfig cruiseConfig) {
        String newServerId = cruiseConfig.server().getServerId();
        if (initialIdHolder.compareAndSet(null, newServerId)) {
            return;
        }

        String initialId = initialIdHolder.get();
        if (!Objects.equals(initialId, newServerId) && env.enforceServerImmutability()) {
            throw new RuntimeException(String.format("The value of 'serverId' uniquely identifies a Go server instance. This field cannot be modified (attempting to change from [%s] to [%s]).", initialId, newServerId));
        }
    }

    public String getInitialServerId() {
        return initialIdHolder.get();
    }
}
