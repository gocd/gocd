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

package com.thoughtworks.go.agent.bootstrapper;

import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptor;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class DefaultAgentLaunchDescriptorImpl implements AgentLaunchDescriptor {

    private final AgentBootstrapper bootstrapper;
    private final Map context = new ConcurrentHashMap();

    public DefaultAgentLaunchDescriptorImpl(AgentBootstrapperArgs bootstrapperArgs, AgentBootstrapper agentBootstrapper) {
        this.bootstrapper = agentBootstrapper;
        buildContext(bootstrapperArgs);
    }

    private void buildContext(AgentBootstrapperArgs bootstrapperArgs) {
        context.putAll(bootstrapperArgs.toProperties());
    }

    @Override
    public Map context() {
        return context;
    }

    @Override
    public AgentBootstrapper getBootstrapper() {
        return bootstrapper;
    }
}
