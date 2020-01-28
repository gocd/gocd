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
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * there is a risk that dao will misbehave when config is merged with remotes
 * Which is why this tests run on merged configuration
 * confirming that case that executes on sole file passes here as well.
 */
public class GoConfigDaoMergedTest extends GoConfigDaoTestBase {

    public GoConfigDaoMergedTest() {
        GoPartialConfig partials = mock(GoPartialConfig.class);
        List<PartialConfig> parts = new ArrayList<>();
        parts.add(new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("remote-pipe")))));
        parts.get(0).setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(git("http://config-repo.git"), "someplugin", "id"), "3213455"));

        when(partials.lastPartials()).thenReturn(parts);

        configHelper = new GoConfigFileHelper(partials);
        goConfigDao = configHelper.getGoConfigDao();
        cachedGoConfig = configHelper.getCachedGoConfig();
    }

    @Before
    public void setup() throws Exception {
        configHelper.initializeConfigFile();
    }
}
