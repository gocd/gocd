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
package com.thoughtworks.go.domain;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AgentStatusTest {
    @Test
    public void shouldCompareStatusAsExpected() {
        AgentStatus statusInOrder[] = new AgentStatus[] {AgentStatus.Pending, AgentStatus.LostContact, AgentStatus.Missing,
                AgentStatus.Building, AgentStatus.Cancelled, AgentStatus.Idle, AgentStatus.Disabled};
        AgentStatus previous = null;

        for(AgentStatus status : statusInOrder) {
            if(previous != null) {
                assertThat(previous.compareTo(status), is(org.hamcrest.Matchers.lessThan(0)));
                assertThat(status.compareTo(previous), is(greaterThan(0)));
            }
            previous = status;
        }
    }

}
