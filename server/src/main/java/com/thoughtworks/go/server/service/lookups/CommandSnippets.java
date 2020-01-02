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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.apache.commons.collections4.IterableUtils.find;


public class CommandSnippets {
    private List<CommandSnippet> snippets;

    public CommandSnippets(List<CommandSnippet> snippets) {
        this.snippets = sortByName(snippets);
    }

    public List<CommandSnippet> invalidSnippets() {
        ArrayList<CommandSnippet> invalidSnippets = new ArrayList<>();
        for (CommandSnippet commandSnippet : snippets) {
            if (!commandSnippet.isValid()) {
                invalidSnippets.add(commandSnippet);
            }
        }
        return invalidSnippets;
    }

    public List<CommandSnippet> filterBy(String prefix) {
        String prefixToCheckAgainst = prefix.toLowerCase();

        ArrayList<CommandSnippet> exactNameMatchingSnippets = new ArrayList<>();
        ArrayList<CommandSnippet> partialNameMatchingSnippets = new ArrayList<>();
        ArrayList<CommandSnippet> exactKeywordMatchingSnippets = new ArrayList<>();
        for (CommandSnippet snippet : snippets) {
            if (!snippet.isValid()) {
                continue;
            }
            if (snippet.isExactMatchOfName(prefixToCheckAgainst)) {
                exactNameMatchingSnippets.add(snippet);
                continue;
            }
            if (snippet.hasNameWhichStartsWith(prefixToCheckAgainst)) {
                partialNameMatchingSnippets.add(snippet);
                continue;
            }
            if (snippet.containsKeyword(prefixToCheckAgainst)) {
                exactKeywordMatchingSnippets.add(snippet);
            }
        }
        ArrayList<CommandSnippet> matchedSnippets = new ArrayList<>();
        matchedSnippets.addAll(exactNameMatchingSnippets);
        matchedSnippets.addAll(partialNameMatchingSnippets);
        matchedSnippets.addAll(exactKeywordMatchingSnippets);
        return matchedSnippets;
    }

    public CommandSnippet findByRelativePath(final String snippetRelativePath) {
        return find(snippets, commandSnippet -> commandSnippet.getRelativePath().equals(snippetRelativePath));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CommandSnippets that = (CommandSnippets) o;

        if (snippets != null ? !snippets.equals(that.snippets) : that.snippets != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return snippets != null ? snippets.hashCode() : 0;
    }

    private List<CommandSnippet> sortByName(List<CommandSnippet> snippets) {
        List<CommandSnippet> snippetsSortedByName = new ArrayList<>(snippets);
        snippetsSortedByName.sort(Comparator.comparing(CommandSnippet::getName));
        return snippetsSortedByName;
    }

    public List<CommandSnippet> getSnippets() {
        return snippets;
    }

    @Override
    public String toString() {
        return "CommandSnippets{snippets=" + snippets + '}';
    }
}
