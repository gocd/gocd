package com.thoughtworks.go.apiv1.internalcommandsnippets.representers

import com.thoughtworks.go.server.service.lookups.CommandSnippet
import com.thoughtworks.go.server.service.lookups.EmptySnippetComment
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class CommandSnippetRepresenterTest {
  @Test
  void 'should represent command snippet'() {
    CommandSnippet commandSnippet = new CommandSnippet("ls", Arrays.asList("-la"), new EmptySnippetComment(), "filename", "/windows/msbuild.xml")
    def json = toObjectString({ CommandSnippetRepresenter.toJSON(it, commandSnippet) })

    assertThatJson(json).isEqualTo([
      "name"         : commandSnippet.name,
      "description"  : commandSnippet.description,
      "author"       : commandSnippet.author,
      "author_info"  : commandSnippet.authorInfo,
      "more_info"    : commandSnippet.moreInfo,
      "command"      : commandSnippet.commandName,
      "arguments"    : commandSnippet.arguments,
      "relative_path": commandSnippet.relativePath
    ])
  }
}
