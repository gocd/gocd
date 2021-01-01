/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.server.domain.Username;
import org.junit.Test;

import java.util.Collections;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GoDashboardEnvironmentTest {
    @Test
    public void shouldKnowWhetherAUserCanAdministerIt() {
        GoDashboardEnvironment env = new GoDashboardEnvironment("env1", new AllowedUsers(s("admin1"), Collections.emptySet()), true);

        assertTrue(env.canAdminister(new Username("admin1")));
        assertFalse(env.canAdminister(new Username("viewer1")));
    }

}