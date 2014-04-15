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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands validating the number of approved agents
 */
@Component
public class LicensedAgentCountValidator {
    private GoConfigService goConfigService;
    private GoLicenseService goLicenseService;
    private ServerHealthService serverHealthService;

    @Autowired
    public LicensedAgentCountValidator(GoConfigService goConfigService, GoLicenseService goLicenseService, ServerHealthService serverHealthService) {
        this.goConfigService = goConfigService;
        this.goLicenseService = goLicenseService;
        this.serverHealthService = serverHealthService;
    }

    public void updateServerHealth() {
        if (hasViolatedLicenseLimit()) {
            serverHealthService.update(ServerHealthState.warning(message(), description(), HealthStateType.exceedsAgentLimit(HealthStateScope.GLOBAL)));
        } else {
            serverHealthService.update(ServerHealthState.success(HealthStateType.exceedsAgentLimit(HealthStateScope.GLOBAL)));
        }
    }

    public boolean hasViolatedLicenseLimit() {
        return goLicenseService.hasRemoteAgentsExceededLicenseLimit();
    }

    private String message() {
        return "Number of enabled agents exceeds licensed number";
    }

    public String description() {
        int licensedAgents = goLicenseService.getNumberOfLicensedRemoteAgents();
        int actualAgents = goConfigService.getNumberOfApprovedRemoteAgents();
        return String.format("Current Go license allows only %s remote agents. Currently %s remote agents are enabled."
                + " Go will continue to assign jobs only to %s remote agents and the remaining remote agents will not be used."
                + " Please disable additional remote agents to comply with your license, or "
                + "<a href='%s'>contact our sales team</a> to buy more agents.", licensedAgents, actualAgents, licensedAgents, GoConstants.THOUGHTWORKS_LICENSE_URL);
    }
}
