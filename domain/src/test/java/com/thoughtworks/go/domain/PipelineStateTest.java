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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class PipelineStateTest {
    @Test
    public void shouldBeEqualToAnotherPipelineStateIfAllAttributesMatch() {
        PipelineState pipelineState1 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        PipelineState pipelineState2 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        pipelineState1.lock(1);
        pipelineState2.lock(1);
        assertEquals(pipelineState1, pipelineState2);
    }

    @Test
    public void shouldBeEqualToAnotherPipelineStateIfBothDoNotHaveLockedBy() {
        PipelineState pipelineState1 = new PipelineState("p");
        PipelineState pipelineState2 = new PipelineState("p");
        pipelineState1.lock(1);
        pipelineState2.lock(1);
        assertEquals(pipelineState1, pipelineState2);
    }

    @Test
    public void shouldNotBeEqualToAnotherPipelineStateIfAllAttributesDoNotMatch() {
        PipelineState pipelineState1 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        PipelineState pipelineState2 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        pipelineState1.lock(1);
        assertNotEquals(pipelineState1, pipelineState2);
    }

    @Test
    public void shouldSetLockedByPipelineIdWhileLockingAPipeline() {
        PipelineState pipelineState = new PipelineState("p");
        pipelineState.lock(100);
        assertThat(pipelineState.isLocked(), is(true));
        assertThat(pipelineState.getLockedByPipelineId(), is(100L));
    }

    @Test
    public void shouldUnsetLockedByPipelineIdWhileUnlockingAPipeline() {
        PipelineState pipelineState = new PipelineState("p");
        pipelineState.lock(100);

        pipelineState.unlock();
        assertThat(pipelineState.isLocked(), is(false));
        assertThat(pipelineState.getLockedByPipelineId(), is(0L));
    }
}
