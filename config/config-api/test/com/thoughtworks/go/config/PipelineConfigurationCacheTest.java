/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.Node;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class PipelineConfigurationCacheTest {

    private PipelineConfigurationCache configCache;
    private CruiseConfig cruiseConfig;

    @Before
    public void setUp() throws Exception {
        cruiseConfig = GoConfigMother.configWithPipelines("P1", "P2", "P3");
        configCache = PipelineConfigurationCache.getInstance();
        configCache.onConfigChange(cruiseConfig);
    }

    @Test
    public void shouldUpdateMapsWhenNewPipelineIsAddedOnConfigChange() {
        GitMaterialConfig git = new GitMaterialConfig("url");
        PipelineConfig p4 = new PipelineConfig(new CaseInsensitiveString("P4"), new MaterialConfigs(git));
        assertThat(configCache.getPipelineConfig("P4"), is(nullValue()));
        assertThat(configCache.getMatchingMaterialsFromConfig(git.getFingerprint()), is(Matchers.nullValue()));

        cruiseConfig.addPipeline("g2", p4);
        configCache.onConfigChange(cruiseConfig);
        assertThat(configCache.getPipelineConfig("P4"), is(p4));
        MaterialConfigs matchingMaterialsFromConfig = configCache.getMatchingMaterialsFromConfig(git.getFingerprint());
        assertThat(matchingMaterialsFromConfig.size(), is(1));
        assertThat(matchingMaterialsFromConfig.contains(git), is(true));
    }

    @Test
    public void shouldUpdateMapsWhenAnExistingPipelineIsUpdatedOnPipelineConfigChange() {
        GitMaterialConfig git = new GitMaterialConfig("url");
        MaterialConfig svn = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("P1")).materialConfigs().get(0);
        PipelineConfig p1 = new PipelineConfig(new CaseInsensitiveString("P1"), new MaterialConfigs(git), new StageConfig(), new StageConfig());

        configCache.onPipelineConfigChange(p1);
        assertThat(configCache.getPipelineConfig("P1"), is(p1));
        MaterialConfigs matchingGitMaterials = configCache.getMatchingMaterialsFromConfig(git.getFingerprint());
        assertThat(matchingGitMaterials.size(), is(1));
        assertThat(matchingGitMaterials.contains(git), is(true));
        MaterialConfigs matchingSvnMaterials = configCache.getMatchingMaterialsFromConfig(svn.getFingerprint());
        assertThat(matchingSvnMaterials.size(), is(2));
    }

    @Test
    public void shouldUpdateMapsWhenANewPipelineIsAddedOnPipelineConfigChange() {
        GitMaterialConfig git = new GitMaterialConfig("url");
        PipelineConfig p4 = new PipelineConfig(new CaseInsensitiveString("P4"), new MaterialConfigs(git), new StageConfig());
        assertThat(configCache.getPipelineConfig("P4"), is(nullValue()));
        assertThat(configCache.getMatchingMaterialsFromConfig(git.getFingerprint()), is(Matchers.nullValue()));

        configCache.onPipelineConfigChange(p4);

        assertThat(configCache.getPipelineConfig("P4"), is(p4));
        MaterialConfigs matchingMaterialsFromConfig = configCache.getMatchingMaterialsFromConfig(git.getFingerprint());
        assertThat(matchingMaterialsFromConfig.size(), is(1));
        assertThat(matchingMaterialsFromConfig.contains(git), is(true));
    }

    @Test
    public void shouldGetPipelineConfig() throws Exception {
        PipelineConfig expectedPipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("P1"));
        assertThat(configCache.getPipelineConfig("P1"), is(expectedPipelineConfig));
        assertThat(configCache.getPipelineConfig("p1"), is(expectedPipelineConfig));
    }

    @Test
    public void shouldReturnNullWhenNoMatchingPipelineIsFound() throws Exception {
        assertThat(configCache.getPipelineConfig("junk"), is(nullValue()));
    }

    @Test
    public void shouldGetMaterialConfigGivenAFingerprint() {
        MaterialConfig expected = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("P1")).materialConfigs().first();
        MaterialConfigs matchingMaterialsFromConfig = configCache.getMatchingMaterialsFromConfig(expected.getFingerprint());
        assertThat(matchingMaterialsFromConfig.size(), is(3));
        assertThat(matchingMaterialsFromConfig.contains(expected), is(true));
    }

    @Test
    public void shouldReturnNullIfMatchingMaterialConfigIsNotFound() {
        assertThat(configCache.getMatchingMaterialsFromConfig("does_not_exist"), is(nullValue()));
    }

    @Test
    public void shouldCachePipelinesDependencies(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2", "p3");
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage") ));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"),new CaseInsensitiveString("stage") ));
        configCache.onConfigChange(cruiseConfig);

        assertThat(configCache.getDependencyMaterialsFor(new CaseInsensitiveString("p1")).getDependencies().isEmpty(), is(true));
        assertThat(configCache.getDependencyMaterialsFor(new CaseInsensitiveString("p2")).getDependencies(), contains(new Node.DependencyNode(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage"))));
        assertThat(configCache.getDependencyMaterialsFor(new CaseInsensitiveString("p3")).getDependencies(), contains(new Node.DependencyNode(new CaseInsensitiveString("p2"),new CaseInsensitiveString("stage"))));
        assertThat(configCache.getDependencyMaterialsFor(new CaseInsensitiveString("junk")).getDependencies().isEmpty(), is(true));
    }

    @Test
    public void shouldUpdatePipelinesDependenciesCacheOnPipelineConfigChange(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2", "p3");
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage") ));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"),new CaseInsensitiveString("stage") ));
        configCache.onConfigChange(cruiseConfig);

        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage") ));
        configCache.onPipelineConfigChange(p3);

        assertThat(configCache.getDependencyMaterialsFor(new CaseInsensitiveString("p1")).getDependencies().isEmpty(), is(true));
        assertThat(configCache.getDependencyMaterialsFor(new CaseInsensitiveString("p2")).getDependencies(), contains(new Node.DependencyNode(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage") )));
        assertThat(configCache.getDependencyMaterialsFor(new CaseInsensitiveString("p3")).getDependencies(), contains(new Node.DependencyNode(new CaseInsensitiveString("p2"),new CaseInsensitiveString("stage") ), new Node.DependencyNode(new CaseInsensitiveString("p1"),new CaseInsensitiveString("stage") )));
    }

    @Test
    public void shouldGetDependencyMaterialsForAPipeline() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2", "p3");
        PipelineConfig p1 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p1"));
        PipelineConfig p2 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p2"));
        p2.addMaterialConfig(new DependencyMaterialConfig(p1.name(), p1.first().name()));
        PipelineConfig p3 = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("p3"));
        p3.addMaterialConfig(new DependencyMaterialConfig(p2.name(), p2.first().name()));
        configCache.onConfigChange(cruiseConfig);

        p3.addMaterialConfig(new DependencyMaterialConfig(p1.name(), p1.first().name()));
        configCache.onPipelineConfigChange(p3);
        assertThat(configCache.getDependencyMaterialsFor(p1.name()).getDependencies().isEmpty(), is(true));
        assertThat(configCache.getDependencyMaterialsFor(p2.name()).getDependencies(), contains(new Node.DependencyNode(p1.name(), p1.first().name())));
        assertThat(configCache.getDependencyMaterialsFor(p3.name()).getDependencies(), contains(new Node.DependencyNode(p2.name(), p2.first().name()), new Node.DependencyNode(p1.name(), p1.first().name())));
    }
}