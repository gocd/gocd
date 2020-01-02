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
package com.thoughtworks.go.apiv1.internalcommandsnippets.representers

import com.thoughtworks.go.server.service.lookups.CommandSnippet
import com.thoughtworks.go.server.service.lookups.EmptySnippetComment
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Arrays.asList
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class CommandSnippetsRepresenterTest {
  @Test
  void 'should represent command snippets'() {
    CommandSnippet commandSnippet1 = new CommandSnippet("ls", asList("-la"), new EmptySnippetComment(), "filename", "/windows/msbuild.xml")
    CommandSnippet commandSnippet2 = new CommandSnippet("ls", asList("-alh", "/tmp"), new EmptySnippetComment(), "something", "/windows/foobar")
    List<CommandSnippet> snippets = asList(commandSnippet1, commandSnippet2)

    def json = toObjectString({ CommandSnippetsRepresenter.toJSON(it, snippets, "xyz") })

    assertThatJson(json).isEqualTo([
      "_links"   : [
        "self": ["href": "http://test.host/go/api/admin/internal/command_snippets?prefix=xyz"]
      ],
      "_embedded": [
        "command_snippets": [
          [
            "name"         : commandSnippet1.name,
            "description"  : commandSnippet1.description,
            "author"       : commandSnippet1.author,
            "author_info"  : commandSnippet1.authorInfo,
            "more_info"    : commandSnippet1.moreInfo,
            "command"      : commandSnippet1.commandName,
            "arguments"    : commandSnippet1.arguments,
            "relative_path": commandSnippet1.relativePath
          ],
          [
            "name"         : commandSnippet2.name,
            "description"  : commandSnippet2.description,
            "author"       : commandSnippet2.author,
            "author_info"  : commandSnippet2.authorInfo,
            "more_info"    : commandSnippet2.moreInfo,
            "command"      : commandSnippet2.commandName,
            "arguments"    : commandSnippet2.arguments,
            "relative_path": commandSnippet2.relativePath
          ]
        ]
      ]
    ])
  }

  @Test
  void 'should represent empty command snippets'() {
    def json = toObjectString({ CommandSnippetsRepresenter.toJSON(it, [], "xyz") })

    assertThatJson(json).isEqualTo([
      "_links"   : [
        "self": ["href": "http://test.host/go/api/admin/internal/command_snippets?prefix=xyz"]
      ],
      "_embedded": [
        "command_snippets": []
      ]
    ])
  }
}