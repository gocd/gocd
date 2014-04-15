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

package com.thoughtworks.go.licensing;

import java.sql.Date;
import java.util.HashMap;

import com.thoughtworks.go.util.GoConstants;
import org.junit.Test;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GoLicenseTest {

    @Test
    public void shouldFillIntoMap() {
        Date date = Date.valueOf("2009-03-05");
        GoLicense goLicense = GoLicense.createLicense(date, 6, Edition.Enterprise, 7);
        HashMap<String, Object> model = new HashMap<String, Object>();
        goLicense.fill(model);
        assertThat(model, hasEntry(GoConstants.MAX_AGENTS, (Object) 6));
        assertThat(model, hasEntry(GoConstants.EXPIRY_DATE, (Object) date));
        assertThat(model, hasEntry(GoConstants.EDITION, (Object) Edition.Enterprise));
        assertThat(model, hasEntry(GoConstants.MAX_USERS, (Object) 7));
    }

    @Test
    public void shouldAllowMaxOfTenUsersForCommunityLicence() {
        GoLicense goLicense = GoLicense.createLicense(Date.valueOf("2009-03-05"), 0, Edition.Free, 100);
        HashMap<String, Object> model = new HashMap<String, Object>();
        goLicense.fill(model);
        assertThat(model, hasEntry(GoConstants.MAX_USERS, (Object) 10));
    }
}
