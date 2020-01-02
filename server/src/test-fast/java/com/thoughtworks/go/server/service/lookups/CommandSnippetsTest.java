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
import java.util.List;

import com.thoughtworks.go.helper.CommandSnippetMother;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class CommandSnippetsTest {
    @Test
    public void shouldOrderSnippetsByName() {
        CommandSnippet maven = CommandSnippetMother.validSnippet("maven");
        CommandSnippet copy = CommandSnippetMother.validSnippet("copy");
        CommandSnippet scp = CommandSnippetMother.validSnippet("scp");

        final List<CommandSnippet> snippets = asList(maven, copy, scp);
        CommandSnippets actual = new CommandSnippets(snippets);

        assertThat(actual, is(new CommandSnippets(Arrays.asList(copy, maven, scp))));
    }

    @Test
    public void shouldLookupCommandByMatchingPrefixAgainstStartOfSnippetNameAndOrderByBestMatch() {
        CommandSnippet scp = CommandSnippetMother.validSnippet("scp");
        CommandSnippet scp_copy = CommandSnippetMother.validSnippet("scp-copy");
        CommandSnippet copy_scp = CommandSnippetMother.validSnippet("copy-scp");

        CommandSnippets snippets = new CommandSnippets(Arrays.asList(scp_copy, scp, copy_scp));
        assertThat(snippets.filterBy("scp"), is(Arrays.asList(scp, scp_copy)));
    }

    @Test
    public void shouldLookupCommandByMatchingPrefixAgainstFullKeywordAndOrderByBestMatch() {
        CommandSnippet copy = CommandSnippetMother.validSnippetWithKeywords("COPY", "someotherthing");
        CommandSnippet robocopy = CommandSnippetMother.validSnippetWithKeywords("robocopy", "cOpY", "windows");
        CommandSnippet scp = CommandSnippetMother.validSnippetWithKeywords("scp", "copy", "ssh");

        CommandSnippets snippets = new CommandSnippets(Arrays.asList(copy, robocopy, scp));
        assertThat(snippets.filterBy("scp"), is(Arrays.asList(scp)));
        assertThat(snippets.filterBy("copy"), is(Arrays.asList(copy, robocopy, scp)));
        assertThat(snippets.filterBy("Copy"), is(Arrays.asList(copy, robocopy, scp)));
        assertThat(snippets.filterBy("windows"), is(Arrays.asList(robocopy)));
        assertThat(snippets.filterBy("wind"), is(Collections.<CommandSnippet>emptyList()));
        assertThat(snippets.filterBy("obo"), is(Collections.<CommandSnippet>emptyList()));
    }

    @Test
    public void shouldAllowNumbersAndSpacesInNameAndKeywords() throws Exception {
        CommandSnippet copy = CommandSnippetMother.validSnippetWithKeywords("COPY", "some other thing");
        CommandSnippet robocopy = CommandSnippetMother.validSnippetWithKeywords("robocopy", "cOpY12", "windows");
        CommandSnippet scp = CommandSnippetMother.validSnippetWithKeywords("scp scp", "copy12", "ssh");

        CommandSnippets snippets = new CommandSnippets(Arrays.asList(copy, robocopy, scp));
        assertThat(snippets.filterBy("scp"), is(Arrays.asList(scp)));
        assertThat(snippets.filterBy("scp scp"), is(Arrays.asList(scp)));
        assertThat(snippets.filterBy("copy"), is(Arrays.asList(copy)));
        assertThat(snippets.filterBy("copy12"), is(Arrays.asList(robocopy, scp)));
        assertThat(snippets.filterBy("some other thing"), is(Arrays.asList(copy)));
    }

    @Test
    public void shouldGetAllInvalidCommandSnippets() {
        CommandSnippet invalidSnippet1 = CommandSnippetMother.invalidSnippetWithEmptyCommand("file1");
        CommandSnippet validSnippet1 = CommandSnippetMother.validSnippet("file2");
        CommandSnippet invalidSnippet2 = CommandSnippetMother.invalidSnippetWithEmptyCommand("file3");
        CommandSnippet validSnippet2 = CommandSnippetMother.validSnippet("file4");

        CommandSnippets snippets = new CommandSnippets(Arrays.asList(invalidSnippet1, validSnippet1, invalidSnippet2, validSnippet2));
        assertThat(snippets.invalidSnippets(), is(Arrays.asList(invalidSnippet1, invalidSnippet2)));
    }

    @Test
    public void shouldGetTaskSnippetByFileName() {
        CommandSnippet maven_clean = CommandSnippetMother.validSnippetWithFileName("maven", "maven-clean");
        CommandSnippet maven_package = CommandSnippetMother.validSnippetWithFileName("maven", "maven-package");

        CommandSnippets commandSnippets = new CommandSnippets(asList(maven_clean, maven_package));
        assertThat(commandSnippets.findByRelativePath("/some/path/maven-clean.xml"), is(maven_clean));
        assertThat(commandSnippets.findByRelativePath("/some/path/maven-package.xml"), is(maven_package));
        assertThat(commandSnippets.findByRelativePath("/some/path/rake-package.xml"), is(nullValue()));
    }

    @Test
    public void shouldNotConsiderInvalidSnippetsDuringFiltering() throws Exception {
        CommandSnippet invalidSnippet1 = CommandSnippetMother.invalidSnippetWithEmptyCommand("file1");
        CommandSnippet validSnippet1 = CommandSnippetMother.validSnippet("file2");
        CommandSnippet invalidSnippet2 = CommandSnippetMother.invalidSnippetWithEmptyCommand("file3");
        CommandSnippet validSnippet2 = CommandSnippetMother.validSnippet("file4");

        CommandSnippets snippets = new CommandSnippets(Arrays.asList(invalidSnippet1, validSnippet1, invalidSnippet2, validSnippet2));
        assertThat(snippets.filterBy("file"), is(Arrays.asList(validSnippet1, validSnippet2)));
    }
}
