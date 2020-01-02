/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class UsernameTest {

    @Test
    public void shouldReturnDisplayName() {
        Username username2 = new Username(new CaseInsensitiveString("dyang"), "Derek Yang");
        assertThat("dyang should be Username.", username2.getUsername(), is(new CaseInsensitiveString("dyang")));
        assertThat("Derek Yang should be display name.", username2.getDisplayName(), is("Derek Yang"));
    }

    @Test
    public void shouldReturnUsernameAsDisplayNameIfDisplayNameIsNotSet() {
        Username username1 = new Username(new CaseInsensitiveString("dyang"));
        assertThat("dyang should be Username.", username1.getUsername(), is(new CaseInsensitiveString("dyang")));
        assertThat("dyang should be display name.", username1.getDisplayName(), is("dyang"));
    }

    @Test
    public void shouldAllowBuildingUsernameFromString() {
        Username one = new Username(new CaseInsensitiveString("myusername"));
        Username two = Username.valueOf("myusername");
        assertThat(one, is(two));
    }

    @Test
    public void shouldCheckIfUserIsAGoAgentUser() {
        final Username go_agent_user = new Username("_go_agent_hostname");

        assertTrue(go_agent_user.isGoAgentUser());
        assertFalse(go_agent_user.isAnonymous());
    }
}
