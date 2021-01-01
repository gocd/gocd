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
package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.util.command.CommandLine;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
public class CommandBuilderTest {

    private File tempWorkDir;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();


    @BeforeEach
    void setUp() throws IOException {
        temporaryFolder.create();
        tempWorkDir = temporaryFolder.newFolder();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void commandWithArgsList_shouldAddCmdBeforeAWindowsCommand() {
        String[] args = {"some thing"};
        CommandBuilderWithArgList commandBuilderWithArgList = new CommandBuilderWithArgList("echo", args, tempWorkDir, null, null, "some desc");
        CommandLine commandLine = commandBuilderWithArgList.buildCommandLine();
        assertThat(commandLine.toStringForDisplay()).isEqualTo("cmd /c echo some thing");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void commandWithArgs_shouldAddCmdBeforeAWindowsCommand() {
        CommandBuilder commandBuilder = new CommandBuilder("echo", "some thing", tempWorkDir, null, null, "some desc");
        CommandLine commandLine = commandBuilder.buildCommandLine();
        assertThat(commandLine.toStringForDisplay()).isEqualTo("cmd /c echo some thing");
    }
}
