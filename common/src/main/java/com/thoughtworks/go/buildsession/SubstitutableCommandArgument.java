/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.command.CommandArgument;

public class SubstitutableCommandArgument extends CommandArgument {
    private final String argument;
    private final String substitution;

    public SubstitutableCommandArgument(String argument, String substitution) {
        this.argument = argument;
        this.substitution = substitution;
    }

    @Override
    public String originalArgument() {
        return argument;
    }

    @Override
    public String forDisplay() {
        return substitution;
    }

    @Override
    public String forCommandLine() {
        return originalArgument();
    }
}
