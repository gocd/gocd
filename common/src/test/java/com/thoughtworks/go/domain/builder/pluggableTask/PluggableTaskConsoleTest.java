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
package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.util.command.SafeOutputStreamConsumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableTaskConsoleTest {

    @Mock
    SafeOutputStreamConsumer safeOutputStreamConsumer;

    private PluggableTaskConsole console;
    private List<String> keys = Arrays.asList("Social Net 1", "Social Net 2", "Social Net 3");
    private List<String> values = Arrays.asList("Twitter", "Facebook", "Mega Upload");

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        console = new PluggableTaskConsole(safeOutputStreamConsumer, "utf-8");
    }

    @Test
    public void shouldPrintLineToPublisher() throws Exception {
        String line = "Test Line";
        doNothing().when(safeOutputStreamConsumer).stdOutput(line);
        console.printLine(line);
        verify(safeOutputStreamConsumer).stdOutput(line);
    }

    @Test
    public void shouldPrintEnvironmentVars() throws Exception {
        Map<String, String> env = new HashMap<>();
        Console.SecureEnvVarSpecifier varSpecifier = mock(Console.SecureEnvVarSpecifier.class);
        for (int i = 0; i < keys.size(); i++) {
            env.put(keys.get(i), values.get(i));
            when(varSpecifier.isSecure(keys.get(i))).thenReturn(i % 2 == 0);
        }
        doNothing().when(safeOutputStreamConsumer).stdOutput("Environment variables: ");
        for (int i = 0; i < keys.size(); i++) {
            doNothing().when(safeOutputStreamConsumer).stdOutput(
                    String.format("Name= %s  Value= %s", keys.get(i),
                            i % 2 == 0 ? PluggableTaskConsole.MASK_VALUE : values.get(i)));
        }
        console.printEnvironment(env, varSpecifier);
        verify(safeOutputStreamConsumer).stdOutput("Environment variables: ");
        for (int i = 0; i < keys.size(); i++) {
            verify(varSpecifier).isSecure(keys.get(i));
            verify(safeOutputStreamConsumer).stdOutput(String.format("Name= %s  Value= %s", keys.get(i),
                    i % 2 == 0 ? PluggableTaskConsole.MASK_VALUE : values.get(i)));
        }
    }

    @Test
    public void shouldReadOutputOfAGiveStream() throws Exception {
        InputStream in = new ByteArrayInputStream(("Lorem ipsum dolor sit amet, consectetur adipisicing elit, \n"
                + "used do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n "
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi \n"
                + "ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit \n"
                + "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \n "
                + "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui \n"
                + "officia deserunt mollit anim id est laborum.").getBytes());

        doNothing().when(safeOutputStreamConsumer).stdOutput(anyString());
        console.readOutputOf(in);
        verify(safeOutputStreamConsumer, timeout(10000).times(7)).stdOutput(anyString());
    }

    @Test
    public void shouldReadErrorOfAGiveStream() throws Exception {
        InputStream in = new ByteArrayInputStream(("Lorem ipsum dolor sit amet, consectetur adipisicing elit, \n"
                + "used do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n "
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi \n"
                + "ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit \n"
                + "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \n "
                + "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui \n"
                + "officia deserunt mollit anim id est laborum.").getBytes());

        doNothing().when(safeOutputStreamConsumer).errOutput(anyString());
        console.readErrorOf(in);
        verify(safeOutputStreamConsumer, timeout(10000).times(7)).errOutput(anyString());
    }

}
