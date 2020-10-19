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
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.service.EntityHashes;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.config.remote.ConfigRepoConfig.createConfigRepoConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.gitMaterialConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig;
import static com.thoughtworks.go.helper.PartialConfigMother.withPipeline;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class PartialConfigHelperTest {
    private PartialConfigHelper helper;
    private EntityHashes hashes;

    @BeforeEach
    void setup() {
        hashes = new EntityHashes(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins());
        helper = new PartialConfigHelper(hashes);
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

    @Test
    void isEquivalent_CollectionPartialConfig_shouldNotFailWhenSerializationToXMLFails() {
        helper = new PartialConfigHelper(hashes) {
            @Override
            protected String digestPartial(PartialConfig partial) {
                throw new RuntimeException("oh no I'm dead!");
            }
        };

        ExecTask invalidExecTask = new ExecTask("docker", new Arguments(new Argument(null)));
        JobConfig invalidJob = new JobConfig("up42_job");
        invalidJob.addTask(invalidExecTask);

        ConfigRepoConfig repo = createConfigRepoConfig(svnMaterialConfig(), "plugin", "id");
        PartialConfig partialConfig = withPipeline("p", new RepoConfigOrigin(repo, "git"));
        partialConfig.getGroups().get(0).get(0).first().getJobs().add(invalidJob);

        List<PartialConfig> a = asList(partialConfig, git("git"));
        List<PartialConfig> b = Collections.emptyList();

        final Boolean result = assertDoesNotThrow(() -> helper.isEquivalent(a, b));
        assertFalse(result);
    }

    @Test
    void hash() {
        assertEquals(
                helper.hash(PartialConfigMother.withPipeline("foo")),
                helper.hash(PartialConfigMother.withPipeline("foo")),
                "Given structurally equal partials, digestPartial() should output the same digest"
        );

        assertNotEquals(
                helper.hash(PartialConfigMother.withPipeline("foo")),
                helper.hash(PartialConfigMother.withPipeline("bar")),
                "Given structurally different partials, digestPartial() should output different digests"
        );

        PartialConfig a = PartialConfigMother.withPipeline("foo");
        PartialConfig b = PartialConfigMother.withPipeline("foo");
        b.setOrigin(new RepoConfigOrigin(((RepoConfigOrigin) b.getOrigin()).getConfigRepo(), "something-else"));

        assertEquals(
                helper.hash(a),
                helper.hash(b),
                "digestPartial() should only consider structure and not metadata (e.g., origin)"
        );
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
