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
package com.thoughtworks.go.server.service.lookups;

import java.util.Arrays;

import com.thoughtworks.go.helper.CommandSnippetMother;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandSnippetTest {
    @Test
    public void commandSnippetNameShouldComeFromCommentWhenItExists() throws Exception {
        CommandSnippetComment commandSnippetComment = mock(CommandSnippetComment.class);
        when(commandSnippetComment.getName()).thenReturn("name_in_comment");
        CommandSnippet commandSnippet = new CommandSnippet("ls", Arrays.asList("-la"), commandSnippetComment, "filename", "/windows/msbuild.xml");

        assertThat(commandSnippet.getName(), is("name_in_comment"));
    }

    @Test
    public void commandSnippetNameShouldBeFileNameWhenNoNameInComment() throws Exception {
        CommandSnippet commandSnippet = new CommandSnippet("ls", Arrays.asList("-la"), new EmptySnippetComment(), "filename", "/windows/msbuild.xml");
        assertThat(commandSnippet.getName(), is("filename"));

        CommandSnippetComment snippetComment = mock(CommandSnippetComment.class);
        when(snippetComment.getName()).thenReturn("");
        commandSnippet = new CommandSnippet("ls", Arrays.asList("-la"), snippetComment, "filename", "/windows/msbuild.xml");

        assertThat(commandSnippet.getName(), is("filename"));
    }

    @Test
    public void shouldAllowMatchesAgainstItsName() throws Exception {
        CommandSnippet snippet = CommandSnippetMother.validSnippet("some-name");
        assertThat(snippet.isExactMatchOfName("some"), is(false));
        assertThat(snippet.isExactMatchOfName("some-name"), is(true));
        assertThat(snippet.isExactMatchOfName("some-name-with-more-stuff"), is(false));
        assertThat(snippet.isExactMatchOfName("some-other-name"), is(false));

        assertThat(snippet.hasNameWhichStartsWith("some"), is(true));
        assertThat(snippet.hasNameWhichStartsWith("some-name"), is(true));
        assertThat(snippet.hasNameWhichStartsWith("some-name-with-more-stuff"), is(false));
        assertThat(snippet.hasNameWhichStartsWith("some-other-name"), is(false));
    }
}
