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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class GoConfigMaterialsTest {

    @Test
    public void shouldProvideSetOfSchedulableMaterials() {
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig("url", "svnDir", true);
        PipelineConfig pipeline1 = new PipelineConfig(new CaseInsensitiveString("pipeline1"), new MaterialConfigs(svnMaterialConfig));
        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1));
        assertThat(config.getAllUniqueMaterialsBelongingToAutoPipelines(), hasItem(svnMaterialConfig));
    }

    @Test
    public void shouldIncludeScmMaterialsFromManualPipelinesInSchedulableMaterials() {
        PipelineConfig pipeline1 = pipelineWithManyMaterials(true);

        pipeline1.add(new StageConfig(new CaseInsensitiveString("manual-stage"), new JobConfigs(), new Approval()));
        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1));
        assertThat(config.getAllUniqueMaterialsBelongingToAutoPipelines().size(), is(4));
    }

    @Test
    public void shouldNotIncludePackageMaterialsWithAutoUpdateFalse() {
        PipelineConfig pipeline1 = pipelineWithManyMaterials(false);
        pipeline1.addMaterialConfig(getPackageMaterialConfigWithAutoUpdateFalse());
        pipeline1.addMaterialConfig(getPackageMaterialConfigWithAutoUpdateTrue());

        pipeline1.add(new StageConfig(new CaseInsensitiveString("manual-stage"), new JobConfigs(), new Approval()));
        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1));
        assertThat(config.getAllUniqueMaterialsBelongingToAutoPipelines().size(), is(4));
    }

    @Test
    public void shouldNotIncludePluggableSCMMaterialsWithAutoUpdateFalse() {
        PipelineConfig pipeline1 = pipelineWithManyMaterials(false);
        PluggableSCMMaterialConfig autoUpdateMaterialConfig = new PluggableSCMMaterialConfig(null, SCMMother.create("scm-id-1"), null, null);
        pipeline1.addMaterialConfig(autoUpdateMaterialConfig);
        PluggableSCMMaterialConfig nonAutoUpdateMaterialConfig = new PluggableSCMMaterialConfig(null, SCMMother.create("scm-id-2"), null, null);
        nonAutoUpdateMaterialConfig.getSCMConfig().setAutoUpdate(false);
        pipeline1.addMaterialConfig(nonAutoUpdateMaterialConfig);

        pipeline1.add(new StageConfig(new CaseInsensitiveString("manual-stage"), new JobConfigs(), new Approval()));
        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1));
        Set<MaterialConfig> materialsBelongingToAutoPipelines = config.getAllUniqueMaterialsBelongingToAutoPipelines();
        assertThat(materialsBelongingToAutoPipelines.size(), is(4));
        assertThat(materialsBelongingToAutoPipelines, containsInAnyOrder(pipeline1.materialConfigs().get(1), pipeline1.materialConfigs().get(2), pipeline1.materialConfigs().get(3), pipeline1.materialConfigs().get(4)));
    }

    private PackageMaterialConfig getPackageMaterialConfigWithAutoUpdateFalse() {
        PackageDefinition packageDefinition = new PackageDefinition("packageWithAutoUpdateFalse", "DLF Package", new Configuration());
        packageDefinition.setRepository(PackageRepositoryMother.create("DLF"));
        packageDefinition.setAutoUpdate(false);
        return new PackageMaterialConfig(new CaseInsensitiveString("JamesBond"), "packageWithAutoUpdateFalse", packageDefinition);
    }

    private PackageMaterialConfig getPackageMaterialConfigWithAutoUpdateTrue() {
        PackageDefinition packageDefinition = new PackageDefinition("packageWithAutoUpdateFalse", "DTDC Package", new Configuration());
        packageDefinition.setRepository(PackageRepositoryMother.create("DTDC"));
        packageDefinition.setAutoUpdate(true);
        return new PackageMaterialConfig(new CaseInsensitiveString("Krish"), "packageWithAutoUpdateTrue", packageDefinition);
    }

    @Test
    public void uniqueMaterialForAutoPipelinesShouldNotReturnPackageMaterialsWithAutoUpdateFalse() throws Exception {
        PipelineConfig pipeline1 = pipelineWithManyMaterials(false);
        pipeline1.add(new StageConfig(new CaseInsensitiveString("manual-stage"), new JobConfigs(), new Approval()));
        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1));
        assertThat(config.getAllUniqueMaterialsBelongingToAutoPipelines().size(), is(3));

    }


    private PipelineConfig pipelineWithManyMaterials(boolean autoUpdate) {
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig();
        svnMaterialConfig.setAutoUpdate(autoUpdate);
        MaterialConfig gitMaterialConfig = MaterialConfigsMother.gitMaterialConfig("/foo/bar.git");
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
        P4MaterialConfig p4MaterialConfig = MaterialConfigsMother.p4MaterialConfig();
        return new PipelineConfig(new CaseInsensitiveString("pipeline1"), new MaterialConfigs(svnMaterialConfig, hgMaterialConfig, gitMaterialConfig, p4MaterialConfig));
    }

    @Test
    public void shouldIncludeDependencyMaterialsFromManualPipelinesInSchedulableMaterials() {
        DependencyMaterialConfig dependencyMaterialConfig = MaterialConfigsMother.dependencyMaterialConfig();
        PipelineConfig pipeline1 = new PipelineConfig(new CaseInsensitiveString("pipeline1"), new MaterialConfigs(dependencyMaterialConfig));
        pipeline1.add(new StageConfig(new CaseInsensitiveString("manual-stage"), new JobConfigs(), new Approval()));
        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1));
        Set<MaterialConfig> materialConfigs = config.getAllUniqueMaterialsBelongingToAutoPipelines();
        assertThat(materialConfigs.size(), is(1));
        assertThat(materialConfigs.contains(dependencyMaterialConfig), is(true));
    }

    @Test
    public void getStagesUsedAsMaterials() {
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig();

        StageConfig upStage = new StageConfig(new CaseInsensitiveString("stage1"), new JobConfigs());
        PipelineConfig up1 = new PipelineConfig(new CaseInsensitiveString("up1"), new MaterialConfigs(hg), upStage);
        PipelineConfig up2 = new PipelineConfig(new CaseInsensitiveString("up2"), new MaterialConfigs(hg), new StageConfig(new CaseInsensitiveString("stage2"), new JobConfigs()));

        DependencyMaterialConfig dependency1 = MaterialConfigsMother.dependencyMaterialConfig("up1", "stage1");
        DependencyMaterialConfig dependency2 = MaterialConfigsMother.dependencyMaterialConfig("up2", "stage2");

        PipelineConfig down1 = new PipelineConfig(new CaseInsensitiveString("down1"), new MaterialConfigs(dependency1, dependency2, hg));
        PipelineConfig down2 = new PipelineConfig(new CaseInsensitiveString("down2"), new MaterialConfigs(dependency1, dependency2, hg));

        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(up1, up2, down1, down2));
        Set<StageConfig> stages = config.getStagesUsedAsMaterials(up1);
        assertThat(stages.size(), is(1));
        assertThat(stages.contains(upStage), is(true));
    }

    @Test
    public void shouldOnlyHaveOneCopyOfAMaterialIfOnlyTheFolderIsDifferent() {
        SvnMaterialConfig svn = MaterialConfigsMother.svnMaterialConfig("url", "folder1", true);
        SvnMaterialConfig svnInDifferentFolder = MaterialConfigsMother.svnMaterialConfig("url", "folder2");
        PipelineConfig pipeline1 = new PipelineConfig(new CaseInsensitiveString("pipeline1"), new MaterialConfigs(svn));
        PipelineConfig pipeline2 = new PipelineConfig(new CaseInsensitiveString("pipeline2"), new MaterialConfigs(svnInDifferentFolder));

        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1, pipeline2));
        assertThat(config.getAllUniqueMaterialsBelongingToAutoPipelines().size(), is(1));
    }

    @Test
    public void shouldHaveBothMaterialsIfTheTypeIsDifferent() {
        SvnMaterialConfig svn = MaterialConfigsMother.svnMaterialConfig("url", "folder1", true);
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig("url", "folder2");
        PipelineConfig pipeline1 = new PipelineConfig(new CaseInsensitiveString("pipeline1"), new MaterialConfigs(svn));
        PipelineConfig pipeline2 = new PipelineConfig(new CaseInsensitiveString("pipeline2"), new MaterialConfigs(hg));

        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline1, pipeline2));
        assertThat(config.getAllUniqueMaterialsBelongingToAutoPipelines().size(), is(2));
    }
}
