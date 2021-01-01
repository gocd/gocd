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
package com.thoughtworks.go.domain;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineStateTest {
    @Test
    void shouldBeEqualToAnotherPipelineStateIfAllAttributesMatch() {
        PipelineState pipelineState1 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        PipelineState pipelineState2 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        pipelineState1.lock(1);
        pipelineState2.lock(1);
        assertThat(pipelineState2).isEqualTo(pipelineState1);
    }

    @Test
    void shouldBeEqualToAnotherPipelineStateIfBothDoNotHaveLockedBy() {
        PipelineState pipelineState1 = new PipelineState("p");
        PipelineState pipelineState2 = new PipelineState("p");
        pipelineState1.lock(1);
        pipelineState2.lock(1);
        assertThat(pipelineState2).isEqualTo(pipelineState1);
    }

    @Test
    void shouldNotBeEqualToAnotherPipelineStateIfAllAttributesDoNotMatch() {
        PipelineState pipelineState1 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        PipelineState pipelineState2 = new PipelineState("p", new StageIdentifier("p", 1, "1", 1L, "s", "1"));
        pipelineState1.lock(1);
        assertThat(pipelineState2).isNotEqualTo(pipelineState1);
    }

    @Test
    void shouldSetLockedByPipelineIdWhileLockingAPipeline() {
        PipelineState pipelineState = new PipelineState("p");
        pipelineState.lock(100);
        assertThat(pipelineState.isLocked()).isTrue();
        assertThat(pipelineState.getLockedByPipelineId()).isEqualTo(100L);
    }

    @Test
    void shouldUnsetLockedByPipelineIdWhileUnlockingAPipeline() {
        PipelineState pipelineState = new PipelineState("p");
        pipelineState.lock(100);

        pipelineState.unlock();
        assertThat(pipelineState.isLocked()).isFalse();
        assertThat(pipelineState.getLockedByPipelineId()).isEqualTo(0L);
    }
}
