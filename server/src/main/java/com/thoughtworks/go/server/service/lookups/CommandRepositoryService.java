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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandRepositoryService {
    private CachedCommandSnippets cache;

    @Autowired
    public CommandRepositoryService(CachedCommandSnippets commandSnippetsCache) {
        this.cache = commandSnippetsCache;
    }

    public List<CommandSnippet> lookupCommand(String prefix) {
        return getAllCommandSnippets().filterBy(prefix);
    }

    public List<CommandSnippet> getAllInvalidCommandSnippets() {
        return getAllCommandSnippets().invalidSnippets();
    }

    public CommandSnippet getCommandSnippetByRelativePath(final String snippetRelativePath) {
        return getAllCommandSnippets().findByRelativePath(snippetRelativePath);
    }

    public void reloadCache() {
        cache.reload();
    }

    private CommandSnippets getAllCommandSnippets() {
        return cache.snippets();
    }
}

