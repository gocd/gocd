/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util.command;

import java.util.ArrayList;
import java.util.List;

public class StringArgument extends CommandArgument {
    private String value;

    public StringArgument(String value) {
        this.value = value;
    }

    @Override
    public String rawUrl() {
        return value;
    }

    /**
     * This is final to prevent subclasses from exposing data they shouldn't in logs etc.
     */
    @Override
    public String forDisplay() {
        return value;
    }

    @Override
    public String forCommandLine() {
        return rawUrl();
    }

    public static CommandArgument[] toArgs(Object... inputs) {
        List<CommandArgument> args = new ArrayList<>();
        for (Object input : inputs) {
            if (input instanceof CommandArgument) {
                args.add((CommandArgument) input);
            } else {
                args.add(new StringArgument(input.toString()));
            }
        }
        return args.toArray(new CommandArgument[0]);
    }
}
