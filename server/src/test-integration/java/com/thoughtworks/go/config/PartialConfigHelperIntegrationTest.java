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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;

import static com.thoughtworks.go.config.remote.ConfigRepoConfig.createConfigRepoConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.gitMaterialConfig;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig;
import static com.thoughtworks.go.helper.PartialConfigMother.withPipeline;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PartialConfigHelperIntegrationTest {
    @Autowired
    PartialConfigHelper helper;

    @Test
    public void shouldAnswerWhetherPartialConfigsAreEquivalent() throws Exception {
        assertTrue(helper.isEquivalent(git("1"), git("1")));

        assertFalse(helper.isEquivalent(git("1"), git("2")));
        assertFalse(helper.isEquivalent(svn("1"), git("1")));

        // handles null
        assertFalse(helper.isEquivalent(null, git("1")));
        assertFalse(helper.isEquivalent(git("1"), null));
    }

    @Test
    public void isEquivalent_CollectionPartialConfig_returnsTrueWhenBothCollectionsAreEmpty() {
        assertTrue(helper.isEquivalent(null, Collections.emptyList()));
        assertTrue(helper.isEquivalent(Collections.emptyList(), null));
        assertTrue(helper.isEquivalent(Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    public void isEquivalent_CollectionPartialConfig_shouldNotFailWhenConfigPartialContainsStructuralErrors() {
        PartialConfig partial1 = git("1");
        ExecTask invalidExecTask = new ExecTask("docker", new Arguments(new Argument(null)));
        JobConfig invalidJob = new JobConfig("up42_job");
        invalidJob.addTask(invalidExecTask);
        partial1.getGroups().get(0).get(0).first().getJobs().add(invalidJob);

        PartialConfig partial2 = git("1");
        assertFalse(helper.isEquivalent(partial1, partial2));
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
