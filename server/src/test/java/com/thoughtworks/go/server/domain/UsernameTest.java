/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsernameTest {

    @Test
    public void shouldReturnDisplayName() {
        Username username2 = new Username(new CaseInsensitiveString("dyang"), "Derek Yang");
        assertThat(username2.getUsername()).isEqualTo(new CaseInsensitiveString("dyang"));
        assertThat(username2.getDisplayName()).isEqualTo("Derek Yang");
    }

    @Test
    public void shouldReturnUsernameAsDisplayNameIfDisplayNameIsNotSet() {
        Username username1 = new Username(new CaseInsensitiveString("dyang"));
        assertThat(username1.getUsername()).isEqualTo(new CaseInsensitiveString("dyang"));
        assertThat(username1.getDisplayName()).isEqualTo("dyang");
    }

    @Test
    public void shouldAllowBuildingUsernameFromString() {
        Username one = new Username(new CaseInsensitiveString("myusername"));
        Username two = Username.valueOf("myusername");
        assertThat(one).isEqualTo(two);
    }

    @Test
    public void shouldCheckIfUserIsAGoAgentUser() {
        final Username go_agent_user = new Username("_go_agent_hostname");

        assertTrue(go_agent_user.isGoAgentUser());
        assertFalse(go_agent_user.isAnonymous());
    }
}
