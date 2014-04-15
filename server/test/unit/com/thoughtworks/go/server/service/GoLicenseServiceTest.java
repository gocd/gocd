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


import com.googlecode.junit.ext.JunitExtRunner;
import com.thoughtworks.go.licensing.Edition;
import com.thoughtworks.go.util.SystemTimeClock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(JunitExtRunner.class)
public class GoLicenseServiceTest {

    private GoLicenseService service;
    private GoConfigService cruiseConfigService;

    @Before
    public void setUp() throws Exception {
        cruiseConfigService = mock(GoConfigService.class);
        service = new GoLicenseService(cruiseConfigService);
    }

    @Test

    public void shouldReturnOSSValues() throws Exception {
        when(cruiseConfigService.getNumberOfApprovedRemoteAgents()).thenReturn(Integer.MAX_VALUE);
        assertThat(service.isLicenseValid(), is(true));
        assertThat(service.isLicenseExpired(), is(false));
        assertThat(service.getCruiseEdition(), is(Edition.OpenSource));
        assertThat(service.getNumberOfLicensedRemoteAgents(), is(Integer.MAX_VALUE));
        assertThat(service.maximumUsersAllowed(), is(Integer.MAX_VALUE));
        assertThat(service.hasRemoteAgentsExceededLicenseLimit(), is(false));
        assertThat(service.isLicenseEmpty(), is(false));
        assertThat(service.getExpirationDate(), is(new Date(SystemTimeClock.ETERNITY.getTime())));
        verify(cruiseConfigService).getNumberOfApprovedRemoteAgents();
    }
}
