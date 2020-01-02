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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineLockCheckerTest  {
    protected PipelineLockService pipelineLockService;
    protected OperationResult operationResult;

    @Test
    public void shouldUpdateOperationResultAfterUnlockSoThatTheErrorMessageOnUIGoesAway() {
        PipelineLockChecker pipelineLockChecker = new PipelineLockChecker("blahPipeline", pipelineLockService);
        HealthStateType healthStateType = HealthStateType.general(HealthStateScope.forPipeline("blahPipeline"));
        when(pipelineLockService.isLocked("blahPipeline")).thenReturn(true);
        pipelineLockChecker.check(operationResult);
        Mockito.verify(operationResult).conflict("Pipeline blahPipeline cannot be scheduled",
                "Pipeline blahPipeline is locked as another instance of this pipeline is running.",
                healthStateType);
        when(pipelineLockService.isLocked("blahPipeline")).thenReturn(false);
        pipelineLockChecker.check(operationResult);
        Mockito.verify(operationResult).success(healthStateType);
    }

    @Before
    public void setUp() throws Exception {
        pipelineLockService = mock(PipelineLockService.class);
        operationResult = mock(OperationResult.class);
    }
}
