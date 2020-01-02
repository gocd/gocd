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
package com.thoughtworks.go.domain;

import java.io.Serializable;

/**
 * @understands different states agent can be in
 */
public enum AgentStatus implements Comparable<AgentStatus>, Serializable {
    Pending("Pending", AgentConfigStatus.Pending, AgentRuntimeStatus.Unknown),
    LostContact("LostContact", AgentConfigStatus.Enabled, AgentRuntimeStatus.LostContact),
    Missing("Missing", AgentConfigStatus.Enabled, AgentRuntimeStatus.Missing),
    Building("Building", AgentConfigStatus.Enabled, AgentRuntimeStatus.Building),
    Cancelled("Cancelled", AgentConfigStatus.Enabled, AgentRuntimeStatus.Cancelled),
    Idle("Idle", AgentConfigStatus.Enabled, AgentRuntimeStatus.Idle),
    Disabled("Disabled", AgentConfigStatus.Disabled, AgentRuntimeStatus.Unknown);

    private final String name;
    private final AgentConfigStatus configStatus;
    private final AgentRuntimeStatus runtimeStatus;

    private AgentStatus(String name, AgentConfigStatus configStatus, AgentRuntimeStatus runtimeStatus) {
        this.name = name;
        this.configStatus = configStatus;
        this.runtimeStatus = runtimeStatus;
    }

    public static AgentStatus fromRuntime(AgentRuntimeStatus runtimeStatus) {
        if (runtimeStatus == AgentRuntimeStatus.LostContact) { return LostContact; }
        if (runtimeStatus == AgentRuntimeStatus.Missing) { return Missing; }
        if (runtimeStatus == AgentRuntimeStatus.Building) { return Building; }
        if (runtimeStatus == AgentRuntimeStatus.Idle) { return Idle; }
        if (runtimeStatus == AgentRuntimeStatus.Cancelled) { return Cancelled; }
        throw new IllegalArgumentException("Unknown runtime status " + runtimeStatus);
    }

    public static AgentStatus fromConfig(AgentConfigStatus configStatus) {
        if (configStatus == AgentConfigStatus.Pending) { return Pending; }
        if (configStatus == AgentConfigStatus.Disabled) { return Disabled; }
        throw new IllegalArgumentException("Unknown config status " + configStatus);
    }

    public boolean isEnabled() {
        return this.configStatus == AgentConfigStatus.Enabled;
    }

    public boolean isRegistered() {
        return this.configStatus != AgentConfigStatus.Pending;
     }

    public boolean isIdle() {
        return this.runtimeStatus == AgentRuntimeStatus.Idle;
    }

    @Override
    public String toString() {
        return name;
    }

    public AgentRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public AgentConfigStatus getConfigStatus() {
        return configStatus;
    }
}
