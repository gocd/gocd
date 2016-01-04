/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class HealthStateScopeTest {
    private static final SvnMaterial MATERIAL1 = MaterialsMother.svnMaterial("url1");
    private static final SvnMaterial MATERIAL2 = MaterialsMother.svnMaterial("url2");

    @Test public void shouldHaveAUniqueScopeForEachMaterial() throws Exception {
        HealthStateScope scope1 = HealthStateScope.forMaterial(MATERIAL1);
        HealthStateScope scope2 = HealthStateScope.forMaterial(MATERIAL1);
        assertThat(scope1, is(scope2));
    }

    @Test public void shouldHaveDifferentScopeForDifferentMaterials() throws Exception {
        HealthStateScope scope1 = HealthStateScope.forMaterial(MATERIAL1);
        HealthStateScope scope2 = HealthStateScope.forMaterial(MATERIAL2);
        assertThat(scope1, not(scope2));
    }

    @Test public void shouldHaveUniqueScopeForStages() throws Exception {
        HealthStateScope scope1 = HealthStateScope.forStage("blahPipeline","blahStage");
        HealthStateScope scope2 = HealthStateScope.forStage("blahPipeline","blahStage");
        HealthStateScope scope25 = HealthStateScope.forStage("blahPipeline","blahOtherStage");
        HealthStateScope scope3 = HealthStateScope.forStage("blahOtherPipeline","blahOtherStage");

        assertThat(scope1, is(scope2));
        assertThat(scope1, not(scope25));
        assertThat(scope1, not(scope3));
    }

    @Test
    public void shouldRemoveScopeWhenMaterialIsRemovedFromConfig() throws Exception {
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");
        config.pipelineConfigByName(new CaseInsensitiveString("blahPipeline")).addMaterialConfig(hgMaterialConfig);
        assertThat(HealthStateScope.forMaterialConfig(hgMaterialConfig).isRemovedFromConfig(config),is(false));
        assertThat(HealthStateScope.forMaterial(MaterialsMother.svnMaterial("file:///bar")).isRemovedFromConfig(config),is(true));
    }

    @Test
    public void shouldRemoveScopeWhenStageIsRemovedFromConfig() throws Exception {
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");
        assertThat(HealthStateScope.forPipeline("fooPipeline").isRemovedFromConfig(config),is(true));
        assertThat(HealthStateScope.forPipeline("blahPipeline").isRemovedFromConfig(config),is(false));

        assertThat(HealthStateScope.forStage("fooPipeline","blahStage").isRemovedFromConfig(config),is(true));
        assertThat(HealthStateScope.forStage("blahPipeline","blahStageRemoved").isRemovedFromConfig(config),is(true));
        assertThat(HealthStateScope.forStage("blahPipeline","blahStage").isRemovedFromConfig(config),is(false));

    }

    @Test
    public void shouldRemoveScopeWhenJobIsRemovedFromConfig() throws Exception {
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");

        assertThat(HealthStateScope.forJob("fooPipeline","blahStage", "barJob").isRemovedFromConfig(config),is(true));
        assertThat(HealthStateScope.forJob("blahPipeline", "blahStage", "blahJob").isRemovedFromConfig(config),is(false));
    }

    @Test
    public void shouldUnderstandPluginScope() {
        HealthStateScope scope = HealthStateScope.forPlugin("plugin.one");
        assertThat(scope.getScope(), is("plugin.one"));
        assertThat(scope.getType(), is(HealthStateScope.ScopeType.PLUGIN));
    }
}
