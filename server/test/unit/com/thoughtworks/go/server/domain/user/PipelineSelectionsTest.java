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

package com.thoughtworks.go.server.domain.user;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;

public class PipelineSelectionsTest {
    @Test
    public void shouldIncludePipelineWhenGroupIsIncluded_whenBlacklistIsEnabled() {
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pipeline1"));
        assertThat(pipelineSelections.includesGroup(createGroup("group1", pipelineConfig("pipelineX"))), is(true));
        assertThat(pipelineSelections.includesGroup(createGroup("group2", pipelineConfig("pipeline2"), pipelineConfig("pipeline1"))), is(false));
        assertThat(pipelineSelections.includesPipeline(pipelineConfig("pipeline1")), is(false));
        assertThat(pipelineSelections.includesPipeline(pipelineConfig("pipeline2")), is(true));
    }

    @Test
    public void shouldIncludePipelinesCaseInsensitively_whenBlacklistIsEnabled() {
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pipeline1"));
        assertThat(pipelineSelections.includesPipeline("pipeline1"), is(false));
        assertThat(pipelineSelections.includesPipeline("Pipeline1"), is(false));
    }

    @Test
    public void shouldReturnASelectionForASinglePipeline_whenBlacklistIsEnabled() {
        PipelineSelections selections = PipelineSelections.singleSelection("pipeline");
        assertThat(selections.includesPipeline("pipeline"), is(true));
        assertThat(selections.includesPipeline(pipelineConfig("pipeline")), is(true));

        assertThat(selections.includesPipeline("PIPELINE"), is(true));
        assertThat(selections.includesPipeline(pipelineConfig("PIPELINE")), is(true));

        assertThat(selections.includesPipeline("foo"), is(false));
        assertThat(selections.includesPipeline(pipelineConfig("foo")), is(false));
    }

    @Test
    public void shouldIncludePipelineWhenGroupIsIncluded_whenWhitelistIsEnabled() {
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pipeline1", "pipelineY"), null, null, false);
        assertThat(pipelineSelections.includesGroup(createGroup("group1", pipelineConfig("pipelineX"))), is(false));
        assertThat(pipelineSelections.includesGroup(createGroup("group2", pipelineConfig("pipeline2"), pipelineConfig("pipeline1"))), is(false));
        assertThat(pipelineSelections.includesGroup(createGroup("group3", pipelineConfig("pipeline2"), pipelineConfig("pipelineY"), pipelineConfig("pipeline1"))), is(false));
        assertThat(pipelineSelections.includesGroup(createGroup("group4", pipelineConfig("pipeline1"))), is(true));
        assertThat(pipelineSelections.includesGroup(createGroup("group5", pipelineConfig("pipeline1"), pipelineConfig("pipelineY"))), is(true));

        assertThat(pipelineSelections.includesPipeline(pipelineConfig("pipeline1")), is(true));
        assertThat(pipelineSelections.includesPipeline(pipelineConfig("pipeline2")), is(false));
    }
}
