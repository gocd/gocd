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
package com.thoughtworks.go.config.commands;

import com.thoughtworks.go.config.CruiseConfig;

public interface EntityConfigUpdateCommand<T> extends CheckedUpdateCommand {
    /**
     * Perform the actual update. Passed a deep clone of the current cruise config.
     */
    void update(CruiseConfig preprocessedConfig) throws Exception;

    /**
     * Called after {@link #update(CruiseConfig)} used to validate the new config.
     */
    boolean isValid(CruiseConfig preprocessedConfig);

    void clearErrors();

    /**
     * Called after {@link #isValid(CruiseConfig)}. Return the entity that was updated.
     * Useful to invalidate the cache used by {@link com.thoughtworks.go.server.service.EntityHashingService}
     */
    T getPreprocessedEntityConfig();

    default void encrypt(CruiseConfig preProcessedConfig) {
    }
}
