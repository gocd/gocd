/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;

/**
 * @understands approving agent by adding them to config
 */
public class ApproveAgentCommand implements UpdateConfigCommand {

    final private String uuid;
    private final String ipAddress;
    private final String hostname;

    public ApproveAgentCommand(String uuid, String ipAddress, String hostname) {
        this.uuid = uuid;
        this.ipAddress = ipAddress;
        this.hostname = hostname;
    }

    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
        cruiseConfig.agents().add(new AgentConfig(uuid, hostname, ipAddress));
        return cruiseConfig;
    }
}