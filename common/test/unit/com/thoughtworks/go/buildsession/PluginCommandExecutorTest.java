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

package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PluginCommandExecutorTest {
    PluginCommandExecutor pluginCommandExecutor;
    BuildCommandExecutor buildCommandExecutor;
    BuildCommand buildCommand;
    BuildSession buildSession;

    @Before
    public void setUp() throws Exception {
        buildCommandExecutor = mock(BuildCommandExecutor.class);
        pluginCommandExecutor = new PluginCommandExecutor(buildCommandExecutor);
        buildCommand = mock(BuildCommand.class);
        buildSession = mock(BuildSession.class);
    }

    @Test
    public void shouldNotExecuteATfsExecutorIfPluginTypeIsNotTfs() throws Exception {
        when(buildCommand.getStringArg("type")).thenReturn("non-tfs type");

        boolean result = pluginCommandExecutor.execute(buildCommand, buildSession);

        verify(buildCommandExecutor, never()).execute(buildCommand, buildSession);
        assertThat(result, is(false));
    }

    @Test
    public void shouldExecuteATfsExecutorIfPluginTypeIsTfs() throws Exception {
        when(buildCommand.getStringArg("type")).thenReturn("tfs");
        when(buildCommandExecutor.execute(buildCommand, buildSession)).thenReturn(true);

        boolean result = pluginCommandExecutor.execute(buildCommand, buildSession);

        verify(buildCommandExecutor).execute(buildCommand, buildSession);
        assertThat(result, is(true));
    }
}