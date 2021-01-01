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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineActiveCheckerTest {
    private StageService service;
    private PipelineActiveChecker checker;
    private PipelineIdentifier pipelineIdentifier;

    @Before
    public void setUp() throws Exception {
        service = mock(StageService.class);
        pipelineIdentifier = new PipelineIdentifier("cruise", 1, "label-1");
        checker = new PipelineActiveChecker(service, pipelineIdentifier);
    }

    @Test
    public void shouldFailIfPipelineIsActive() {
        when(service.isAnyStageActiveForPipeline(pipelineIdentifier)).thenReturn(true);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getDescription(),
                is("Pipeline[name='cruise', counter='1', label='label-1'] is still in progress"));
    }

    @Test
    public void shouldPassIfPipelineIsNotActive() {
        when(service.isAnyStageActiveForPipeline(pipelineIdentifier)).thenReturn(false);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess(), is(true));
    }
}
