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

import com.thoughtworks.go.util.command.CommandArgument;
import com.thoughtworks.go.util.command.StreamConsumer;

import java.util.List;

public class SafeStreamConsumer implements StreamConsumer {
    private final StreamConsumer consumer;
    private final List<CommandArgument> arguments;

    public SafeStreamConsumer(StreamConsumer consumer, List<CommandArgument> arguments) {
        this.consumer = consumer;
        this.arguments = arguments;
    }

    public void consumeLine(String line) {
        consumer.consumeLine(replaceSecretInfo(line));
    }

    private String replaceSecretInfo(String line) {
        for (CommandArgument argument : arguments) {
            line = line.replace(argument.forCommandline(), argument.forDisplay());
        }
        return line;
    }


}
