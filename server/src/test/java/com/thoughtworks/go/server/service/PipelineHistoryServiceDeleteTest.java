/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PipelineHistoryServiceDeleteTest {
    @Mock
    private PipelineDao pipelineDao;
    @Mock
    private SecurityService securityService;

    private PipelineHistoryService pipelineHistoryService;

    @BeforeEach
    public void setup() {
        pipelineHistoryService = new PipelineHistoryService(
            pipelineDao, null, securityService, null, null, null, null, null, null, null, null
        );
    }

    @Test
    public void shouldDeletePipelineInstance() {
        String pipelineName = "test-pipeline";
        int counter = 5;
        Username user = new Username(new CaseInsensitiveString("testuser"));

        Pipeline pipeline = new Pipeline(pipelineName, BuildCause.createManualForced());
        pipeline.setId(123);
        pipeline.setCounter(counter);

        when(securityService.hasOperatePermissionForPipeline(user.getUsername(), pipelineName)).thenReturn(true);
        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, counter)).thenReturn(pipeline);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineHistoryService.deletePipelineInstance(pipelineName, counter, user, result);

        verify(pipelineDao).deletePipeline(pipeline);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).contains("Deleted pipeline instance 'test-pipeline/5'");
    }

    @Test
    public void shouldNotDeletePipelineInstanceWhenUserLacksPermission() {
        String pipelineName = "test-pipeline";
        int counter = 5;
        Username user = new Username(new CaseInsensitiveString("testuser"));

        when(securityService.hasOperatePermissionForPipeline(user.getUsername(), pipelineName)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineHistoryService.deletePipelineInstance(pipelineName, counter, user, result);

        verify(pipelineDao, never()).deletePipeline(any());
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(403);
    }

    @Test
    public void shouldNotDeletePipelineInstanceWhenNotFound() {
        String pipelineName = "test-pipeline";
        int counter = 5;
        Username user = new Username(new CaseInsensitiveString("testuser"));

        when(securityService.hasOperatePermissionForPipeline(user.getUsername(), pipelineName)).thenReturn(true);
        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, counter)).thenReturn(null);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineHistoryService.deletePipelineInstance(pipelineName, counter, user, result);

        verify(pipelineDao, never()).deletePipeline(any());
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(404);
    }
}
