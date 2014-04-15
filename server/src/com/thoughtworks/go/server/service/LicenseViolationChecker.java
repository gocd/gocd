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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.web.LicenseExpiryValidator;
import com.thoughtworks.go.server.web.LicensedAgentCountValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseViolationChecker {
    private final LicenseExpiryValidator licenseExpiryValidator;
    private final LicensedAgentCountValidator licensedAgentCountValidator;
    private GoConfigService goConfigService;

    @Autowired
    public LicenseViolationChecker(LicenseExpiryValidator licenseExpiryValidator, LicensedAgentCountValidator licensedAgentCountValidator, GoConfigService goConfigService) {
        this.licenseExpiryValidator = licenseExpiryValidator;
        this.licensedAgentCountValidator = licensedAgentCountValidator;
        this.goConfigService = goConfigService;
    }

    public void initialize() {
        goConfigService.register(new ConfigChangedListener() {
            public void onConfigChange(CruiseConfig newCruiseConfig) {
                checkForViolation();
                LicenseViolationChecker.this.licensedAgentCountValidator.updateServerHealth();
            }
        });
    }

    public void checkForViolation() {
        licenseExpiryValidator.updateServerHealth();
    }
}
