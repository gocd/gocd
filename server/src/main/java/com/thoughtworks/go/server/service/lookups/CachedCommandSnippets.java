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

import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.thoughtworks.go.util.SystemEnvironment.COMMAND_REPOSITORY_CACHE_TIME_IN_SECONDS;
import static com.thoughtworks.go.util.SystemEnvironment.COMMAND_REPOSITORY_DIRECTORY;

@Component
public class CachedCommandSnippets {
    private static final Logger LOG = LoggerFactory.getLogger(CachedCommandSnippets.class);

    protected static final String CACHE_KEY = "COMMAND_SNIPPETS_CACHE";
    private CommandRepositoryDirectoryWalker commandRepositoryDirectoryWalker;
    private GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;

    private long lastCacheReloadTime;
    private GoCache goCache;
    private String previousRepositoryDirectory;

    @Autowired
    public CachedCommandSnippets(CommandRepositoryDirectoryWalker commandRepositoryDirectoryWalker, GoConfigService goConfigService, SystemEnvironment systemEnvironment,
                                 GoCache goCache) {
        this.commandRepositoryDirectoryWalker = commandRepositoryDirectoryWalker;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.goCache = goCache;
    }

    public CommandSnippets reload() {
        CommandSnippets snippets;
        LOG.info("[Command Repository] Reloading command snippets.");

        lastCacheReloadTime = new Date().getTime();
        synchronized (this) {
            previousRepositoryDirectory = getRepositoryDirectory();
            snippets = commandRepositoryDirectoryWalker.getAllCommandSnippets(previousRepositoryDirectory);
        }

        goCache.put(CACHE_KEY, snippets);

        LOG.debug("[Command Repository] Reloaded command snippets.");
        return snippets;
    }

    public CommandSnippets snippets() {
        CommandSnippets commandSnippets = (CommandSnippets) goCache.get(CACHE_KEY);

        if (shouldReload(commandSnippets)) {
            commandSnippets = reload();
        }
        return commandSnippets;
    }

    private boolean shouldReload(CommandSnippets existingCommandSnippets) {
        return existingCommandSnippetsDoNotExist(existingCommandSnippets) || commandRepoDirectoryHasBeenChanged() || cacheHasTimedOut();
    }

    private boolean existingCommandSnippetsDoNotExist(CommandSnippets existingCommandSnippets) {
        return existingCommandSnippets == null;
    }

    private boolean commandRepoDirectoryHasBeenChanged() {
        return !getRepositoryDirectory().equals(previousRepositoryDirectory);
    }

    private boolean cacheHasTimedOut() {
        long cacheTimeoutInMilliseconds = systemEnvironment.get(COMMAND_REPOSITORY_CACHE_TIME_IN_SECONDS) * 1000;
        long currentTime = new Date().getTime();
        long nextCacheReloadTime = lastCacheReloadTime + cacheTimeoutInMilliseconds;
        return currentTime > nextCacheReloadTime;
    }

    private String getRepositoryDirectory() {
        ServerConfig serverConfig = goConfigService.currentCruiseConfig().server();
        return String.format("%s/%s", systemEnvironment.get(COMMAND_REPOSITORY_DIRECTORY), serverConfig.getCommandRepositoryLocation());
    }
}
