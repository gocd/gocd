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

import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;

/**
 * @understands checking for valid license
 */
public class LicenseChecker implements SchedulingChecker {
    private GoLicenseService goLicenseService;
    private static final HealthStateType LICENSE_INVALID = HealthStateType.invalidLicense(GLOBAL);

    public LicenseChecker(GoLicenseService goLicenseService) {
        this.goLicenseService = goLicenseService;
    }

    public void check(OperationResult result) {
        if (!goLicenseService.isLicenseValid()) {
            result.notAcceptable("Failed to schedule the pipeline because Go does not have a valid license.", LICENSE_INVALID);
        } else if(goLicenseService.isLicenseExpired()) {
            result.notAcceptable("Failed to schedule the pipeline because your license has expired.", "The server is running with an expired License. Please fix this to resume pipeline scheduling.", LICENSE_INVALID);
        }
        else {
            result.success(LICENSE_INVALID);
        }
    }
}
