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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;

import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;

/**
 * @understands checks number of enabled users are not over license limit
 */
public class EnabledUsersLicenseLimitChecker implements SchedulingChecker {
    static final HealthStateType USER_LIMIT_EXCEEDED = HealthStateType.userLimitExceeded(GLOBAL);
    private final UserService userService;
    private final GoLicenseService goLicenseService;
    private final ServerHealthService serverHealthService;

    public EnabledUsersLicenseLimitChecker(UserService userService, GoLicenseService goLicenseService, ServerHealthService serverHealthService) {
        this.userService = userService;
        this.goLicenseService = goLicenseService;
        this.serverHealthService = serverHealthService;
    }

    public void check(OperationResult result) {
        if (goLicenseService.isLicenseEmpty()) {
            result.error("License Violation", "There is no license configured. Scheduling will resume once a valid license is used.", USER_LIMIT_EXCEEDED);
            return;
        }
        if (userService.isLicenseUserLimitExceeded()) {
            result.error("License Violation",
                    String.format("Current Go licence allows only %s users. There are currently %s users enabled."
                            + " Go pipeline scheduling will be stopped until %s users are disabled."
                            + " Please disable users to comply with the license limit, or "
                            + "<a href='%s'>contact our sales team</a> to upgrade your license.", goLicenseService.maximumUsersAllowed(), userService.enabledUserCount(),
                            (userService.enabledUserCount() - goLicenseService.maximumUsersAllowed()), GoConstants.THOUGHTWORKS_LICENSE_URL),
                    USER_LIMIT_EXCEEDED);
            return;
        }
        serverHealthService.update(ServerHealthState.success(EnabledUsersLicenseLimitChecker.USER_LIMIT_EXCEEDED));
    }
}