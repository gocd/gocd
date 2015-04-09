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

import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PipelineConfigValidationTest {
    private CruiseConfig config;
    private PipelineConfig pipeline;
    private GoConfigMother goConfigMother;
    private ValidationContext validationContext;

    @Before
    public void setup() {
        config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");
        pipeline = config.pipelineConfigByName(new CaseInsensitiveString("go"));
        goConfigMother = new GoConfigMother();
    }

    @Test
    public void shouldEnsureStageNameUniqueness() {
        CruiseConfig cruiseConfig = new CruiseConfig();
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, "pipeline1", "stage", "build");
        JobConfig jobConfig = new JobConfig("my-build");
        jobConfig.addTask(new ExecTask("ls", "-la", "tmp"));
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig));
        pipelineConfig.addStageWithoutValidityAssertion(stageConfig);
        pipelineConfig.validate(validationContext);

        assertThat(stageConfig.errors().getAllOn("name"),
                is(Arrays.asList("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.")));

        assertThat(pipelineConfig.get(0).errors().getAllOn("name"),
                is(Arrays.asList("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.")));

        assertThat(cruiseConfig.validateAfterPreprocess().get(0).getAllOn("name"),
                is(Arrays.asList("You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.")));
    }

    @Test
    public void isValid_shouldEnsureLabelTemplateRefersToValidMaterials() {
        pipeline.setLabelTemplate("pipeline-${COUNT}-${myGit}");

        pipeline.validate(validationContext);
        ConfigErrors configErrors = pipeline.errors();
        List<String> errors = configErrors.getAllOn("labelTemplate");
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("You have defined a label template in pipeline go that refers to a material called myGit, but no material with this name is defined."));
    }

    @Test
    public void isValid_shouldEnsureLabelTemplateRefersToAMaterialOrCOUNT() {
        pipeline.setLabelTemplate("label-template-without-material-or-count");

        pipeline.validate(validationContext);
        ConfigErrors configErrors = pipeline.errors();
        List<String> errors = configErrors.getAllOn("labelTemplate");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), startsWith("Invalid label"));
    }

    @Test
    public void isValid_shouldEnsureLabelTemplateHasValidVariablePattern() {
        pipeline.setLabelTemplate("pipeline-${COUNT");

        pipeline.validate(validationContext);
        ConfigErrors configErrors = pipeline.errors();
        assertThat(configErrors.isEmpty(), is(false));
        List<String> errors = configErrors.getAllOn("labelTemplate");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), startsWith("Invalid label"));
    }

    @Test
    public void isValid_labelTemplateShouldAcceptLabelTemplateWithHashCharacter() {
        pipeline.setLabelTemplate("foo${COUNT}-tanker#");

        pipeline.validate(validationContext);
        ConfigErrors configErrors = pipeline.errors();
        assertThat(configErrors.isEmpty(), is(true));
        List<String> errors = configErrors.getAllOn("labelTemplate");
        assertThat(errors, is(nullValue()));
    }

    @Test
    public void isValid_shouldMatchMaterialNamesInACaseInsensitiveManner() {
        pipeline.setLabelTemplate("pipeline-${count}-${myGit}");

        ScmMaterialConfig gitMaterialConfig = MaterialConfigsMother.gitMaterialConfig("git://url");
        gitMaterialConfig.setName(new CaseInsensitiveString("mygit"));
        pipeline.addMaterialConfig(gitMaterialConfig);
        pipeline.validate(validationContext);
        assertThat(pipeline.errors().isEmpty(), is(true));
        List<String> errors = pipeline.errors().getAllOn("labelTemplate");
        assertThat(errors, is(nullValue()));
    }

    @Test
    public void isValid_shouldEnsureReturnsTrueWhenLabelTemplateRefersToValidMaterials() {
        pipeline.setLabelTemplate("pipeline-${COUNT}-${myGit}");
        GitMaterialConfig gitConfig = MaterialConfigsMother.gitMaterialConfig("git://url");
        gitConfig.setName(new CaseInsensitiveString("myGit"));
        pipeline.addMaterialConfig(gitConfig);
        pipeline.validate(validationContext);
        assertThat(pipeline.errors().isEmpty(), is(true));
        List<String> errors = pipeline.errors().getAllOn("labelTemplate");
        assertThat(errors, is(nullValue()));
    }

    @Test
    public void isValid_shouldAllowColonForLabelTemplate() throws Exception {
        pipeline.setLabelTemplate("pipeline-${COUNT}-${repo:name}");
        pipeline.addMaterialConfig(new PackageMaterialConfig(new CaseInsensitiveString("repo:name"), "package-id", PackageDefinitionMother.create("package-id")));
        pipeline.validate(validationContext);
        assertThat(pipeline.errors().getAllOn("labelTemplate"),is(nullValue()));
    }

    @Test
    public void validate_shouldEnsureThatTemplateFollowsTheNameType() {
        PipelineConfig config = new PipelineConfig(new CaseInsensitiveString("name"), new MaterialConfigs());
        config.setTemplateName(new CaseInsensitiveString(".Name"));
        config.validate(validationContext);
        assertThat(config.errors().isEmpty(), is(false));
        assertThat(config.errors().on(PipelineConfig.TEMPLATE_NAME), is("Invalid template name '.Name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void validate_shouldEnsureThatPipelineFollowsTheNameType() {
        PipelineConfig config = new PipelineConfig(new CaseInsensitiveString(".name"), new MaterialConfigs());
        config.validate(validationContext);
        assertThat(config.errors().isEmpty(), is(false));
        assertThat(config.errors().on(PipelineConfig.NAME), is("Invalid pipeline name '.name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldBeValidIfTheReferencedPipelineExists() throws Exception {
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));
        pipeline.validate(validationContext);
        assertThat(pipeline.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowMultipleDependenciesForDifferentPipelines() throws Exception {
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline3"), new CaseInsensitiveString("stage")));
        pipeline.validate(validationContext);
        assertThat(pipeline.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowDependenciesFromMultiplePipelinesToTheSamePipeline() throws Exception {
        pipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));

        PipelineConfig pipeline3 = config.pipelineConfigByName(new CaseInsensitiveString("pipeline3"));
        pipeline3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage")));

        pipeline.validate(validationContext);
        assertThat(pipeline.errors().isEmpty(), is(true));
        pipeline3.validate(validationContext);
        assertThat(pipeline3.errors().isEmpty(), is(true));

    }

    @Test
    public void shouldNotAllowAnEmptyView() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        P4MaterialConfig materialConfig = new P4MaterialConfig("localhost:1666", "");

        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.addMaterialConfig(materialConfig);
        materialConfig.validate(validationContext);
        assertThat(materialConfig.errors().on("view"), is("P4 view cannot be empty."));
    }
}
