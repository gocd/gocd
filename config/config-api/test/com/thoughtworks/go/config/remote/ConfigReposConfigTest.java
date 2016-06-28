/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class ConfigReposConfigTest {
    private  ConfigReposConfig repos;
    @Before
    public void setUp()
    {
        repos = new ConfigReposConfig();
    }
    @Test
    public void shouldReturnFalseThatHasMaterialWhenEmpty(){
        assertThat(repos.isEmpty(),is(true));
        assertThat(repos.hasMaterial(mock(MaterialConfig.class)),is(false));
    }

    @Test
    public void shouldReturnTrueThatHasMaterialWhenAddedConfigRepo(){
        repos.add(new ConfigRepoConfig(new GitMaterialConfig("http://git"),"myplugin"));
        assertThat(repos.hasMaterial(new GitMaterialConfig("http://git")),is(true));
    }

    @Test
    public void shouldReturnTrueThatHasConfigRepoWhenAddedConfigRepo(){
        repos.add(new ConfigRepoConfig(new GitMaterialConfig("http://git"),"myplugin"));
        assertThat(repos.contains(new ConfigRepoConfig(new GitMaterialConfig("http://git"),"myplugin")),is(true));
    }
    @Test
    public void shouldReturnFalseThatHasConfigRepoWhenEmpty(){
        assertThat(repos.isEmpty(),is(true));
        assertThat(repos.contains(new ConfigRepoConfig(new GitMaterialConfig("http://git"),"myplugin")),is(false));
    }

    @Test
    public void shouldErrorWhenDuplicateReposExist(){
        ConfigRepoConfig repo1 = new ConfigRepoConfig(new GitMaterialConfig("http://git"), "myplugin");
        ConfigRepoConfig repo2 = new ConfigRepoConfig(new GitMaterialConfig("http://git"), "myotherplugin");
        repos.add(repo1);
        repos.add(repo2);
        // this is a limitation, we identify config repos by material fingerprint later
        // so there cannot be one repository parsed by 2 plugins.
        // This also does not seem like practical use case anyway
        repos.validate(null);
        assertThat(repo1.errors().on(ConfigRepoConfig.UNIQUE_REPO),
                is("You have defined multiple configuration repositories with the same repository - http://git"));
        assertThat(repo2.errors().on(ConfigRepoConfig.UNIQUE_REPO),
                is("You have defined multiple configuration repositories with the same repository - http://git"));

    }
    @Test
    public void shouldNotErrorWhenReposFingerprintDiffer(){
        ConfigRepoConfig repo1 = new ConfigRepoConfig(new GitMaterialConfig("http://git"), "myplugin");
        ConfigRepoConfig repo2 = new ConfigRepoConfig(new GitMaterialConfig("http://git","develop"), "myotherplugin");
        repos.add(repo1);
        repos.add(repo2);
        repos.validate(null);
        assertThat(repo1.errors().isEmpty(),is(true));
        assertThat(repo2.errors().isEmpty(),is(true));
    }
}
