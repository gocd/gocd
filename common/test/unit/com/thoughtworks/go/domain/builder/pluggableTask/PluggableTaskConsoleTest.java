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

package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.StringBufferInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableTaskConsoleTest {
    @Mock
    DefaultGoPublisher publisher;
    private PluggableTaskConsole console;
    private List<String> keys = Arrays.asList("Social Net 1", "Social Net 2", "Social Net 3");
    private List<String> values = Arrays.asList("Twitter", "Facebook", "Mega Upload");

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        console = new PluggableTaskConsole(publisher);
    }

    @Test
    public void shouldPrintLineToPublisher() throws Exception {
        String line = "Test Line";
        doNothing().when(publisher).consumeLine(line);
        console.printLine(line);
        verify(publisher).consumeLine(line);
    }

    @Test
    public void shouldPrintEnvironmentVars() throws Exception {
        Map<String, String> env = new HashMap<>();
        Console.SecureEnvVarSpecifier varSpecifier = mock(Console.SecureEnvVarSpecifier.class);
        for (int i = 0; i < keys.size(); i++) {
            env.put(keys.get(i), values.get(i));
            when(varSpecifier.isSecure(keys.get(i))).thenReturn(i % 2 == 0);
        }
        doNothing().when(publisher).consumeLine("Environment variables: ");
        for (int i = 0; i < keys.size(); i++) {
            doNothing().when(publisher).consumeLine(
                    String.format("Name= %s  Value= %s", keys.get(i),
                            i % 2 == 0 ? PluggableTaskConsole.MASK_VALUE : values.get(i)));
        }
        console.printEnvironment(env, varSpecifier);
        verify(publisher).consumeLine("Environment variables: ");
        for (int i = 0; i < keys.size(); i++) {
            verify(varSpecifier).isSecure(keys.get(i));
            verify(publisher).consumeLine(String.format("Name= %s  Value= %s", keys.get(i),
                    i % 2 == 0 ? PluggableTaskConsole.MASK_VALUE : values.get(i)));
        }
    }

    @Test
    public void shouldReadOutputOfAGiveStream() throws Exception {
        StringBufferInputStream in = new StringBufferInputStream("Lorem ipsum dolor sit amet, consectetur adipisicing elit, \n"
                + "used do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n "
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi \n"
                + "ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit \n"
                + "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \n "
                + "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui \n"
                + "officia deserunt mollit anim id est laborum.");

        doNothing().when(publisher).consumeLine(anyString());
        console.readOutputOf(in);
        Thread.sleep(100);// may become flaky!! Fingers crossed
        verify(publisher, times(7)).consumeLine(anyString());
    }

    @Test
    public void shouldReadErrorOfAGiveStream() throws Exception {
        StringBufferInputStream in = new StringBufferInputStream("Lorem ipsum dolor sit amet, consectetur adipisicing elit, \n"
                + "used do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n "
                + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi \n"
                + "ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit \n"
                + "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \n "
                + "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui \n"
                + "officia deserunt mollit anim id est laborum.");

        doNothing().when(publisher).consumeLine(anyString());
        console.readErrorOf(in);
        Thread.sleep(100);// may become flaky!! Fingers crossed
        verify(publisher, times(7)).consumeLine(anyString());
    }

}
