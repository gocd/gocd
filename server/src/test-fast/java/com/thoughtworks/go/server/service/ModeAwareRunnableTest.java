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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ModeAwareRunnableTest {

    @Mock
    private SystemEnvironment systemEnvironment;


    @Test
    public void shouldRunTaskWhenServerIsActive() throws Exception {
        Runnable originalRunnable = mock(Runnable.class);
        when(systemEnvironment.isServerActive()).thenReturn(true);

        ModeAwareRunnable runnable = new ModeAwareRunnable(originalRunnable, systemEnvironment);

        runnable.run();
        verify(originalRunnable).run();
    }

    @Test
    public void shouldNotRunTaskWhenServerIsNotActive() throws Exception {
        Runnable originalRunnable = mock(Runnable.class);
        when(systemEnvironment.isServerActive()).thenReturn(false);

        ModeAwareRunnable runnable = new ModeAwareRunnable(originalRunnable, systemEnvironment);

        runnable.run();
        verify(originalRunnable, never()).run();
    }
}
