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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.util.SystemEnvironment;

import java.util.Map;
import java.util.UUID;

public class CheckConnectionSubprocessExecutionContext implements SubprocessExecutionContext {
    private SystemEnvironment systemEnvironment;

    public CheckConnectionSubprocessExecutionContext(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String getProcessNamespace(String fingerprint) {
        return UUID.randomUUID().toString();
    }

    @Override
    public Map<String, String> getDefaultEnvironmentVariables() {
        return systemEnvironment.getGitAllowedProtocols();
    }

    @Override
    public Boolean isGitShallowClone() {
        throw new UnsupportedOperationException(" This is an unexpected call. Perform this action only on the server.");
    }

    @Override
    public boolean isServer() {
        throw new UnsupportedOperationException("This is an unexpected call.");
    }

    @Override
    public void setGitShallowClone(boolean value) {
        throw new UnsupportedOperationException("This is an unexpected call.");
    }
}
