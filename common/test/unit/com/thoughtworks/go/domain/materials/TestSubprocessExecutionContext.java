/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.materials;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.util.SystemEnvironment;

public class TestSubprocessExecutionContext implements SubprocessExecutionContext {
    private SystemEnvironment systemEnvironment;
    public String getProcessNamespace(String fingerprint) {
        return UUID.randomUUID().toString();
    }

    public TestSubprocessExecutionContext() {
        this.systemEnvironment = new SystemEnvironment();
    }

    public TestSubprocessExecutionContext(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public Map<String, String> getDefaultEnvironmentVariables() {
        return new HashMap<>();
    }

    @Override
    public Boolean isGitShallowClone() {
        return systemEnvironment.get(SystemEnvironment.GO_SERVER_SHALLOW_CLONE);
    }
}
