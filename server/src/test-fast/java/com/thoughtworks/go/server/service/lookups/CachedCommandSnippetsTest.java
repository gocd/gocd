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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.helper.CommandSnippetMother.validSnippet;
import static com.thoughtworks.go.util.SystemEnvironment.COMMAND_REPOSITORY_DIRECTORY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CachedCommandSnippetsTest {
    private static final String EXPECTED_REPO_DIR = "some-directory/default";
    private static final String CUSTOM_REPO_DIR = "some-directory/custom";
    @Mock private SystemEnvironment systemEnvironment;
    @Mock private CommandRepositoryDirectoryWalker walker;
    @Mock private GoConfigService goConfigService;
    @Mock private GoCache goCache;
    private CachedCommandSnippets commandSnippetsCache;
    private CommandSnippets expectedSnippets;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        setupAFakeCommandRepoLocation("default");
        when(systemEnvironment.get(SystemEnvironment.COMMAND_REPOSITORY_CACHE_TIME_IN_SECONDS)).thenReturn(300);

        expectedSnippets = new CommandSnippets(Arrays.asList(validSnippet("snippet1")));
        when(walker.getAllCommandSnippets(EXPECTED_REPO_DIR)).thenReturn(expectedSnippets);

        commandSnippetsCache = new CachedCommandSnippets(walker, goConfigService, systemEnvironment, goCache);
    }

    @Test
    public void shouldFetchCommandSnippetsWhenCacheHasBeenJustCreated() throws Exception {
        when(goCache.get(CachedCommandSnippets.CACHE_KEY)).thenReturn(null);

        commandSnippetsCache.snippets();

        verify(walker).getAllCommandSnippets(EXPECTED_REPO_DIR);
        verify(goCache, times(1)).get(CachedCommandSnippets.CACHE_KEY);
    }

    @Test
    public void shouldFetchFromCacheIfAlreadyInCache() throws Exception {
        when(goCache.get(CachedCommandSnippets.CACHE_KEY)).thenReturn(null, expectedSnippets);

        CommandSnippets actualSnippetsWhichAreNotFromCache = commandSnippetsCache.snippets();
        CommandSnippets actualSnippetsWhichAreFromCache = commandSnippetsCache.snippets();

        verify(walker).getAllCommandSnippets(EXPECTED_REPO_DIR);
        verify(goCache, times(2)).get(CachedCommandSnippets.CACHE_KEY);
        assertThat(expectedSnippets, is(actualSnippetsWhichAreNotFromCache));
        assertThat(expectedSnippets, is(actualSnippetsWhichAreFromCache));
    }

    @Test
    public void shouldAllowForcefullyReloadingCache() throws Exception {
        when(goCache.get(CachedCommandSnippets.CACHE_KEY)).thenReturn(null, expectedSnippets);

        commandSnippetsCache.snippets();
        commandSnippetsCache.reload();
        commandSnippetsCache.snippets();

        verify(walker, times(2)).getAllCommandSnippets(EXPECTED_REPO_DIR);
    }

    @Test
    public void shouldTimeoutCacheAndReloadAfterCacheHasExpiredBecauseOfTime() throws Exception {
        when(goCache.get(CachedCommandSnippets.CACHE_KEY)).thenReturn(null, expectedSnippets);
        when(systemEnvironment.get(SystemEnvironment.COMMAND_REPOSITORY_CACHE_TIME_IN_SECONDS)).thenReturn(1);

        commandSnippetsCache.snippets();
        Thread.sleep(2 * 1000);
        commandSnippetsCache.snippets();

        verify(walker, times(2)).getAllCommandSnippets(EXPECTED_REPO_DIR);
    }

    @Test
    public void shouldReloadCacheWhenRepoLocationHasBeenModified() throws Exception {
        CommandSnippets expectedSnippetsFromCustomRepo = new CommandSnippets(Arrays.asList(validSnippet("snippet2")));
        when(goCache.get(CachedCommandSnippets.CACHE_KEY)).thenReturn(null, expectedSnippets);
        when(walker.getAllCommandSnippets(CUSTOM_REPO_DIR)).thenReturn(expectedSnippetsFromCustomRepo);
        setupAFakeCommandRepoLocation("default", "custom");

        CommandSnippets snippetsFromDefaultRepo = commandSnippetsCache.snippets();
        CommandSnippets snippetsFromCustomRepo = commandSnippetsCache.snippets();

        verify(walker).getAllCommandSnippets(EXPECTED_REPO_DIR);
        verify(walker).getAllCommandSnippets(CUSTOM_REPO_DIR);
        assertThat(snippetsFromDefaultRepo, is(expectedSnippets));
        assertThat(snippetsFromCustomRepo, is(expectedSnippetsFromCustomRepo));
    }

    @Test(timeout = 10 * 1000)
    public void shouldWorkWhenThereAreThreadsTryingToAccessAndReloadCommandSnippets() throws Exception {
        when(systemEnvironment.get(SystemEnvironment.COMMAND_REPOSITORY_CACHE_TIME_IN_SECONDS)).thenReturn(1);

        Thread threadWhichAccessesSnippets = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 200; i++) {
                    commandSnippetsCache.snippets();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        Thread threadWhichReloadsSnippets = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 200; i++) {
                    commandSnippetsCache.reload();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        List<Throwable> exceptionsInTheThreads = new ArrayList<>();
        threadWhichAccessesSnippets.setUncaughtExceptionHandler(uncaughtExceptionHandler(exceptionsInTheThreads));
        threadWhichReloadsSnippets.setUncaughtExceptionHandler(uncaughtExceptionHandler(exceptionsInTheThreads));

        threadWhichAccessesSnippets.start();
        threadWhichReloadsSnippets.start();

        threadWhichAccessesSnippets.join();
        threadWhichReloadsSnippets.join();

        assertThat(exceptionsInTheThreads, is(Collections.<Throwable>emptyList()));
    }

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler(final List<Throwable> exceptionsInTheThreads) {
        return new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                exceptionsInTheThreads.add(e);
            }
        };
    }

    private void setupAFakeCommandRepoLocation(final String location, final String... otherLocations) {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        ServerConfig serverConfig = mock(ServerConfig.class);

        when(systemEnvironment.get(COMMAND_REPOSITORY_DIRECTORY)).thenReturn("some-directory");
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.server()).thenReturn(serverConfig);
        when(serverConfig.getCommandRepositoryLocation()).thenReturn(location, otherLocations);
    }
}
