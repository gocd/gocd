/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.PipelineIdentifier;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;


public class EmptyPipelineInstanceModelTest {
    private EmptyPipelineInstanceModel instanceModel;

    @Before
    public void setUp() {
        instanceModel = PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createNeverRun(), new StageInstanceModels());
    }

    @Test
    public void shouldAdvertiseAsUnrealPipeline() {
        assertThat(instanceModel.hasHistoricalData(), is(false));
    }

    @Test
    public void shouldReturnUnknownModificationAsCurrent() {
        assertThat(instanceModel.getCurrentRevision("foo"), is(PipelineInstanceModel.UNKNOWN_REVISION));
    }

    @Test
    public void shouldBeCapableOfGeneratingPipelineIdentifier() {
        assertThat(instanceModel.getPipelineIdentifier(), is(new PipelineIdentifier("pipeline", 0, "unknown")));
    }

    @Test
    public void shouldHaveNegetivePipelineId() {
        assertThat(instanceModel.getId(), is(-1l));
    }
}
