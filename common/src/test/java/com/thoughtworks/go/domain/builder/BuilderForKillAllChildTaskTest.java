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

package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.process.CurrentProcess;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

class BuilderForKillAllChildTaskTest {
    @Mock
    CurrentProcess currentProcess;
    @Mock
    DefaultGoPublisher defaultGoPublisher;
    private BuilderForKillAllChildTask builder;

    @BeforeEach
    void setUp() {
        initMocks(this);
        builder = new BuilderForKillAllChildTask(currentProcess);
    }

    @Nested
    class build {
        @Test
        void shouldKillAllChildProcessOfTheAgent() {
            builder.build(defaultGoPublisher, null, null, null, null, null);

            verify(currentProcess).infanticide();
        }

        @Test
        void shouldForceKillChildProcessIfCalledMoreThanOnce() {
            builder.build(defaultGoPublisher, null, null, null, null, null);
            builder.build(defaultGoPublisher, null, null, null, null, null);

            verify(currentProcess).forceKillChildren();
        }
    }
}