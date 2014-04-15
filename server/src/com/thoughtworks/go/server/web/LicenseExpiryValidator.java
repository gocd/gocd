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

import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.DateUtils.formatISO8601;

@Component
public class LicenseExpiryValidator {
    private GoLicenseService goLicenseService;
    private ServerHealthService serverHealthService;
    private final Localizer localizer;

    @Autowired
    public LicenseExpiryValidator(GoLicenseService goLicenseService, ServerHealthService serverHealthService, Localizer localizer) {
        this.goLicenseService = goLicenseService;
        this.serverHealthService = serverHealthService;
        this.localizer = localizer;
    }

    public void updateServerHealth() {
        if (isLicenseExpired()) {
            serverHealthService.update(ServerHealthState.warning(message(), description(), HealthStateType.expiredLicense(HealthStateScope.GLOBAL)));
        } else {
            serverHealthService.update(ServerHealthState.success(HealthStateType.expiredLicense(HealthStateScope.GLOBAL)));
        }
    }

    public boolean isLicenseExpired() {
        return goLicenseService.isLicenseExpired();
    }

    private String message() {
        return LocalizedMessage.string("LICENSE_EXPIRED").localize(localizer);
    }

    public String description() {
        return LocalizedMessage.string("LICENSE_EXPIRED_WITH_URL_AND_DATE", GoConstants.THOUGHTWORKS_LICENSE_URL, formatISO8601(goLicenseService.getExpirationDate())).localize(localizer);
    }
}
