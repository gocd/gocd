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
import java.util.List;

import com.thoughtworks.go.helper.CommandSnippetMother;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommandRepositoryServiceTest {
    private CommandRepositoryService commandRepositoryService;
    private CommandSnippets snippets;
    private CachedCommandSnippets cache;

    @Before
    public void setUp() throws Exception {
        snippets = mock(CommandSnippets.class);
        cache = mock(CachedCommandSnippets.class);

        when(cache.snippets()).thenReturn(snippets);
        commandRepositoryService = new CommandRepositoryService(cache);
    }

    @Test
    public void shouldLookupCommands(){
        String prefix = "some-prefix";
        List<CommandSnippet> matchedSnippets = Arrays.asList(CommandSnippetMother.validSnippet("maven"));
        when(snippets.filterBy(prefix)).thenReturn(matchedSnippets);

        assertThat(commandRepositoryService.lookupCommand(prefix), is(matchedSnippets));
    }

    @Test
    public void shouldFindAllInvalidCommands(){
        List<CommandSnippet> invalidSnippets = Arrays.asList(CommandSnippetMother.validSnippet("maven"));
        when(snippets.invalidSnippets()).thenReturn(invalidSnippets);

        assertThat(commandRepositoryService.getAllInvalidCommandSnippets(), is(invalidSnippets));
    }

    @Test
    public void shouldFindSnippetByRelativePath(){
        String fileName = "/some-filename.xml";
        CommandSnippet snippetWhichMatches = CommandSnippetMother.validSnippet("maven");
        when(snippets.findByRelativePath(fileName)).thenReturn(snippetWhichMatches);

        assertThat(commandRepositoryService.getCommandSnippetByRelativePath(fileName), is(snippetWhichMatches));
    }

    @Test
    public void shouldReloadCache(){
        commandRepositoryService.reloadCache();

        verify(cache).reload();
    }
}
