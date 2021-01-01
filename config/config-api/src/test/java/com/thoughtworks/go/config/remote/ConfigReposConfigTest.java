/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ConfigReposConfigTest {
    private ConfigReposConfig repos;

    @Before
    public void setUp() {
        repos = new ConfigReposConfig();
    }

    @Test
    public void shouldReturnFalseThatHasMaterialWhenEmpty() {
        assertThat(repos.isEmpty(), is(true));
        assertThat(repos.hasMaterial(mock(MaterialConfig.class)), is(false));
    }

    @Test
    public void shouldReturnTrueThatHasMaterialWhenAddedConfigRepo() {
        repos.add(ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplugin", "id"));
        assertThat(repos.hasMaterial(git("http://git")), is(true));
    }

    @Test
    public void shouldFindConfigRepoWithSpecifiedId() {
        String id = "repo1";
        ConfigRepoConfig configRepo1 = ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplugin", id);
        repos.add(configRepo1);
        assertThat(repos.getConfigRepo(id), is(configRepo1));
    }

    @Test
    public void shouldFindReturnNullWhenConfigRepoWithSpecifiedIdIsNotPresent() {
        assertNull(repos.getConfigRepo("repo1"));
    }

    @Test
    public void shouldReturnTrueThatHasConfigRepoWhenAddedConfigRepo() {
        repos.add(ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplugin", "repo-id"));
        assertThat(repos.contains(ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplugin", "repo-id")), is(true));
    }

    @Test
    public void shouldReturnFalseThatHasConfigRepoWhenEmpty() {
        assertThat(repos.isEmpty(), is(true));
        assertThat(repos.contains(ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplugin", "id")), is(false));
    }

    @Test
    public void shouldErrorWhenDuplicateReposExist() {
        ConfigRepoConfig repo1 = ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplugin", "id");
        ConfigRepoConfig repo2 = ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myotherplugin", "other-id");
        repos.add(repo1);
        repos.add(repo2);
        // this is a limitation, we identify config repos by material fingerprint later
        // so there cannot be one repository parsed by 2 plugins.
        // This also does not seem like practical use case anyway
        repos.validate(null);
        assertThat(repos.errors().on("material"),
                is("You have defined multiple configuration repositories with the same repository."));
    }

    @Test
    public void shouldErrorWhenDuplicateIdsExist() {
        ConfigRepoConfig repo1 = ConfigRepoConfig.createConfigRepoConfig(git("http://git1"), "myplugin", "id");
        ConfigRepoConfig repo2 = ConfigRepoConfig.createConfigRepoConfig(git("http://git2"), "myotherplugin", "id");
        repos.add(repo1);
        repos.add(repo2);
        repos.validate(null);
        assertThat(repos.errors().on("id"),
                is("You have defined multiple configuration repositories with the same id."));
    }

    @Test
    public void shouldNotErrorWhenReposFingerprintDiffer() {
        ConfigRepoConfig repo1 = ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplugin", "id1");
        ConfigRepoConfig repo2 = ConfigRepoConfig.createConfigRepoConfig(git("https://git", "develop"), "myotherplugin", "id2");
        repos.add(repo1);
        repos.add(repo2);
        repos.validate(null);
        assertThat(repo1.errors().isEmpty(), is(true));
        assertThat(repo2.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldReturnTrueIfContainsConfigRepoWithTheSpecifiedId() {
        ConfigRepoConfig repo = ConfigRepoConfig.createConfigRepoConfig(git("http://git1"), "myplugin", "id");
        repos.add(repo);

        assertThat(repos.hasConfigRepo(repo.getId()), is(true));
    }

    @Test
    public void shouldReturnFalseIfDoesNotContainTheConfigRepoWithTheSpecifiedId() {
        assertThat(repos.hasConfigRepo("unknown"), is(false));
    }
}
