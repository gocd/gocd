/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.git.builder;

import com.thoughtworks.go.domain.materials.git.GitLog;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.SecretString;

import java.io.File;
import java.util.List;

public class GitLogCommandBuilder implements Builder {
    private boolean isFormatted = false;
    private final CommandLine git = CommandLine.createCommandLine("git")
            .withEncoding("UTF-8")
            .withArg("log")
            .withArg("--no-color");

    private static final OutputFormatter FORMATTER = OutputFormatter.newFormatter(GitLog.class)
            .withCommitHash()
            .withAuthorName()
            .withAuthorEmail()
            .withDate()
            .withSubject()
            .withRawBody()
            .withAdditionalInfo();

    public WithBranchBuilder latestCommit() {
        git.withArg("-1");
        return new WithBranchBuilder(this, git);
    }

    public Builder between(String fromRevision, String toRevision) {
        git.withArg(String.format("%s..%s", fromRevision, toRevision));
        return this;
    }

    public GitLogCommandBuilder withNonArgSecrets(List<SecretString> secrets) {
        git.withNonArgSecrets(secrets);
        return this;
    }

    public Builder outputFormatYaml() {
        if (isFormatted) {
            return this;
        }

        git.withArg(String.format("--pretty=format:%s", FORMATTER.format()));
        isFormatted = true;
        return this;
    }

    @Override
    public Builder withWorkingDir(File workingDir) {
        this.git.withWorkingDir(workingDir);
        return this;
    }

    @Override
    public CommandLine build() {
        return git;
    }

}

