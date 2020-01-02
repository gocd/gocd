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

import java.sql.SQLException;

import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageMother;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class PipelineTest {

    @Test
    public void shouldReturnNextStageName() throws Exception {
        Pipeline pipeline = new Pipeline();
        Stage stage1 = StageMother.custom("stage1");
        stage1.setId(1);
        stage1.setOrderId(1);
        Stage stage2 = StageMother.custom("stage2");
        stage2.setId(2);
        stage2.setOrderId(2);
        Stage stage3 = StageMother.custom("stage3");
        stage3.setId(3);
        stage3.setOrderId(3);
        pipeline.getStages().add(stage2);
        pipeline.getStages().add(stage1);
        pipeline.getStages().add(stage3);
        assertThat(pipeline.nextStageName("stage1"), is("stage2"));
        shouldReturnNullIfNoneNext(pipeline);
        shouldReturnNullIfStageNotExist(pipeline);
    }

    private void shouldReturnNullIfNoneNext(Pipeline pipeline) {
        assertThat(pipeline.nextStageName("stage3"), is(nullValue()));
    }

    private void shouldReturnNullIfStageNotExist(Pipeline pipeline) {
        assertThat(pipeline.nextStageName("notExist"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForEmptyPipeline() throws Exception {
        Pipeline pipeline = new Pipeline();
        assertThat(pipeline.nextStageName("anyStage"), is(nullValue()));
    }

    @Test
    public void shouldUseOldBuildCauseMessageIfThereIsNoneForThisPipeline() throws SQLException {
        MaterialRevisions materialRevisions = ModificationsMother.multipleModifications();
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "");
        Pipeline pipeline = new Pipeline("Test", buildCause);
        assertThat(pipeline.getBuildCauseMessage(), not(Matchers.nullValue()));
    }

    @Test
    public void shouldHaveBuildCauseMessageUnknownIfBuildCauseIsNull() throws SQLException {
        Pipeline pipeline = new Pipeline("Test", null);
        assertThat(pipeline.getBuildCauseMessage(), is("Unknown"));
    }

    @Test
    public void shouldIncrementCounterAndUpdateLabel() throws Exception {
        Pipeline pipeline = new Pipeline();
        pipeline.setBuildCause(ModificationsMother.modifyNoFiles(PipelineConfigMother.pipelineConfig("mingle")));
        pipeline.updateCounter(1);
        assertThat(pipeline.getCounter(), is(2));
        assertThat(pipeline.getLabel(), is("2"));
    }

    @Test
    public void shouldUseOneAsFirstCounter() throws Exception {
        Pipeline pipeline = new Pipeline();
        pipeline.setBuildCause(ModificationsMother.modifyNoFiles(PipelineConfigMother.pipelineConfig("mingle")));
        pipeline.updateCounter(0);
        assertThat(pipeline.getCounter(), is(1));
        assertThat(pipeline.getLabel(), is("1"));
    }

    @Test
    public void shouldReturnIfAPipelineIsABisect() {
        Pipeline pipeline = new Pipeline();
        pipeline.setNaturalOrder(1.0);
        assertThat(pipeline.isBisect(), is(false));
        pipeline.setNaturalOrder(5.0);
        assertThat(pipeline.isBisect(), is(false));
        pipeline.setNaturalOrder(1.5);
        assertThat(pipeline.isBisect(), is(true));
        pipeline.setNaturalOrder(5.0625);
        assertThat(pipeline.isBisect(), is(true));
        pipeline.setNaturalOrder(5.000030517578125);
        assertThat(pipeline.isBisect(), is(true));

    }
}



