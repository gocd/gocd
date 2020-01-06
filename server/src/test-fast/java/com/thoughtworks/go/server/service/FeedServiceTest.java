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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.xml.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FeedServiceTest {
    @Mock
    private Username username;
    @Mock
    private PipelineHistoryService pipelineHistoryService;
    @Mock
    private XmlApiService xmlApiService;
    @Mock
    private StageService stageService;
    @Mock
    private SecurityService securityService;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private JobInstanceService jobInstanceService;

    @InjectMocks
    private FeedService feedService;
    private static final String BASE_URL = "https://go-server/go";

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void shouldReturnPipelineXmlDocument() {
        PipelineInstanceModels models = PipelineInstanceModels.createPipelineInstanceModels();
        when(pipelineHistoryService.latestInstancesForConfiguredPipelines(username)).thenReturn(models);

        feedService.pipelinesXml(username, BASE_URL);

        verify(pipelineHistoryService).latestInstancesForConfiguredPipelines(username);

        verify(xmlApiService).write(any(PipelinesXmlRepresenter.class), eq(BASE_URL));
        verifyNoMoreInteractions(xmlApiService);
        verifyZeroInteractions(goConfigService);
        verifyZeroInteractions(securityService);
        verifyZeroInteractions(stageService);
    }

    @Nested
    class StagesXml {
        @Test
        void shouldThrowRecordNotFoundExceptionWhenPipelineDoesNotExist() {
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("up42"))).thenReturn(false);

            assertThatCode(() -> feedService.stagesXml(username, "up42", null, BASE_URL))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline with name 'up42' was not found!");
        }

        @Test
        void shouldThrowNotAuthorizedExceptionWhenUserDoesNotHavePermission() {
            String pipelineName = "up42";
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(false);

            assertThatCode(() -> feedService.stagesXml(username, pipelineName, null, BASE_URL))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("Not authorized to view pipeline");
        }

        @Test
        void shouldReturnsStagesXmlDocument() {
            String pipelineName = "up42";
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            feedService.stagesXml(username, pipelineName, null, BASE_URL);

            verify(stageService).feed(pipelineName, username);
            verify(xmlApiService).write(any(FeedEntriesRepresenter.class), eq(BASE_URL));
            verifyNoMoreInteractions(xmlApiService);
            verifyZeroInteractions(pipelineHistoryService);
            verifyZeroInteractions(jobInstanceService);
        }

        @Test
        void shouldReturnsStagesXmlDocumentBeforeId() {
            String pipelineName = "up42";
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            feedService.stagesXml(username, pipelineName, 100L, BASE_URL);

            verify(stageService).feedBefore(100L, pipelineName, username);
            verify(xmlApiService).write(any(FeedEntriesRepresenter.class), eq(BASE_URL));
            verifyNoMoreInteractions(xmlApiService);
            verifyZeroInteractions(pipelineHistoryService);
            verifyZeroInteractions(jobInstanceService);
        }
    }

    @Nested
    class PipelineXML {
        @Test
        void shouldReturnPipelineXmlDocument() {
            String pipelineName = "up42";

            feedService.pipelineXml(username, pipelineName, 100, BASE_URL);

            verify(xmlApiService).write(any(PipelineXmlRepresenter.class), eq(BASE_URL));
            verify(pipelineHistoryService).load(pipelineName, 100, username);
            verifyNoMoreInteractions(xmlApiService);
            verifyNoMoreInteractions(pipelineHistoryService);
            verifyZeroInteractions(stageService);
            verifyZeroInteractions(jobInstanceService);
        }
    }

    @Nested
    class StageXML {
        @Test
        void shouldReturnStageXmlDocument() {
            String pipelineName = "up42";
            String stageName = "unit-tests";
            feedService.stageXml(username, pipelineName, 100, stageName, 1, BASE_URL);

            verify(xmlApiService).write(any(StageXmlRepresenter.class), eq(BASE_URL));
            verify(stageService).findStageWithIdentifier(pipelineName, 100, stageName, "1", username);
            verifyNoMoreInteractions(xmlApiService);
            verifyNoMoreInteractions(stageService);
            verifyZeroInteractions(pipelineHistoryService);
            verifyZeroInteractions(jobInstanceService);
        }
    }

    @Nested
    class JobXML {
        @Test
        void shouldReturnStageXmlDocument() {
            String pipelineName = "up42";
            String stageName = "unit-tests";
            String jobName = "junit.xml";
            feedService.jobXml(username, pipelineName, 100, stageName, 1, jobName, BASE_URL);

            verify(xmlApiService).write(any(JobXmlRepresenter.class), eq(BASE_URL));
            verify(jobInstanceService).findJobInstance(pipelineName, stageName, jobName, 100, 1, username);
            verifyNoMoreInteractions(xmlApiService);
            verifyNoMoreInteractions(jobInstanceService);
            verifyZeroInteractions(stageService);
            verifyZeroInteractions(pipelineHistoryService);
        }
    }

    @Nested
    class WaitingJobsXML {
        @Test
        void shouldReturnStageXmlDocument() {
            feedService.waitingJobPlansXml(BASE_URL);

            verify(xmlApiService).write(any(JobPlanXmlRepresenter.class), eq(BASE_URL));
            verify(jobInstanceService).waitingJobPlans();
            verifyNoMoreInteractions(xmlApiService);
            verifyNoMoreInteractions(jobInstanceService);
            verifyZeroInteractions(stageService);
            verifyZeroInteractions(pipelineHistoryService);
        }
    }
}
