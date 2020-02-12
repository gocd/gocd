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
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.domain.materials.git.builder.GitLogCommandBuilder;
import com.thoughtworks.go.util.command.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class GitLogCommandBuilderTest {

    @Test
    void shouldBuildGitLogCommandToGetLatestModification() {
        CommandLine command = new GitLogCommandBuilder()
                .latestCommit()
                .build();

        assertThat(command.toString()).isEqualTo("git log --no-color -1");
    }

    @Test
    void shouldBuildGitLogCommandToGetLastModificationWithRemoteBranch() {
        CommandLine command = new GitLogCommandBuilder()
                .latestCommit()
                .withRemoteBranch("origin/SomeBranch")
                .build();

        assertThat(command.toString()).isEqualTo("git log --no-color -1 origin/SomeBranch");
    }

    @Test
    void shouldBuildGitLogCommandToGetModificationSinceRevision() {
        CommandLine command = new GitLogCommandBuilder()
                .between("45a76368ed", "origin/SomeBranch")
                .build();

        assertThat(command.toString()).isEqualTo("git log --no-color 45a76368ed..origin/SomeBranch");
    }

    @Test
    void shouldBuildGitLogCommandWithWorkingDirectory(@TempDir File workingDir) {
        CommandLine command = new GitLogCommandBuilder()
                .between("45a76368ed", "origin/SomeBranch")
                .withWorkingDir(workingDir)
                .build();

        assertThat(command.getWorkingDirectory()).isEqualTo(workingDir);
    }

    @Test
    void shouldSetFormatArg() {
        CommandLine command = new GitLogCommandBuilder()
                .latestCommit()
                .outputFormatYaml()
                .build();

        assertThat(command.toString()).contains("--pretty=format:");
    }
}
