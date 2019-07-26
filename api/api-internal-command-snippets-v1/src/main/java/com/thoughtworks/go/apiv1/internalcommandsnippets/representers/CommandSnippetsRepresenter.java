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
