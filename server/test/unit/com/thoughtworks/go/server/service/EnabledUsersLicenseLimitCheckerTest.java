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
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EnabledUsersLicenseLimitCheckerTest {
    private UserService userService;
    private EnabledUsersLicenseLimitChecker checker;
    private OperationResult operationResult;
    private GoLicenseService goLicenseService;
    private ServerHealthService serverHealthService;

    @Before public void setUp() throws Exception {
        userService = mock(UserService.class);
        goLicenseService = mock(GoLicenseService.class);
        serverHealthService = mock(ServerHealthService.class);
        checker = new EnabledUsersLicenseLimitChecker(userService, goLicenseService, serverHealthService);
        operationResult = new ServerHealthStateOperationResult();
    }

    @Test
    public void shouldFailForUserLicenceLimitExceededSituation() {
        when(userService.isLicenseUserLimitExceeded()).thenReturn(true);
        when(goLicenseService.maximumUsersAllowed()).thenReturn(10);
        when(userService.enabledUserCount()).thenReturn(12);
        checker.check(operationResult);
        assertThat(operationResult.canContinue(), is(false));
        String desc = String.format(
                "Current Go licence allows only %s users. There are currently %s users enabled. Go pipeline scheduling will be stopped until %s users are disabled. "
                        + "Please disable users to comply with the license limit, or "
                        + "<a href='http://www.thoughtworks.com/products/go-continuous-delivery/compare'>contact our sales team</a> to upgrade your license.",
                10, 12, 2, GoConstants.THOUGHTWORKS_LICENSE_URL);
        assertThat(operationResult.getServerHealthState(), is(ServerHealthState.error("License Violation", desc, EnabledUsersLicenseLimitChecker.USER_LIMIT_EXCEEDED)));
        verifyNoMoreInteractions(serverHealthService);
    }

    @Test
    public void shouldNotFailForUsersUnderLicenceLimit() {
        when(userService.isLicenseUserLimitExceeded()).thenReturn(false);
        checker.check(operationResult);
        assertThat(operationResult.canContinue(), is(true));
    }

    @Test
    public void shouldFailWhenThereIsNoLicense() {
        when(goLicenseService.isLicenseEmpty()).thenReturn(true);
        checker.check(operationResult);
        assertThat(operationResult.canContinue(), is(false));
        String desc = "There is no license configured. Scheduling will resume once a valid license is used.";
        assertThat(operationResult.getServerHealthState(), is(ServerHealthState.error("License Violation", desc, EnabledUsersLicenseLimitChecker.USER_LIMIT_EXCEEDED)));
        verifyNoMoreInteractions(serverHealthService);
    }

    @Test
    public void shouldClearErrorMessageWhenSuccessful() {
        checker.check(operationResult);
        verify(serverHealthService).update(ServerHealthState.success(EnabledUsersLicenseLimitChecker.USER_LIMIT_EXCEEDED));
    }
}
