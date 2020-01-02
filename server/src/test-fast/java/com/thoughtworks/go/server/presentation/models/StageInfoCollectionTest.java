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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class StageInfoCollectionTest {

    @Test
    public void shouldMatchForBothEmpty() throws Exception {
        assertThat(new StageConfigurationModels().match(new PipelineConfig()), is(true));
    }

    @Test
    public void shouldMatch() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("cruise", "ut", "ft");
        assertThat(new StageConfigurationModels(pipelineConfig).match(pipelineConfig), is(true));
    }

    @Test
    public void shouldEqual() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("cruise", "ut", "ft");
        StageConfigurationModels twoAutoStages = new StageConfigurationModels(pipelineConfig);
        assertThat(twoAutoStages.equals(new StageConfigurationModels(pipelineConfig)), is(true));
    }

    @Test
    public void shouldEqualForDifferentImplementations() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("cruise", "ut", "ft");
        StageConfigurationModels twoAutoStages = new StageConfigurationModels(pipelineConfig);

        StageConfigurationModels infoCollection = new StageConfigurationModels();
        infoCollection.add(new NullStageHistoryItem("ut", true));
        infoCollection.add(new NullStageHistoryItem("ft", true));
        assertThat(twoAutoStages.equals(infoCollection), is(true));

    }

}
