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

import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.server.service.result.OperationResult;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class StageActiveCheckerTest {

    private StageService service;
    private String pipelineName;
    private StageActiveChecker checker;
    private String stageName;
    private OperationResult result;

    @Before
    public void setUp() throws Exception {
        service = mock(StageService.class);
        pipelineName = "cruise";
        stageName = "dev";
        checker = new StageActiveChecker(pipelineName, stageName, service);
        result = mock(OperationResult.class);
    }

    @Test
    public void shouldNotThrowExceptionIfStageIsNotActive() {
        when(service.isStageActive(pipelineName, stageName)).thenReturn(false);
        checker.check(result);
        verify(result).success(any(HealthStateType.class));
    }

    @Test public void shouldBeAConflictOperationResultWhenStageAlreadyScheduled() throws Exception {
        when(service.isStageActive(pipelineName, stageName)).thenReturn(true);
        checker.check(result);
        verify(result).conflict(eq("Failed to trigger pipeline [cruise]"),eq("Stage [dev] in pipeline [cruise] is still in progress"),any(HealthStateType.class));
    }
}
