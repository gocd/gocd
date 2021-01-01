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
package com.thoughtworks.cruise.agent.common.launcher;

import java.util.Map;

/**
 * obj of this type will be passed from bootstrapper to launcher
 */
public interface AgentLaunchDescriptor {

    Map context();

    /**
     * future proofing - in case we need the bootstrapper (reflection only)
     *
     * @return com.thoughtworks.go.agent.bootstrapper.AgentBootstrapper as Object to avoid circular dep between bootstrapper and common
     */
    Object getBootstrapper();
}
