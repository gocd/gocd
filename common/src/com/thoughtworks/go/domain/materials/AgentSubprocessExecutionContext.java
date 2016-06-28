/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.CachedDigestUtils;

import java.util.HashMap;
import java.util.Map;

class AgentSubprocessExecutionContext implements SubprocessExecutionContext {
    private AgentIdentifier agentIdentifier;
    private final String workingDirectory;

    AgentSubprocessExecutionContext(final AgentIdentifier agentIdentifier, String workingDirectory) {
        this.agentIdentifier = agentIdentifier;
        this.workingDirectory = workingDirectory;
    }

    public String getProcessNamespace(String fingerprint) {
        return CachedDigestUtils.sha256Hex(fingerprint + agentIdentifier.getUuid() + workingDirectory);
    }

    @Override
    public Map<String, String> getDefaultEnvironmentVariables() {
        return new HashMap<>();
    }

    @Override
    public Boolean isGitShallowClone() {
        throw new UnsupportedOperationException("This is an unexpected call. Perform this action only on the server.");
    }

    @Override
    public boolean isServer() {
        return false;
    }
}
