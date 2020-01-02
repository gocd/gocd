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
import com.thoughtworks.go.spark.Routes;

import java.util.List;

public class CommandSnippetsRepresenter {
    public static void toJSON(OutputWriter outputWriter, List<CommandSnippet> commandSnippets, String prefix) {
        outputWriter
            .addLinks(outputLinkWriter -> outputLinkWriter.addLink("self", Routes.InternalCommandSnippets.self(prefix)))
            .addChild("_embedded", embeddedWriter ->
                embeddedWriter.addChildList("command_snippets", commandSnippet ->
                    commandSnippets.forEach(snippet ->
                        commandSnippet.addChild(commandSnippetWriter ->
                            CommandSnippetRepresenter.toJSON(commandSnippetWriter, snippet)
                        )
                    )
                )
            );
    }
}
