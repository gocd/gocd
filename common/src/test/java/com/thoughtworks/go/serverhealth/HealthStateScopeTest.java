/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthStateScopeTest {
    private static final SvnMaterial MATERIAL1 = MaterialsMother.svnMaterial("url1");
    private static final SvnMaterial MATERIAL2 = MaterialsMother.svnMaterial("url2");

    @Test
    public void shouldHaveAUniqueScopeForEachMaterial() {
        HealthStateScope scope1 = HealthStateScope.forMaterial(MATERIAL1);
        HealthStateScope scope2 = HealthStateScope.forMaterial(MATERIAL1);
        assertThat(scope1).isEqualTo(scope2);
    }

    @Test
    public void shouldHaveDifferentScopeForDifferentMaterials() {
        HealthStateScope scope1 = HealthStateScope.forMaterial(MATERIAL1);
        HealthStateScope scope2 = HealthStateScope.forMaterial(MATERIAL2);
        assertThat(scope1).isNotEqualTo(scope2);
    }

    @Test
    public void shouldHaveDifferentScopeWhenAutoUpdateHasChanged() {
        SvnMaterial mat = MaterialsMother.svnMaterial("url1");
        HealthStateScope scope1 = HealthStateScope.forMaterial(mat);
        mat.setAutoUpdate(false);
        HealthStateScope scope2 = HealthStateScope.forMaterial(mat);
        assertThat(scope1).isNotEqualTo(scope2);
    }

    @Test
    public void shouldHaveUniqueScopeForStages() {
        HealthStateScope scope1 = HealthStateScope.forStage("blahPipeline", "blahStage");
        HealthStateScope scope2 = HealthStateScope.forStage("blahPipeline", "blahStage");
        HealthStateScope scope25 = HealthStateScope.forStage("blahPipeline", "blahOtherStage");
        HealthStateScope scope3 = HealthStateScope.forStage("blahOtherPipeline", "blahOtherStage");

        assertThat(scope1).isEqualTo(scope2);
        assertThat(scope1).isNotEqualTo(scope25);
        assertThat(scope1).isNotEqualTo(scope3);
    }

    @Test
    public void shouldRemoveScopeWhenMaterialIsRemovedFromConfig() {
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");
        config.pipelineConfigByName(new CaseInsensitiveString("blahPipeline")).addMaterialConfig(hgMaterialConfig);
        assertThat(HealthStateScope.forMaterialConfig(hgMaterialConfig).isRemovedFromConfig(config)).isFalse();
        assertThat(HealthStateScope.forMaterial(MaterialsMother.svnMaterial("file:///bar")).isRemovedFromConfig(config)).isTrue();
    }

    @Test
    public void shouldNotRemoveScopeWhenMaterialBelongsToConfigRepoMaterial() {
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");
        config.getConfigRepos().add(ConfigRepoConfig.createConfigRepoConfig(hgMaterialConfig, "id1", "foo"));
        assertThat(HealthStateScope.forMaterialConfig(hgMaterialConfig).isRemovedFromConfig(config)).isFalse();
    }

    @Test
    public void shouldNotRemoveScopeWhenMaterialUpdateBelongsToConfigRepoMaterial() {
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");
        config.getConfigRepos().add(ConfigRepoConfig.createConfigRepoConfig(hgMaterialConfig, "id1", "foo"));
        assertThat(HealthStateScope.forMaterialConfigUpdate(hgMaterialConfig).isRemovedFromConfig(config)).isFalse();
    }

    @Test
    public void shouldRemoveScopeWhenStageIsRemovedFromConfig() {
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");
        assertThat(HealthStateScope.forPipeline("fooPipeline").isRemovedFromConfig(config)).isTrue();
        assertThat(HealthStateScope.forPipeline("blahPipeline").isRemovedFromConfig(config)).isFalse();

        assertThat(HealthStateScope.forStage("fooPipeline", "blahStage").isRemovedFromConfig(config)).isTrue();
        assertThat(HealthStateScope.forStage("blahPipeline", "blahStageRemoved").isRemovedFromConfig(config)).isTrue();
        assertThat(HealthStateScope.forStage("blahPipeline", "blahStage").isRemovedFromConfig(config)).isFalse();

    }

    @Test
    public void shouldRemoveScopeWhenJobIsRemovedFromConfig() {
        CruiseConfig config = GoConfigMother.pipelineHavingJob("blahPipeline", "blahStage", "blahJob", "fii", "baz");

        assertThat(HealthStateScope.forJob("fooPipeline", "blahStage", "barJob").isRemovedFromConfig(config)).isTrue();
        assertThat(HealthStateScope.forJob("blahPipeline", "blahStage", "blahJob").isRemovedFromConfig(config)).isFalse();
    }

    @Test
    public void shouldUnderstandPluginScope() {
        HealthStateScope scope = HealthStateScope.aboutPlugin("plugin.one");
        assertThat(scope.getScope()).isEqualTo("plugin.one");
        assertThat(scope.getType()).isEqualTo(HealthStateScope.ScopeType.PLUGIN);
    }
}
