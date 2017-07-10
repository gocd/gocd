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

import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.AgentSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.LabeledOutputStreamConsumer;
import com.thoughtworks.go.util.command.TaggedStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;

import java.io.File;

public class TfsExecutor implements BuildCommandExecutor {

    TfsMaterial tfsMaterial;

    public TfsExecutor() {
        tfsMaterial = null;
    }

    public TfsExecutor(TfsMaterial tfsMaterial) {
        this.tfsMaterial = tfsMaterial;
    }

    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        String url = command.getStringArg("url");
        String username = command.getStringArg("username");
        String password = command.getStringArg("password");
        String domain = command.getStringArg("domain");
        String projectPath = command.getStringArg("projectPath");
        String revision = command.getStringArg("revision");
        File workingDir = buildSession.resolveRelativeDir(command.getWorkingDirectory());

        ConsoleOutputStreamConsumer consoleOutputStreamConsumer = new LabeledOutputStreamConsumer(TaggedStreamConsumer.PREP, TaggedStreamConsumer.PREP_ERR,
                buildSession.processOutputStreamConsumer());
        RevisionContext revisionContext = new RevisionContext(new StringRevision(revision));
        AgentSubprocessExecutionContext execCtx = new AgentSubprocessExecutionContext(buildSession.getAgentIdentifier(), workingDir.getAbsolutePath());

        tfsMaterial = (tfsMaterial == null) ? new TfsMaterial(new GoCipher(), new UrlArgument(url), username, domain, password, projectPath) : tfsMaterial;
        tfsMaterial.updateTo(consoleOutputStreamConsumer, workingDir, revisionContext, execCtx);

        return true;
    }
}
