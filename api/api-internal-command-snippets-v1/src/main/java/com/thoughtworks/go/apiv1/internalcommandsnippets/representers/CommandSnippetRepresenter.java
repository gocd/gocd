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
