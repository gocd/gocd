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
 * Understands: ensures tokenGenerationKey is never changed
 */
public class TokenGenerationKeyImmutabilityValidator implements GoConfigValidator {
    private final SystemEnvironment env;
    private final AtomicReference<String> initialKeyHolder = new AtomicReference<>();

    public TokenGenerationKeyImmutabilityValidator(SystemEnvironment env) {
        this.env = env;
    }

    @Override
    public void validate(CruiseConfig cruiseConfig) {
        String newTokenGenerationKey = cruiseConfig.server().getTokenGenerationKey();
        if (initialKeyHolder.compareAndSet(null, newTokenGenerationKey)) {
            return;
        }

        String initialKey = initialKeyHolder.get();
        if (!Objects.equals(initialKey, newTokenGenerationKey) && env.enforceServerImmutability()) {
            throw new RuntimeException("The value of 'tokenGenerationKey' cannot be modified while the server is online. If you really want to make this change, you may do so while the server is offline. Please note: updating 'tokenGenerationKey' will invalidate all registration tokens issued to the agents so far.");
        }
    }

    protected String getInitialTokenGenerationKey() {
        return initialKeyHolder.get();
    }
}
