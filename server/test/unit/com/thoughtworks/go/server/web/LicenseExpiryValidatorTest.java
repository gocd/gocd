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

import java.util.Date;

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.DateUtils.formatISO8601;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LicenseExpiryValidatorTest {
    private LicenseExpiryValidator expiryValidator;
    private GoLicenseService goLicenseService;
    private ServerHealthService serverHealthService;
    private Localizer localizer;

    @Before
    public void setUp() throws Exception {
        goLicenseService = mock(GoLicenseService.class);
        serverHealthService = mock(ServerHealthService.class);
        localizer = mock(Localizer.class);
        expiryValidator = new LicenseExpiryValidator(goLicenseService, serverHealthService, localizer);

    }

    @Test
    public void shouldUnderstandIfLicenseIsExpired() {
        when(goLicenseService.isLicenseExpired()).thenReturn(true).thenReturn(false);
        assertThat(expiryValidator.isLicenseExpired(), is(true));
        assertThat(expiryValidator.isLicenseExpired(), is(false));
    }

    @Test
    public void shouldUpdateServerHealthStateWhenLicenseExpires() {
        when(localizer.localize("LICENSE_EXPIRED_WITH_URL_AND_DATE", GoConstants.THOUGHTWORKS_LICENSE_URL, formatISO8601(new Date(111, 02, 01)))).thenReturn("description");
        when(localizer.localize("LICENSE_EXPIRED", new Object[0])).thenReturn("message");
        when(goLicenseService.isLicenseExpired()).thenReturn(true);
        when(goLicenseService.getExpirationDate()).thenReturn(new Date(111, 02, 01));

        expiryValidator.updateServerHealth();

        ServerHealthState expectedServerHealthState = ServerHealthState.warning("message", "description", HealthStateType.expiredLicense(HealthStateScope.GLOBAL));
        verify(serverHealthService).update(expectedServerHealthState);
    }

    @Test
    public void shouldUpdateServerHealthStateWhenConfigChanges() {
        when(goLicenseService.isLicenseExpired()).thenReturn(false);
        expiryValidator.updateServerHealth();
        verify(serverHealthService).update(ServerHealthState.success(HealthStateType.expiredLicense(HealthStateScope.GLOBAL)));
    }
}
