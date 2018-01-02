/*
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TemplateExpansionPreprocessorTest {
    private TemplateExpansionPreprocessor preprocessor;

    @Before
    public void setUp() throws Exception {
        preprocessor = new TemplateExpansionPreprocessor();
    }

    @Test
    public void shouldNotThrowAnExceptionWhenAPipelineHasAtLeastOneStage() throws Exception {
        PipelineConfig pipelineConfig = pipelineConfigWithGivenStages("foo");
        preprocessor.process(new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig)));
    }

    @Test
    public void shouldNotExpandWhenTemplateAssociatedWithPipelineDoesNotExist() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipelineConfig.templatize(new CaseInsensitiveString("does_not_exist"));
        preprocessor.process(new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig)));
        assertThat(pipelineConfig.hasTemplateApplied(), is(false));
    }

    @Test
    public void shouldValidatePipelineToCheckItDoesNotAllowBothTemplateAndStages() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipelineConfig.templatize(new CaseInsensitiveString("template"));
        pipelineConfig.addStageWithoutValidityAssertion(new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs()));
        preprocessor.process(new BasicCruiseConfig(new BasicPipelineConfigs(pipelineConfig)));
        assertThat(pipelineConfig.hasTemplateApplied(), is(false));
        assertThat(pipelineConfig.errors().on("stages"), is("Cannot add stages to pipeline 'p' which already references template 'template'"));
        assertThat(pipelineConfig.errors().on("template"), is("Cannot set template 'template' on pipeline 'p' because it already has stages defined"));
    }

    @Test
    public void shouldCloneStagesSoThatMutationDoesnotAffectTemplate() throws Exception {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipelineName"), new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig("http://google.com")));
        pipelineConfig.setTemplateName(new CaseInsensitiveString("templateName"));
        PipelineTemplateConfig template = new PipelineTemplateConfig();
        JobConfig jobConfigFromTemplate = new JobConfig("job-1");
        StageConfig stageConfigFromTemplate = new StageConfig(new CaseInsensitiveString("stage-1"), new JobConfigs(jobConfigFromTemplate));
        template.add(stageConfigFromTemplate);
        pipelineConfig.usingTemplate(template);

        StageConfig stageConfigFromPipeline = pipelineConfig.get(0);
        EnvironmentVariablesConfig variablesConfig = new EnvironmentVariablesConfig();
        variablesConfig.add("FOO", "BAR");
        stageConfigFromPipeline.setVariables(variablesConfig);
        JobConfig jobConfigFromPipeline = stageConfigFromPipeline.jobConfigByConfigName(new CaseInsensitiveString("job-1"));
        EnvironmentVariablesConfig jobVariablesConfigFromPipeline = new EnvironmentVariablesConfig();
        jobVariablesConfigFromPipeline.add("BAZ", "QUUX");
        jobConfigFromPipeline.setVariables(jobVariablesConfigFromPipeline);


        assertThat(stageConfigFromPipeline.getVariables().isEmpty(), is(false));
        assertThat(jobConfigFromPipeline.getVariables().isEmpty(), is(false));

        assertThat(stageConfigFromTemplate.getVariables().isEmpty(), is(true));
        assertThat(jobConfigFromTemplate.getVariables().isEmpty(), is(true));
    }

    private PipelineConfig pipelineConfigWithGivenStages(String... stageNames) {
        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig();
        pipelineConfig.clear();
        for (String stageName : stageNames) {
            pipelineConfig.add(StageConfigMother.stageConfig(stageName));
        }
        return pipelineConfig;
    }
}
