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

import com.thoughtworks.go.domain.BuildCommand;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

public class PluginCommandExecutor implements BuildCommandExecutor {
    private final TfsExecutor tfsExecutor;

    PluginCommandExecutor(TfsExecutor tfsExecutor) {
        this.tfsExecutor = tfsExecutor;
    }

    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        String type = command.getStringArg("type");

        if ("tfs".equals(type)) {
            return tfsExecutor.execute(command, buildSession);
        }

        throw bomb(format("Don't know how to handle plugin of type: %s", type));
    }
}
