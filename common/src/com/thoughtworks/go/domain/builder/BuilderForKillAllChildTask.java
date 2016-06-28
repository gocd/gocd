/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.builder;

import com.jezhumble.javasysmon.JavaSysMon;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.BuildLogElement;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;

public class BuilderForKillAllChildTask extends Builder {
    public BuilderForKillAllChildTask() {
        super(new RunIfConfigs(), new NullBuilder(), "Kills child processes");
    }

    @Override
    public void build(BuildLogElement buildLogElement, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension) throws CruiseControlException {
        new JavaSysMon().infanticide();
    }

    @Override
    public BuildCommand buildCommand() {
        return BuildCommand.noop(); //killing sub process is built in for build command protocol agent
    }
}
