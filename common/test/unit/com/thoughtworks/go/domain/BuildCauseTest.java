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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.buildcause.BuildCauseOutOfDateException;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import com.thoughtworks.go.helper.MaterialsMother;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.Test;

public class BuildCauseTest {

    @Test
    public void shouldReturnTrimBuildCauseIfRevisionIsLongerThan12() {
        ModificationSummaries summaries1 = new ModificationSummaries();
        summaries1.visit(
                new Modification(null, "comment1", null, null, "This could be a long Hg Revision Number"));
        assertThat(summaries1.getModification(0).getRevision(), is("This could b..."));
    }

    @Test
    public void differentBuildCausesShouldNotBeTheSame() {
        assertThat(BuildCause.createWithEmptyModifications().isSameAs(BuildCause.createManualForced()), is(false));
        assertThat(BuildCause.createManualForced().isSameAs(BuildCause.createWithEmptyModifications()), is(false));
    }

    @Test
    public void sameBuildCausesShouldBeSame() {
        MaterialRevisions first = new MaterialRevisions(
            new MaterialRevision(MaterialsMother.svnMaterial(), oneModifiedFile("revision1"))
        );
        MaterialRevisions second = new MaterialRevisions(
            new MaterialRevision(MaterialsMother.svnMaterial(), oneModifiedFile("revision1"))
        );
        assertThat(BuildCause.createWithModifications(first, "").isSameAs(BuildCause.createWithModifications(second, "")), is(true));
    }

    @Test
    public void shouldSetBuildTriggerOnUpdatingMaterialRevisions() {
        MaterialRevisions first = new MaterialRevisions(
            new MaterialRevision(MaterialsMother.svnMaterial(), oneModifiedFile("revision1"))
        );
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        buildCause.setMaterialRevisions(first);
        assertThat(buildCause.getBuildCauseMessage(), is("modified by lgao"));
    }

    @Test
    public void shouldNotChangeBuildTriggerForForcedBuildCauseWhenUpdatingMaterialRevisions() {
        MaterialRevisions first = new MaterialRevisions(
            new MaterialRevision(MaterialsMother.svnMaterial(), oneModifiedFile("revision1"))
        );
        BuildCause buildCause = BuildCause.createManualForced();
        buildCause.setMaterialRevisions(first);
        assertThat(buildCause.getBuildCauseMessage(), is("Forced by anonymous"));
    }

    @Test
    public void shouldNotThrowWhenMaterialAndConfigOriginRevisionDontMatch_WhenManualTrigger() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialConfig materialConfig = material.config();

        MaterialRevisions first = new MaterialRevisions(
                new MaterialRevision(material, oneModifiedFile("revision1"))
        );
        BuildCause buildCause = BuildCause.createManualForced();
        buildCause.setMaterialRevisions(first);

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build");
        pipelineConfig.materialConfigs().clear();
        pipelineConfig.materialConfigs().add(materialConfig);
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(materialConfig,"plug"),"revision2"));

        buildCause.assertPipelineConfigAndMaterialRevisionMatch(pipelineConfig);
    }

    @Test
    public void shouldThrowWhenMaterialAndConfigOriginRevisionDontMatch_WhenAutoTrigger() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialConfig materialConfig = material.config();

        MaterialRevisions first = new MaterialRevisions(
                new MaterialRevision(material, oneModifiedFile("revision1"))
        );
        BuildCause buildCause = BuildCause.createWithModifications(first,"");
        buildCause.setMaterialRevisions(first);

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build");
        pipelineConfig.materialConfigs().clear();
        pipelineConfig.materialConfigs().add(materialConfig);
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(materialConfig,"plug"),"revision2"));

        try {
            buildCause.assertPipelineConfigAndMaterialRevisionMatch(pipelineConfig);
        }
        catch (BuildCauseOutOfDateException ex)
        {
            //good
            return;
        }
        fail("should have thrown");
    }

    @Test
    public void shouldNotThrowWhenMaterialAndConfigOriginRevisionMatch_WhenAutoTrigger() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialConfig materialConfig = material.config();

        MaterialRevisions first = new MaterialRevisions(
                new MaterialRevision(material, oneModifiedFile("revision1"))
        );
        BuildCause buildCause = BuildCause.createWithModifications(first,"");
        buildCause.setMaterialRevisions(first);

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build");
        pipelineConfig.materialConfigs().clear();
        pipelineConfig.materialConfigs().add(materialConfig);
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(materialConfig,"plug"),"revision1"));

        buildCause.assertPipelineConfigAndMaterialRevisionMatch(pipelineConfig);
    }

}
