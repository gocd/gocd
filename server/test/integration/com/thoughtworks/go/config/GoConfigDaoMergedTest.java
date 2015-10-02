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
package com.thoughtworks.go.config;


import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * there is a risk that dao will misbehave when config is merged with remotes
 * Which is why this tests run on merged configuration
 * confirming that case that executes on sole file passes here as well.
 */
public class GoConfigDaoMergedTest extends GoConfigDaoTestBase {

    public  GoConfigDaoMergedTest()
    {
        GoPartialConfig partials = mock(GoPartialConfig.class);
        List<PartialConfig> parts = new ArrayList<>();
        parts.add(new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("remote-pipe")))));
        parts.get(0).setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(new GitMaterialConfig("http://config-repo.git"),"someplugin"),"3213455"));

        when(partials.lastPartials()).thenReturn(parts);

        configHelper = new GoConfigFileHelper(partials);
        goConfigDao = configHelper.getGoConfigDao();
        cachedGoConfig = configHelper.getCachedGoConfig();
    }

    @Before
    public void setup() throws Exception {
        configHelper.initializeConfigFile();
        logger = LogFixture.startListening();
    }

    @After
    public void teardown() throws Exception {
        logger.stopListening();
    }

    @Test
    public void shouldUpgradeOldXmlWhenRequestedTo() throws Exception {
        cachedGoConfig.save(ConfigFileFixture.VERSION_5, true);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.getAllPipelineConfigs().size(), is(2));
        assertNotNull(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("framework")));
    }

    @Test
    public void shouldFailWhenTryingToAddPipelineDefinedRemotely() throws Exception {
        PipelineConfig dupPipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("remote-pipe", "ut",
                "www.spring.com");
        try {
            goConfigDao.addPipeline(dupPipelineConfig, DEFAULT_GROUP);
        }
        catch (RuntimeException ex)
        {
            assertThat(ex.getMessage(),is("Pipeline called 'remote-pipe' is already defined in configuration repository http://config-repo.git at 3213455"));
            return;
        }
        fail("Should have thrown");
    }
}
