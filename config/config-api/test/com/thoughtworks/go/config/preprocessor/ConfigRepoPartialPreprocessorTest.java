/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.preprocessor;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PartialsProvider;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigRepoPartialPreprocessorTest {
    @Test
    public void shouldMergeLatestPartialsToConfig() {
        final PartialConfig partialConfig = PartialConfigMother.withPipeline("partial");
        ConfigRepoPartialPreprocessor preprocessor = new ConfigRepoPartialPreprocessor();
        preprocessor.init(new PartialsProvider() {
            @Override
            public List<PartialConfig> lastPartials() {
                return asList(partialConfig);
            }
        });
        PipelineConfig pipelineInMain = PipelineConfigMother.createPipelineConfig("main_pipeline", "stage", "job");
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineInMain));

        preprocessor.process(cruiseConfig);
        assertThat(cruiseConfig.getAllPipelineNames().contains(pipelineInMain.name()), is(true));
        assertThat(cruiseConfig.getAllPipelineNames().contains(partialConfig.getGroups().first().get(0).name()), is(true));
    }

    @Test
    public void shouldMergePartialsSetOnConfig() {
        final PartialConfig partialConfig = PartialConfigMother.withPipeline("partial");
        ConfigRepoPartialPreprocessor preprocessor = new ConfigRepoPartialPreprocessor();
        preprocessor.init(new PartialsProvider() {
            @Override
            public List<PartialConfig> lastPartials() {
                return new ArrayList<>();
            }
        });
        PipelineConfig pipelineInMain = PipelineConfigMother.createPipelineConfig("main_pipeline", "stage", "job");
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineInMain));
        cruiseConfig.setPartials(asList(partialConfig));
        preprocessor.process(cruiseConfig);
        assertThat(cruiseConfig.getAllPipelineNames().contains(pipelineInMain.name()), is(true));
        assertThat(cruiseConfig.getAllPipelineNames().contains(partialConfig.getGroups().first().get(0).name()), is(true));
    }
}