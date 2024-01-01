/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StageLockCheckerTest {
    private PipelineLockService lockService;
    private PipelineIdentifier pipeline;
    private StageLockChecker checker;
    private HttpOperationResult result;

    @BeforeEach
    public void setUp() throws Exception {
        lockService = mock(PipelineLockService.class);
        pipeline = new PipelineIdentifier("mingle", 10, "2.0.10");
        checker = new StageLockChecker(pipeline, lockService);
        result = new HttpOperationResult();
    }

    @Test
    public void shouldAllowStagesInTheSamePipelineInstance() {
        when(lockService.canScheduleStageInPipeline(pipeline)).thenReturn(true);
        checker.check(result);
        assertThat(result.canContinue(), is(true));
    }

    @Test
    public void shouldNotAllowStagesIfThePipelineIsLocked() {
        when(lockService.canScheduleStageInPipeline(pipeline)).thenReturn(false);
        checker.check(result);
        assertThat(result.canContinue(), is(false));
        assertThat(result.message(), containsString("is locked"));
    }
}
