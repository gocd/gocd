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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.domain.NullPipeline;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ExecTaskBuilderTest {
    private UpstreamPipelineResolver resolver;
    private ExecTaskBuilder execTaskBuilder;
    private BuilderFactory builderFactory;

    static Pipeline pipelineStub(final String label, final String defaultWorkingFolder) {
        return new NullPipeline() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public File defaultWorkingFolder() {
                return new File(defaultWorkingFolder);
            }
        };
    }

    @BeforeEach
    void setUp() {
        resolver = mock(UpstreamPipelineResolver.class);
        builderFactory = mock(BuilderFactory.class);
        execTaskBuilder = new ExecTaskBuilder();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Test
    void shouldUseProjectDefaultWorkingDirectoryIfNotSpecified() {
        ExecTask task = new ExecTask("command", "", (String) null);
        final File defaultWorkingDir = new File("foo");

        CommandBuilder builder = (CommandBuilder) execTaskBuilder.createBuilder(builderFactory, task, pipelineStub("label", "foo"), resolver);

        assertThat(builder.getWorkingDir()).isEqualTo(defaultWorkingDir);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldNormalizeWorkingDirectory() {
        ExecTask execTask = new ExecTask("ant", "", "folder\\child");

        CommandBuilder builder = (CommandBuilder) execTaskBuilder.createBuilder(builderFactory, execTask, pipelineStub("label", "."), resolver);

        assertThat(builder.getWorkingDir().getPath()).isEqualTo("./folder/child");
    }
}
