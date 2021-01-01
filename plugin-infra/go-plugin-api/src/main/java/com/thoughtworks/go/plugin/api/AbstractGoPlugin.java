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
package com.thoughtworks.go.plugin.api;

/**
 * An abstract implementation GoPlugin which will take care of GoApplicationAccessor initialization.
 */
public abstract class AbstractGoPlugin implements GoPlugin {

    protected GoApplicationAccessor goApplicationAccessor;

    /**
     * Initializes GoApplicationAccessor with an instance of GoApplicationAccessor
     *
     * @param goApplicationAccessor An instance of GoApplicationAccessor
     */
    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        this.goApplicationAccessor = goApplicationAccessor;
    }
}
