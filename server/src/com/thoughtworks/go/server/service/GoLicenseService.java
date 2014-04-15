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

import com.thoughtworks.go.licensing.Edition;
import com.thoughtworks.go.licensing.GoLicense;
import com.thoughtworks.go.licensing.LicenseValidity;
import com.thoughtworks.go.licensing.ValidLicense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service

public class GoLicenseService  {
    private GoConfigService goConfigService;
    private final GoLicense decryptedLicense = GoLicense.OSS_LICENSE;
    private final LicenseValidity licenseValidity = new ValidLicense(decryptedLicense);

    @Autowired
    public GoLicenseService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean isLicenseValid() {
        return licenseValidity.isValid();
    }

    public boolean isLicenseExpired() {
        return false;
    }

    public Edition getCruiseEdition() {
        return decryptedLicense.edition();
    }

    public int getNumberOfLicensedRemoteAgents() {
        return decryptedLicense.numberOfAgents();
    }

    public int maximumUsersAllowed() {
        return decryptedLicense.numberOfUsers();
    }

    public boolean hasRemoteAgentsExceededLicenseLimit() {
        int licensedAgent = getNumberOfLicensedRemoteAgents();
        int remoteAgents = goConfigService.getNumberOfApprovedRemoteAgents();
        return remoteAgents > licensedAgent;
    }

    public boolean isLicenseEmpty() {
        return false;
    }

    public Date getExpirationDate() {
        return decryptedLicense.getExpirationDate();
    }
}
