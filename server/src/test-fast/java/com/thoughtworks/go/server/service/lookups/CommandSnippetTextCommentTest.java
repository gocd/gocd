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
import java.util.Collections;

import com.thoughtworks.go.helper.CommandSnippetMother;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class CommandSnippetTextCommentTest {
    @Test
    public void shouldParseSnippetTagsFromCommentWithLowerCasingOfKeywords() {
        CommandSnippetXmlParser.CommandSnippetTextComment comment = new CommandSnippetXmlParser.CommandSnippetTextComment(CommandSnippetMother.commandSnippetComment("robo copy"));
        assertThat(comment.getName(), is("robo copy"));
        assertThat(comment.getDescription(), is("some-description"));
        assertThat(comment.getAuthor(), is("Go Team"));
        assertThat(comment.getKeywords(), is(Arrays.asList("file", "doc transfer", "copy")));
        assertThat(comment.getAuthorInfo(), is("TWEr@thoughtworks.com"));
        assertThat(comment.getMoreInfo(), is("http://some-url"));
    }

    @Test
    public void shouldParseSnippetTagsFromCommentWhenNewlinesAreCRLF() {
        String commentContentWithCRLF = CommandSnippetMother.commandSnippetComment("robo copy").replaceAll("\n", "\r\n");
        CommandSnippetXmlParser.CommandSnippetTextComment comment = new CommandSnippetXmlParser.CommandSnippetTextComment(commentContentWithCRLF);

        assertThat(comment.getName(), is("robo copy"));
        assertThat(comment.getDescription(), is("some-description"));
        assertThat(comment.getAuthor(), is("Go Team"));
        assertThat(comment.getKeywords(), is(Arrays.asList("file", "doc transfer", "copy")));
        assertThat(comment.getAuthorInfo(), is("TWEr@thoughtworks.com"));
        assertThat(comment.getMoreInfo(), is("http://some-url"));
    }

    @Test
    public void shouldGetAnEmptyListAsKeywordsWhenTheKeywordsLineIsMissingInTheComment() throws Exception {
        CommandSnippetXmlParser.CommandSnippetTextComment comment = new CommandSnippetXmlParser.CommandSnippetTextComment("name: robocopy\n");
        assertThat(comment.getKeywords(), is(Collections.<String>emptyList()));
    }

    @Test
    public void shouldNotFailIfACommentDoesNotHaveSomeFields() throws Exception {
        CommandSnippetXmlParser.CommandSnippetTextComment comment = new CommandSnippetXmlParser.CommandSnippetTextComment("   description: blah-description\n  someotherthing: blah\nauthor: hello");
        assertThat(comment.getName(), is(nullValue()));
        assertThat(comment.getDescription(), is("blah-description"));
        assertThat(comment.getAuthor(), is("hello"));
        assertThat(comment.getKeywords(), is(Collections.<String>emptyList()));
        assertThat(comment.getAuthorInfo(), is(nullValue()));
        assertThat(comment.getMoreInfo(), is(nullValue()));
    }
}

