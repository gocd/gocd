/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.statusapi;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IsConnectedToServerV1Test {

    @Test
    public void shouldReturnFalseIfAgentHasLostContact() throws Exception {
        AgentHealthHolder mock = mock(AgentHealthHolder.class);
        when(mock.hasLostContact()).thenReturn(true);
        IsConnectedToServerV1 handler = new IsConnectedToServerV1(mock);
        assertFalse(handler.isPassed());
    }

    @Test
    public void shouldReturnTrueIfAgentHasNotLostContact() throws Exception {
        AgentHealthHolder mock = mock(AgentHealthHolder.class);
        when(mock.hasLostContact()).thenReturn(false);
        IsConnectedToServerV1 handler = new IsConnectedToServerV1(mock);
        assertTrue(handler.isPassed());
    }
}