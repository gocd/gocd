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
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.xml.*;
import com.thoughtworks.go.server.domain.xml.materials.MaterialXmlRepresenter;
import org.dom4j.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Date;

import static com.thoughtworks.go.helper.ModificationsMother.createSvnMaterialRevisions;
import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static com.thoughtworks.go.helper.PipelineHistoryMother.pipelineInstanceModel;
import static com.thoughtworks.go.server.dao.FeedModifier.Before;
import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock
    private Document document;

    @InjectMocks
    private FeedService feedService;
    private static final String BASE_URL = "https://go-server/go";

    @BeforeEach
    void setUp() {
        initMocks(this);

        when(xmlApiService.write(any(XmlRepresentable.class), eq(BASE_URL))).thenReturn(document);
    }

    @Test
    void shouldReturnPipelineXmlDocument() {
        PipelineInstanceModels models = PipelineInstanceModels.createPipelineInstanceModels();
        when(pipelineHistoryService.latestInstancesForConfiguredPipelines(username)).thenReturn(models);

        Document document = feedService.pipelinesXml(username, BASE_URL);

        assertThat(document).isNotNull();
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
            Integer pipelineCounter = null;

            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            Document document = feedService.stagesXml(username, pipelineName, pipelineCounter, BASE_URL);

            assertThat(document).isNotNull();
            verify(stageService).findStageFeedBy(pipelineName, pipelineCounter, Before, username);
            verify(xmlApiService).write(any(FeedEntriesRepresenter.class), eq(BASE_URL));
            verifyNoMoreInteractions(xmlApiService);
            verifyZeroInteractions(pipelineHistoryService);
            verifyZeroInteractions(jobInstanceService);
        }

        @Test
        void shouldReturnsStagesXmlDocumentBeforeId() {
            String pipelineName = "up42";
            int pipelineCounter = 100;
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            Document document = feedService.stagesXml(username, pipelineName, pipelineCounter, BASE_URL);

            assertThat(document).isNotNull();
            verify(stageService).findStageFeedBy(pipelineName, pipelineCounter, Before, username);
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

            Document document = feedService.pipelineXml(username, pipelineName, 100, BASE_URL);

            assertThat(document).isNotNull();
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

            Document document = feedService.stageXml(username, pipelineName, 100, stageName, 1, BASE_URL);

            assertThat(document).isNotNull();
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
        void shouldReturnJobXmlDocument() {
            String pipelineName = "up42";
            String stageName = "unit-tests";
            String jobName = "junit.xml";

            Document document = feedService.jobXml(username, pipelineName, 100, stageName, 1, jobName, BASE_URL);

            assertThat(document).isNotNull();
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
        void shouldReturnWaitingJobXmlDocument() {
            Document document = feedService.waitingJobPlansXml(BASE_URL);

            assertThat(document).isNotNull();
            verify(xmlApiService).write(any(JobPlanXmlRepresenter.class), eq(BASE_URL));
            verify(jobInstanceService).waitingJobPlans();
            verifyNoMoreInteractions(xmlApiService);
            verifyNoMoreInteractions(jobInstanceService);
            verifyZeroInteractions(stageService);
            verifyZeroInteractions(pipelineHistoryService);
        }
    }

    @Nested
    class MaterialXML {
        @Test
        void shouldThrowRecordNotFoundExceptionWhenMaterialRevisionWithFingerprintDoesNotExist() {
            String pipelineName = "up42";
            Integer pipelineCounter = 2;
            PipelineInstanceModel model = pipelineInstanceModel(pipelineName, pipelineCounter, new Date());
            model.setLatestRevisions(new MaterialRevisions());
            when(pipelineHistoryService.load(pipelineName, pipelineCounter, username)).thenReturn(model);

            assertThatCode(() -> feedService.materialXml(username, pipelineName, pipelineCounter, "foo", BASE_URL))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Material with pipeline unique fingerprint 'foo' was not found for pipeline run(up42/2)!");
        }

        @Test
        void shouldReturnMaterialXmlDocument() {
            MaterialRevisions revisions = createSvnMaterialRevisions(oneModifiedFile("rev"));
            String pipelineName = "up42";
            Integer pipelineCounter = 2;
            String pipelineUniqueFingerprint = revisions.getMaterialRevision(0).getMaterial().getPipelineUniqueFingerprint();

            PipelineInstanceModel model = pipelineInstanceModel(pipelineName, pipelineCounter, new Date());
            model.setLatestRevisions(revisions);
            when(pipelineHistoryService.load(pipelineName, pipelineCounter, username)).thenReturn(model);

            Document document = feedService.materialXml(username, pipelineName, pipelineCounter, pipelineUniqueFingerprint, BASE_URL);

            assertThat(document).isNotNull();
            verify(xmlApiService).write(any(MaterialXmlRepresenter.class), eq(BASE_URL));
            verifyNoMoreInteractions(xmlApiService);
            verifyZeroInteractions(jobInstanceService);
            verifyZeroInteractions(stageService);
        }
    }
}
