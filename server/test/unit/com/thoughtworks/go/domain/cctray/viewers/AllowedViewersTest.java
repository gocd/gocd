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
package com.thoughtworks.go.domain.cctray.viewers;

import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AllowedViewersTest {
    @Test
    public void shouldCheckViewPermissionsInACaseInsensitiveWay() throws Exception {
        AllowedViewers viewers = new AllowedViewers(s("USER1", "user2", "User3", "AnoTherUsEr"));

        assertThat(viewers.contains("user1"), is(true));
        assertThat(viewers.contains("USER1"), is(true));
        assertThat(viewers.contains("User1"), is(true));
        assertThat(viewers.contains("USER2"), is(true));
        assertThat(viewers.contains("uSEr3"), is(true));
        assertThat(viewers.contains("anotheruser"), is(true));
        assertThat(viewers.contains("NON-EXISTENT-USER"), is(false));
    }
}