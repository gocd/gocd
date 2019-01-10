/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.presentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


/**
 * @understands: PipelineTemplateConfigViewModelTest
 */
public class PipelineTemplateConfigViewModelTest {

    @Test
    public void shouldSetConfigAttributesWithNoExistingPipeline() {
        PipelineTemplateConfigViewModel viewModel = new PipelineTemplateConfigViewModel();
        Map m = m("template", m(PipelineTemplateConfig.NAME, "template_name"), PipelineTemplateConfigViewModel.USE_EXISTING_PIPELINE, "0", PipelineTemplateConfigViewModel.PIPELINE_NAMES, "quux");

        viewModel.setConfigAttributes(m);

        PipelineTemplateConfig template = viewModel.templateConfig();

        assertThat(template.name(), is(new CaseInsensitiveString("template_name")));
        assertThat(template.size(), is(1));
        assertThat(template.get(0).name().toString(), is(StageConfig.DEFAULT_NAME));
        JobConfigs jobs = template.get(0).getJobs();
        assertThat(jobs.size(), is(1));
        assertThat(jobs.get(0).name().toString(), is(JobConfig.DEFAULT_NAME));
    }

    @Test
    public void shouldSetConfigAttributesBasedOnExistingPipeline() {
        StageConfig stage1 = StageConfigMother.custom("stage_foo", "foo1", "foo2");
        StageConfig stage2 = StageConfigMother.custom("stage_bar", "bar1", "bar2");
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage1, stage2);
        PipelineTemplateConfigViewModel viewModel = new PipelineTemplateConfigViewModel(new PipelineTemplateConfig(), "", Arrays.asList(PipelineConfigMother.pipelineConfig("foo_bar"), pipeline));
        Map m = m("template", m(PipelineTemplateConfig.NAME, "template_name"), PipelineTemplateConfigViewModel.USE_EXISTING_PIPELINE, "1", PipelineTemplateConfigViewModel.SELECTED_PIPELINE_NAME, pipeline.name().toString());

        viewModel.setConfigAttributes(m);

        assertThat(viewModel.useExistingPipeline(), is(true));
        assertThat(viewModel.selectedPipelineName(), is(pipeline.name().toString()));

        PipelineTemplateConfig template = viewModel.templateConfig();
        assertThat(template.name(), is(new CaseInsensitiveString("template_name")));
        assertThat(template.size(), is(2));
        assertThat(template.get(0), is(stage1));
        assertThat(template.get(1), is(stage2));

    }

    @Test
    public void shouldReturnTheSelectedPipelineNameAppropriately() {
        PipelineTemplateConfigViewModel viewModel = new PipelineTemplateConfigViewModel();
        assertThat(viewModel.selectedPipelineName(), is(""));
    }

    @Test
    public void shouldReturnStringNamesOfPipelines() {
        PipelineTemplateConfigViewModel viewModel = new PipelineTemplateConfigViewModel(new PipelineTemplateConfig(), "",
                Arrays.asList(PipelineConfigMother.pipelineConfig("pipeline_1"), PipelineConfigMother.pipelineConfig(
                        ".pipeline.2")));
        assertThat(viewModel.pipelineNames(), is(Arrays.asList("pipeline_1", ".pipeline.2")));

        viewModel = new PipelineTemplateConfigViewModel();
        assertThat(viewModel.pipelineNames(), is(new ArrayList<String>()));
    }
}
