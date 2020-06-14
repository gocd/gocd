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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.server.service.EntityHashes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.config.remote.ConfigRepoConfig.createConfigRepoConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.gitMaterialConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig;
import static com.thoughtworks.go.helper.PartialConfigMother.withPipeline;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PartialConfigHelperTest {
    @Mock
    private EntityHashes hasher;
    private PartialConfigHelper helper;

    @BeforeEach
    void setup() {
        initMocks(this);
        helper = new PartialConfigHelper(hasher);
        when(hasher.digestPartial(any(PartialConfig.class))).thenAnswer((Answer<String>) invocation -> {
            final PartialConfig partial = invocation.getArgument(0);
            return Integer.toString(partial.hashCode());
        });
    }

    @Test
    void isEquivalent_PartialConfig() {
        assertTrue(helper.isEquivalent(git("1"), git("1")));

        assertFalse(helper.isEquivalent(git("1"), git("2")));
        assertFalse(helper.isEquivalent(svn("1"), git("1")));

        // handles null
        assertFalse(helper.isEquivalent(null, git("1")));
        assertFalse(helper.isEquivalent(git("1"), null));
    }

    @Test
    void isEquivalent_CollectionPartialConfig_returnsTrueWhenBothCollectionsAreEmpty() {
        assertTrue(helper.isEquivalent(null, Collections.emptyList()));
        assertTrue(helper.isEquivalent(Collections.emptyList(), null));
        assertTrue(helper.isEquivalent(Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    void isEquivalent_CollectionPartialConfig_returnsTrueWhenGivenEquivalentCollections() {
        List<PartialConfig> a = asList(git("1"), svn("1"));
        List<PartialConfig> b = asList(git("1"), svn("1"));
        assertTrue(helper.isEquivalent(a, b));
    }

    @Test
    void isEquivalent_CollectionPartialConfig_returnsTrueWhenGivenDifferentCollections() {
        List<PartialConfig> a = asList(git("1"), svn("1"));
        List<PartialConfig> b = asList(git("2"), svn("2"));
        assertFalse(helper.isEquivalent(a, b));
        assertFalse(helper.isEquivalent(a, Collections.emptyList()));
    }

    private PartialConfig git(String name) {
        ConfigRepoConfig repo = createConfigRepoConfig(gitMaterialConfig(), "plugin", "id" + name);
        return withPipeline("p" + name, new RepoConfigOrigin(repo, "git_r" + name));
    }

    private PartialConfig svn(String name) {
        ConfigRepoConfig repo = createConfigRepoConfig(svnMaterialConfig(), "plugin", "id" + name);
        return withPipeline("p" + name, new RepoConfigOrigin(repo, "git_r" + name));
    }
}