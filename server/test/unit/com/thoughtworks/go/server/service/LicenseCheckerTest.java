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

import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.licensing.Edition;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LicenseCheckerTest {
    private GoLicenseService goLicenseService;
    private LicenseChecker licenseChecker;

    @Before
    public void setUp(){
        goLicenseService = mock(GoLicenseService.class);
        licenseChecker = new LicenseChecker(goLicenseService);
    }

    @Test
    public void shouldSetResultToInvalidLicenseWhenLicenseIsInvalid() {
        when(goLicenseService.isLicenseValid()).thenReturn(false);
        HttpOperationResult result = new HttpOperationResult();
        licenseChecker.check(result);
        assertThat(result.httpCode(), is(406));
        assertThat(result.getServerHealthState().getType(), is(HealthStateType.invalidLicense(HealthStateScope.GLOBAL)));
        assertThat(result.message() ,is("Failed to schedule the pipeline because Go does not have a valid license."));
    }

    @Test
    public void shouldSetTherightServerHealthMessageWhenEnterpriseLicenseIsExpired() {
        when(goLicenseService.isLicenseValid()).thenReturn(true);
        when(goLicenseService.isLicenseExpired()).thenReturn(true);
        when(goLicenseService.getCruiseEdition()).thenReturn(Edition.Enterprise);
        HttpOperationResult result = new HttpOperationResult();
        licenseChecker.check(result);
        assertThat(result.httpCode(), is(406));
        assertThat(result.getServerHealthState().getType(), is(HealthStateType.invalidLicense(HealthStateScope.GLOBAL)));
        assertThat(result.message() ,is("Failed to schedule the pipeline because your license has expired."));
    }

    @Test
    public void shouldSetTheHealthStateAsSuccessWhenLicenseIsCorrected() {
        when(goLicenseService.isLicenseValid()).thenReturn(true);
        when(goLicenseService.isLicenseExpired()).thenReturn(false);
        when(goLicenseService.getCruiseEdition()).thenReturn(Edition.Enterprise);
        HttpOperationResult result = new HttpOperationResult();
        licenseChecker.check(result);
        assertThat(result.getServerHealthState().getType(), is(HealthStateType.invalidLicense(HealthStateScope.GLOBAL)));
        assertThat(result.getServerHealthState().getLogLevel(), is(HealthStateLevel.OK));
    }

    @Test
    public void shouldSetTheHealthStateAsInvalidWhenLicenseIsCommunityAndExpired() {
        when(goLicenseService.isLicenseValid()).thenReturn(true);
        when(goLicenseService.isLicenseExpired()).thenReturn(true);
        when(goLicenseService.getCruiseEdition()).thenReturn(Edition.Free);
        HttpOperationResult result = new HttpOperationResult();
        licenseChecker.check(result);
        assertThat(result.httpCode(), is(406));
        assertThat(result.getServerHealthState().getType(), is(HealthStateType.invalidLicense(HealthStateScope.GLOBAL)));
        assertThat(result.message() ,is("Failed to schedule the pipeline because your license has expired."));
    }
}
