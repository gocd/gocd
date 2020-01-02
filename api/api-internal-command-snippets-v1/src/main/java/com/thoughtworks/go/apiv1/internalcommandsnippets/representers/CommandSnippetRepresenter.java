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
package com.thoughtworks.go.apiv1.internalcommandsnippets.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.server.service.lookups.CommandSnippet;

public class CommandSnippetRepresenter {
    public static void toJSON(OutputWriter outputWriter, CommandSnippet commandSnippet) {
        outputWriter.add("name", commandSnippet.getName())
            .add("description", commandSnippet.getDescription())
            .add("author", commandSnippet.getAuthor())
            .add("author_info", commandSnippet.getAuthorInfo())
            .add("more_info", commandSnippet.getMoreInfo())
            .add("command", commandSnippet.getCommandName())
            .addChildList("arguments", commandSnippet.getArguments())
            .add("relative_path", commandSnippet.getRelativePath());
    }
}
