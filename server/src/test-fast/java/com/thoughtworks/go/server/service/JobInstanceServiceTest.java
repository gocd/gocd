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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.JobResultMessage;
import com.thoughtworks.go.server.messaging.JobResultTopic;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.JobInstancesModel;
import com.thoughtworks.go.server.ui.SortOrder;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.helper.JobInstanceMother.completed;
import static com.thoughtworks.go.helper.JobInstanceMother.scheduled;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class JobInstanceServiceTest {
    @Mock
    private JobInstanceDao jobInstanceDao;
    @Mock
    private JobResultTopic topic;
    @Mock
    private StageDao stageDao;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private CruiseConfig cruiseConfig;
    @Mock
    private SecurityService securityService;
    @Mock
    private ServerHealthService serverHealthService;

    private JobInstance job;
    private TestTransactionSynchronizationManager transactionSynchronizationManager;
    private TransactionTemplate transactionTemplate;
    private JobStatusCache jobStatusCache;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        job = JobInstanceMother.building("dev");
        transactionSynchronizationManager = new TestTransactionSynchronizationManager();
        transactionTemplate = new TestTransactionTemplate(transactionSynchronizationManager);
        jobStatusCache = new JobStatusCache(stageDao);
    }

    @AfterEach
    public void after() {
        Mockito.verifyNoMoreInteractions(jobInstanceDao, stageDao);
    }

    @Test
    public void shouldNotifyListenerWhenJobStatusChanged() throws Exception {
        final JobStatusListener listener = Mockito.mock(JobStatusListener.class);

        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
            null, serverHealthService, listener);
        jobService.updateStateAndResult(job);

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(listener).jobStatusChanged(job);
    }

    @Test
    public void shouldNotifyAllListenersWhenUpdateJobStatus() throws Exception {
        final JobStatusListener listener1 = Mockito.mock(JobStatusListener.class, "listener1");
        final JobStatusListener listener2 = Mockito.mock(JobStatusListener.class, "listener2");

        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
            null, null, goConfigService, null, serverHealthService, listener1, listener2);
        jobService.updateStateAndResult(job);

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(listener1).jobStatusChanged(job);
        verify(listener2).jobStatusChanged(job);
    }

    @Test
    public void shouldNotifyListenerWhenUpdateAssignedInfo() throws Exception {
        final JobStatusListener listener1 = Mockito.mock(JobStatusListener.class, "listener1");
        final JobStatusListener listener2 = Mockito.mock(JobStatusListener.class, "listener2");

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
            null, null, goConfigService, null, serverHealthService, listener1, listener2);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.updateAssignedInfo(job);
            }
        });
        verify(listener1).jobStatusChanged(job);
        verify(listener2).jobStatusChanged(job);
        verify(jobInstanceDao).updateAssignedInfo(job);
    }

    @Test
    public void shouldNotifyAllListenersWhenSaveJob() throws Exception {
        final JobStatusListener listener1 = Mockito.mock(JobStatusListener.class, "listener1");
        final JobStatusListener listener2 = Mockito.mock(JobStatusListener.class, "listener2");
        final Pipeline pipeline = new NullPipeline();
        final Stage stage = new Stage();
        stage.setId(1);

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
            null, null, goConfigService, null, serverHealthService, listener1, listener2);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.save(new StageIdentifier(pipeline.getName(), null, pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter())), stage.getId(), job);
            }
        });

        verify(jobInstanceDao).save(1l, job);
        verify(listener1).jobStatusChanged(job);
        verify(listener2).jobStatusChanged(job);
    }

    @Test
    public void shouldIgnoreErrorsWhenNotifyingListenersDuringSave() throws Exception {
        final JobStatusListener failingListener = Mockito.mock(JobStatusListener.class, "listener1");
        final JobStatusListener passingListener = Mockito.mock(JobStatusListener.class, "listener2");
        doThrow(new RuntimeException("Should not be rethrown by save")).when(failingListener).jobStatusChanged(job);
        final Pipeline pipeline = new NullPipeline();
        final Stage stage = new Stage();
        stage.setId(1);

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
            null, null, goConfigService, null, serverHealthService, failingListener, passingListener);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.save(new StageIdentifier(pipeline.getName(), null, pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter())), stage.getId(), job);
            }
        });

        verify(jobInstanceDao).save(1l, job);
        verify(passingListener).jobStatusChanged(job);
    }

    @Test
    public void shouldNotifyListenersWhenAssignedJobIsCancelled() {
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
            null, null, goConfigService, null, serverHealthService);
        job.setAgentUuid("dummy agent");

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.cancelJob(job);
            }
        });

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(topic).post(new JobResultMessage(job.getIdentifier(), JobResult.Cancelled, job.getAgentUuid()));
    }

    @Test
    public void shouldNotNotifyListenersWhenScheduledJobIsCancelled() {
        final JobInstance scheduledJob = scheduled("dev");

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null,
            null, goConfigService, null, serverHealthService);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.cancelJob(scheduledJob);
            }
        });

        jobService.cancelJob(scheduledJob);
        verify(jobInstanceDao).updateStateAndResult(scheduledJob);
        verify(topic, never()).post(any(JobResultMessage.class));
    }

    @Test
    public void shouldNotNotifyListenersWhenACompletedJobIsCancelled() {
        final JobInstance completedJob = completed("dev");

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null,
            null, goConfigService, null, serverHealthService);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.cancelJob(completedJob);
            }
        });

        jobService.cancelJob(completedJob);
        verify(jobInstanceDao, never()).updateStateAndResult(completedJob);
        verify(topic, never()).post(any(JobResultMessage.class));
    }

    @Test
    public void shouldNotNotifyListenersWhenAssignedJobCancellationTransactionRollsback() {
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
            null, null, goConfigService, null, serverHealthService);
        job.setAgentUuid("dummy agent");

        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    jobService.cancelJob(job);
                    throw new RuntimeException("to rollback txn");
                }
            });
        } catch (RuntimeException e) {
            //ignore
        }

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(topic, never()).post(any(JobResultMessage.class));
    }

    @Test
    public void shouldNotNotifyListenersWhenScheduledJobIsFailed() {
        final JobInstance scheduledJob = scheduled("dev");
        scheduledJob.setId(10);
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null,
            null, goConfigService, null, serverHealthService);

        when(jobInstanceDao.buildByIdWithTransitions(scheduledJob.getId())).thenReturn(scheduledJob);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                JobInstance jobInstance = jobService.buildByIdWithTransitions(scheduledJob.getId());
                jobService.failJob(jobInstance);
            }
        });

        verify(jobInstanceDao).buildByIdWithTransitions(scheduledJob.getId());
        verify(jobInstanceDao).updateStateAndResult(scheduledJob);
        assertThat(scheduledJob.isFailed(), is(true));
    }

    @Test
    public void shouldDelegateToDAO_getJobHistoryCount() {
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache,
            transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, null, serverHealthService);

        jobService.getJobHistoryCount("pipeline", "stage", "job");

        verify(jobInstanceDao).getJobHistoryCount("pipeline", "stage", "job");
    }

    @Test
    public void shouldDelegateToDAO_findJobHistoryPage() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache,
            transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, serverHealthService);

        Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
        jobService.findJobHistoryPage("pipeline", "stage", "job", pagination, "looser", new HttpOperationResult());

        verify(jobInstanceDao).findJobHistoryPage("pipeline", "stage", "job", pagination.getPageSize(), pagination.getOffset());
    }

    @Test
    public void shouldPopulateErrorWhenPipelineNotFound_findJobHistoryPage() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(false);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache,
            transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, serverHealthService);

        Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
        HttpOperationResult result = new HttpOperationResult();
        JobInstances jobHistoryPage = jobService.findJobHistoryPage("pipeline", "stage", "job", pagination, "looser", result);

        assertThat(jobHistoryPage, is(nullValue()));
        assertThat(result.httpCode(), is(404));
    }

    @Test
    public void shouldPopulateErrorWhenUnauthorized_findJobHistoryPage() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(false);

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache,
            transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, serverHealthService);

        Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
        HttpOperationResult result = new HttpOperationResult();
        JobInstances jobHistoryPage = jobService.findJobHistoryPage("pipeline", "stage", "job", pagination, "looser", result);

        assertThat(jobHistoryPage, is(nullValue()));
        assertThat(result.canContinue(), is(false));
    }

    @Test
    public void shouldLoadOriginalJobPlan() {
        JobResolverService resolver = mock(JobResolverService.class);
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
            resolver,
            null, goConfigService, null, serverHealthService);
        DefaultJobPlan expectedPlan = new DefaultJobPlan(new Resources(), new ArrayList<>(), 7, new JobIdentifier(), null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        when(jobInstanceDao.loadPlan(7l)).thenReturn(expectedPlan);
        JobIdentifier givenId = new JobIdentifier("pipeline-name", 9, "label-9", "stage-name", "2", "job-name", 10l);
        when(resolver.actualJobIdentifier(givenId)).thenReturn(new JobIdentifier("pipeline-name", 8, "label-8", "stage-name", "1", "job-name", 7l));
        assertThat(jobService.loadOriginalJobPlan(givenId), sameInstance(expectedPlan));
        verify(jobInstanceDao).loadPlan(7l);
    }

    @Test
    public void shouldRegisterANewListener() throws SQLException {
        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
            null, serverHealthService);
        JobStatusListener listener = mock(JobStatusListener.class);
        jobService.registerJobStateChangeListener(listener);
        jobService.updateStateAndResult(job);

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(listener).jobStatusChanged(job);
    }

    @Test
    public void shouldGetCompletedJobsOnAgentOnTheGivenPage() {
        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
            null, serverHealthService);
        ArrayList<JobInstance> expected = new ArrayList<>();
        when(jobInstanceDao.totalCompletedJobsOnAgent("uuid")).thenReturn(500);
        when(jobInstanceDao.completedJobsOnAgent("uuid", JobInstanceService.JobHistoryColumns.pipeline, SortOrder.ASC, 50, 50)).thenReturn(expected);

        JobInstancesModel actualModel = jobService.completedJobsOnAgent("uuid", JobInstanceService.JobHistoryColumns.pipeline, SortOrder.ASC, 2, 50);

        assertThat(actualModel, is(new JobInstancesModel(new JobInstances(expected), Pagination.pageByNumber(2, 500, 50))));
        verify(jobInstanceDao).totalCompletedJobsOnAgent("uuid");
        verify(jobInstanceDao).completedJobsOnAgent("uuid", JobInstanceService.JobHistoryColumns.pipeline, SortOrder.ASC, 50, 50);
    }

    @Test
    public void shouldRemoveJobRelatedServerHealthMessagesOnConfigChange() {
        ServerHealthService serverHealthService = new ServerHealthService();
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(HealthStateScope.forJob("p1", "s1", "j1"))));
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(HealthStateScope.forJob("p2", "s2", "j2"))));
        assertThat(serverHealthService.logs().errorCount(), is(2));
        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
            null, serverHealthService);
        jobService.onConfigChange(new BasicCruiseConfig());
        assertThat(serverHealthService.logs().errorCount(), is(0));
    }

    @Test
    public void shouldRemoveJobRelatedServerHealthMessagesOnPipelineConfigChange() {
        ServerHealthService serverHealthService = new ServerHealthService();
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(HealthStateScope.forJob("p1", "s1", "j1"))));
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(HealthStateScope.forJob("p2", "s2", "j2"))));
        assertThat(serverHealthService.logs().errorCount(), is(2));
        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
            null, serverHealthService);
        JobInstanceService.PipelineConfigChangedListener pipelineConfigChangedListener = jobService.new PipelineConfigChangedListener();
        pipelineConfigChangedListener.onEntityConfigChange(PipelineConfigMother.pipelineConfig("p1", "s_new", new MaterialConfigs(), "j1"));
        assertThat(serverHealthService.logs().errorCount(), is(1));
        assertThat(serverHealthService.logs().get(0).getType().getScope().getScope(), is("p2/s2/j2"));
    }

    @Test
    public void shouldGetRunningJobsFromJobInstanceDao() {
        List<JobInstance> expectedRunningJobs = Arrays.asList(job);
        when(jobInstanceDao.getRunningJobs()).thenReturn(expectedRunningJobs);

        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
            null, serverHealthService);

        List<JobInstance> jobInstances = jobService.allRunningJobs();

        assertThat(jobInstances, is(expectedRunningJobs));
        verify(jobInstanceDao).getRunningJobs();
    }

    @Test
    void shouldThrowExceptionIfPipelineDoesNotExistForJobInstance() {
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(false);
        JobInstanceService jobInstanceService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, null, serverHealthService);

        assertThatCode(() -> jobInstanceService.findJobInstance("pipeline", "stage", "job", 1, 1, new Username("admin")))
            .isInstanceOf(RecordNotFoundException.class)
            .hasMessage("Pipeline with name 'pipeline' was not found!");
    }

    @Test
    void shouldThrowExceptionIfTheUserDoesNotHaveAccessToViewPipelineForJobInstance() {
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("user"), "pipeline")).thenReturn(false);

        JobInstanceService jobInstanceService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, serverHealthService);

        assertThatCode(() -> jobInstanceService.findJobInstance("pipeline", "stage", "job", 1, 1, new Username("user")))
            .isInstanceOf(NotAuthorizedException.class)
            .hasMessage("Not authorized to view pipeline");
    }

    @Nested
    class JobHistoryViaCursor {
        private JobInstanceService jobService;
        ;
        private Username username = Username.valueOf("user");
        String pipelineName = "pipeline";
        String stageName = "stage";
        String jobConfigName = "job";

        @BeforeEach
        void setUp() {
            jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, serverHealthService);
        }

        @Test
        void shouldFetchLatestRecords() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            jobService.getJobHistoryViaCursor(username, pipelineName, stageName, jobConfigName, 0, 0, 10);

            verify(jobInstanceDao).findDetailedJobHistoryViaCursor(pipelineName, stageName, jobConfigName, FeedModifier.Latest, 0, 10);
        }

        @Test
        void shouldFetchRecordsAfterTheSpecifiedCursor() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            jobService.getJobHistoryViaCursor(username, pipelineName, stageName, jobConfigName, 2, 0, 10);

            verify(jobInstanceDao).findDetailedJobHistoryViaCursor(pipelineName, stageName, jobConfigName, FeedModifier.After, 2, 10);
        }

        @Test
        void shouldFetchRecordsBeforeTheSpecifiedCursor() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            jobService.getJobHistoryViaCursor(username, pipelineName, stageName, jobConfigName, 0, 2, 10);

            verify(jobInstanceDao).findDetailedJobHistoryViaCursor(pipelineName, stageName, jobConfigName, FeedModifier.Before, 2, 10);
        }

        @Test
        void shouldThrowErrorIfPipelineDoesNotExist() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

            assertThatCode(() -> jobService.getJobHistoryViaCursor(username, pipelineName, stageName, jobConfigName, 0, 0, 10))
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("Pipeline with name 'pipeline' was not found!");
        }

        @Test
        void shouldThrowErrorIfUserDoesNotHaveAccessRights() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);

            assertThatCode(() -> jobService.getJobHistoryViaCursor(username, pipelineName, stageName, jobConfigName, 0, 0, 10))
                    .isInstanceOf(NotAuthorizedException.class)
                    .hasMessage("User 'user' does not have permission to view pipeline with name 'pipeline'");
        }

        @Test
        void shouldThrowErrorIfCursorIsANegativeInteger() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            assertThatCode(() -> jobService.getJobHistoryViaCursor(username, pipelineName, stageName, jobConfigName, -10, 0, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The query parameter 'after', if specified, must be a positive integer.");

            assertThatCode(() -> jobService.getJobHistoryViaCursor(username, pipelineName, stageName, jobConfigName, 0, -10, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The query parameter 'before', if specified, must be a positive integer.");
        }
    }

    @Nested
    class LatestAndOldestStageInstanceId {
        private JobInstanceService jobService;
        private Username username = Username.valueOf("user");
        String pipelineName = "pipeline";
        String stageName = "stage";
        String jobConfigName = "job";

        @BeforeEach
        void setUp() {
            jobService = new JobInstanceService(jobInstanceDao, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, serverHealthService);
        }

        @Test
        void shouldReturnTheLatestAndOldestRunID() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            jobService.getOldestAndLatestJobInstanceId(username, pipelineName, stageName, jobConfigName);

            verify(jobInstanceDao).getOldestAndLatestJobInstanceId(pipelineName, stageName, jobConfigName);
        }


        @Test
        void shouldThrowErrorIfPipelineDoesNotExist() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

            assertThatCode(() -> jobService.getOldestAndLatestJobInstanceId(username, pipelineName, stageName, jobConfigName))
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("Pipeline with name 'pipeline' was not found!");
        }

        @Test
        void shouldThrowErrorIfUserDoesNotHaveAccessRights() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);

            assertThatCode(() -> jobService.getOldestAndLatestJobInstanceId(username, pipelineName, stageName, jobConfigName))
                    .isInstanceOf(NotAuthorizedException.class)
                    .hasMessage("User 'user' does not have permission to view pipeline with name 'pipeline'");
        }
    }
}
