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

import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.process.CurrentProcess;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;

public class BuilderForKillAllChildTask extends Builder {
    private CurrentProcess currentProcess;
    private boolean cancelAttempted = false;

    public BuilderForKillAllChildTask() {
        this(new RunIfConfigs(), new NullBuilder(), "Kills child processes", new CurrentProcess());
    }

    private BuilderForKillAllChildTask(RunIfConfigs conditions, Builder cancelBuilder, String description,
                                       CurrentProcess currentProcess) {
        super(conditions, cancelBuilder, description);
        this.currentProcess = currentProcess;
    }

    protected BuilderForKillAllChildTask(CurrentProcess currentProcess) {
        this(new RunIfConfigs(), new NullBuilder(), "Kills child processes", currentProcess);

    }

    @Override
    public void build(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext,
                      TaskExtension taskExtension, ArtifactExtension artifactExtension,
                      PluginRequestProcessorRegistry pluginRequestProcessorRegistry, String consoleLogCharset) {
        if (!cancelAttempted) {
            publisher.consumeLineWithPrefix("Attempting to kill child processes.");
            cancelAttempted = true;
            currentProcess.infanticide();
        } else {
            publisher.consumeLineWithPrefix("Attempting to force kill child processes.");
            currentProcess.forceKillChildren();
        }
    }

}
