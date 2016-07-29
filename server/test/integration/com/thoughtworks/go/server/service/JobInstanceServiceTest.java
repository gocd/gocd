/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.plugin.api.hook.joblifecycle.IJobPostCompletionHook;
import com.thoughtworks.go.plugin.api.hook.joblifecycle.IJobPreScheduleHook;
import com.thoughtworks.go.plugin.api.hook.joblifecycle.JobContext;
import com.thoughtworks.go.plugin.api.hook.joblifecycle.ResponseContext;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ExceptionHandler;
import com.thoughtworks.go.plugin.infra.PluginManager;
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
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.sql.SQLException;
import java.util.ArrayList;

import static com.thoughtworks.go.helper.JobInstanceMother.completed;
import static com.thoughtworks.go.helper.JobInstanceMother.scheduled;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class JobInstanceServiceTest {
    @Mock private JobInstanceDao jobInstanceDao;
    @Mock private JobResultTopic topic;
    @Mock private PropertiesService buildPropertiesService;
    @Mock private StageDao stageDao;
    @Mock private GoConfigService goConfigService;
    @Mock private CruiseConfig cruiseConfig;
	@Mock private SecurityService securityService;
    @Mock private PluginManager pluginManager;

    @Captor private ArgumentCaptor<Action<IJobPostCompletionHook>> jobPostCompletionActionHandlerCaptor;
    @Captor private ArgumentCaptor<ExceptionHandler<IJobPostCompletionHook>> jobPostCompletionExceptionHandlerCaptor;
    @Captor private ArgumentCaptor<Action<IJobPreScheduleHook>> jobPreScheduleActionHandlerCaptor;
    @Captor private ArgumentCaptor<ExceptionHandler<IJobPreScheduleHook>> jobPreScheduleExceptionHandlerCaptor;

    private JobInstance job;
    private TestTransactionSynchronizationManager transactionSynchronizationManager;
    private TransactionTemplate transactionTemplate;
    private JobStatusCache jobStatusCache;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        job = JobInstanceMother.building("dev");
        transactionSynchronizationManager = new TestTransactionSynchronizationManager();
        transactionTemplate = new TestTransactionTemplate(transactionSynchronizationManager);
        jobStatusCache = new JobStatusCache(stageDao);
    }

    @After
    public void after() {
        Mockito.verifyNoMoreInteractions(jobInstanceDao, stageDao, buildPropertiesService);
    }

    @Test
    public void shouldNotifyListenerWhenJobStatusChanged() throws Exception {
        final JobStatusListener listener = Mockito.mock(JobStatusListener.class);

        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
				null, pluginManager, listener);
        jobService.updateStateAndResult(job);

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(listener).jobStatusChanged(job);
    }

    @Test
    public void shouldNotifyAllListenersWhenUpdateJobStatus() throws Exception {
        final JobStatusListener listener1 = Mockito.mock(JobStatusListener.class, "listener1");
        final JobStatusListener listener2 = Mockito.mock(JobStatusListener.class, "listener2");

        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager, listener1, listener2);
        jobService.updateStateAndResult(job);

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(listener1).jobStatusChanged(job);
        verify(listener2).jobStatusChanged(job);
    }

    @Test
    public void shouldNotifyListenerWhenUpdateAssignedInfo() throws Exception {
        final JobStatusListener listener1 = Mockito.mock(JobStatusListener.class, "listener1");
        final JobStatusListener listener2 = Mockito.mock(JobStatusListener.class, "listener2");

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager, listener1, listener2);

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

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager, listener1, listener2);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
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

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager, failingListener, passingListener);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.save(new StageIdentifier(pipeline.getName(), null, pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter())), stage.getId(), job);
            }
        });

        verify(jobInstanceDao).save(1l, job);
        verify(passingListener).jobStatusChanged(job);
    }

    @Test
    public void shouldNotifyListenersWhenAssignedJobIsCancelled() {
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager);
        job.setAgentUuid("dummy agent");

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.cancelJob(job);
            }
        });

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(buildPropertiesService).saveCruiseProperties(job);
        verify(topic).post(new JobResultMessage(job.getIdentifier(), JobResult.Cancelled, job.getAgentUuid()));
    }

    @Test
    public void shouldNotNotifyListenersWhenScheduledJobIsCancelled() {
        final JobInstance scheduledJob = scheduled("dev");

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null,
                null, goConfigService, null, pluginManager);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.cancelJob(scheduledJob);
            }
        });

        jobService.cancelJob(scheduledJob);
        verify(jobInstanceDao).updateStateAndResult(scheduledJob);
        verify(buildPropertiesService).saveCruiseProperties(scheduledJob);
        verify(topic, never()).post(Matchers.<JobResultMessage>any());
    }

    @Test
    public void shouldNotNotifyListenersWhenACompletedJobIsCancelled() {
        final JobInstance completedJob = completed("dev");

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null,
                null, goConfigService, null, pluginManager);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.cancelJob(completedJob);
            }
        });

        jobService.cancelJob(completedJob);
        verify(jobInstanceDao, never()).updateStateAndResult(completedJob);
        verify(buildPropertiesService, never()).saveCruiseProperties(completedJob);
        verify(topic, never()).post(Matchers.<JobResultMessage>any());
    }

    @Test
    public void shouldNotNotifyListenersWhenAssignedJobCancellationTransactionRollsback() {
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager);
        job.setAgentUuid("dummy agent");

        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    jobService.cancelJob(job);
                    throw new RuntimeException("to rollback txn");
                }
            });
        } catch (RuntimeException e) {
            //ignore
        }

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(buildPropertiesService).saveCruiseProperties(job);
        verify(topic, never()).post(any(JobResultMessage.class));
    }

    @Test
    public void shouldNotNotifyListenersWhenScheduledJobIsFailed() {
        final JobInstance scheduledJob = scheduled("dev");
        scheduledJob.setId(10);
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null,
                null, goConfigService, null, pluginManager);

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
        verify(buildPropertiesService).saveCruiseProperties(scheduledJob);
        assertThat(scheduledJob.isFailed(), is(true));
    }

	@Test
	public void shouldDelegateToDAO_getJobHistoryCount() {
		final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache,
				transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, null, pluginManager);

		jobService.getJobHistoryCount("pipeline", "stage", "job");

		verify(jobInstanceDao).getJobHistoryCount("pipeline", "stage", "job");
	}

	@Test
	public void shouldDelegateToDAO_findJobHistoryPage() {
		when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
		when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);

		final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache,
				transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, pluginManager);

		Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
		jobService.findJobHistoryPage("pipeline", "stage", "job", pagination, "looser", new HttpOperationResult());

		verify(jobInstanceDao).findJobHistoryPage("pipeline", "stage", "job", pagination.getPageSize(), pagination.getOffset());
	}

	@Test
	public void shouldPopulateErrorWhenPipelineNotFound_findJobHistoryPage() {
		when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(false);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
		when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);

		final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache,
				transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, pluginManager);

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

		final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache,
				transactionTemplate, transactionSynchronizationManager, null, null, goConfigService, securityService, pluginManager);

		Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
		HttpOperationResult result = new HttpOperationResult();
		JobInstances jobHistoryPage = jobService.findJobHistoryPage("pipeline", "stage", "job", pagination, "looser", result);

		assertThat(jobHistoryPage, is(nullValue()));
		assertThat(result.canContinue(), is(false));
	}

    @Test
    public void shouldLoadOriginalJobPlan() {
        JobResolverService resolver = mock(JobResolverService.class);
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, topic, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                resolver,
                null, goConfigService, null, pluginManager);
        DefaultJobPlan expectedPlan = new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 7, new JobIdentifier(), null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        when(jobInstanceDao.loadPlan(7l)).thenReturn(expectedPlan);
        JobIdentifier givenId = new JobIdentifier("pipeline-name", 9, "label-9", "stage-name", "2", "job-name", 10l);
        when(resolver.actualJobIdentifier(givenId)).thenReturn(new JobIdentifier("pipeline-name", 8, "label-8", "stage-name", "1", "job-name", 7l));
        assertThat(jobService.loadOriginalJobPlan(givenId), sameInstance((JobPlan) expectedPlan));
        verify(jobInstanceDao).loadPlan(7l);
    }

    @Test
    public void shouldRegisterANewListener() throws SQLException {
        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
				null, pluginManager);
        JobStatusListener listener = mock(JobStatusListener.class);
        jobService.registerJobStateChangeListener(listener);
        jobService.updateStateAndResult(job);

        verify(jobInstanceDao).updateStateAndResult(job);
        verify(listener).jobStatusChanged(job);
    }

    @Test
    public void shouldGetCompletedJobsOnAgentOnTheGivenPage() {
        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager, null, null, goConfigService,
				null, pluginManager);
        ArrayList<JobInstance> expected = new ArrayList<JobInstance>();
        when(jobInstanceDao.totalCompletedJobsOnAgent("uuid")).thenReturn(500);
        when(jobInstanceDao.completedJobsOnAgent("uuid", JobInstanceService.JobHistoryColumns.pipeline, SortOrder.ASC, 50, 50)).thenReturn(expected);

        JobInstancesModel actualModel = jobService.completedJobsOnAgent("uuid", JobInstanceService.JobHistoryColumns.pipeline, SortOrder.ASC, 2, 50);

        assertThat(actualModel, is(new JobInstancesModel(new JobInstances(expected), Pagination.pageByNumber(2, 500, 50))));
        verify(jobInstanceDao).totalCompletedJobsOnAgent("uuid");
        verify(jobInstanceDao).completedJobsOnAgent("uuid", JobInstanceService.JobHistoryColumns.pipeline, SortOrder.ASC, 50, 50);
    }

    @Test
    public void shouldInvokePreScheduleHooks() {
        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                jobService.save(new StageIdentifier("pipelineName", 1, "1", "defaultStage", "1"), 42l, job);
            }
        });

        verify(pluginManager).doOnAll(eq(IJobPreScheduleHook.class), jobPreScheduleActionHandlerCaptor.capture(), jobPreScheduleExceptionHandlerCaptor.capture());

        IJobPreScheduleHook preScheduleHook = mock(IJobPreScheduleHook.class);
        Action<IJobPreScheduleHook> value = jobPreScheduleActionHandlerCaptor.getValue();
        value.execute(preScheduleHook, null);

        final ArgumentCaptor<JobContext> argumentCaptor = ArgumentCaptor.forClass(JobContext.class);
        verify(preScheduleHook).call(argumentCaptor.capture());

        JobContext jobContext = argumentCaptor.getValue();
        Assert.assertThat(jobContext.getPipelineName(), Is.is("pipelineName"));
        Assert.assertThat(jobContext.getPipelineCounter(), Is.is("1"));
        Assert.assertThat(jobContext.getPipelineLabel(), Is.is("1"));
        Assert.assertThat(jobContext.getStageName(), Is.is("defaultStage"));
        Assert.assertThat(jobContext.getStageCounter(), Is.is(String.valueOf("1")));
        Assert.assertThat(jobContext.getJobName(), Is.is(job.getName()));
        Assert.assertThat(jobContext.getJobCounter(), Is.is(String.valueOf(job.getId())));

        verify(jobInstanceDao).save(42, job);
    }

    @Test
    public void shouldReThrowExceptionWhenPreScheduleHookThrowsException() {

        final JobInstanceService jobService = new JobInstanceService(jobInstanceDao, null, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager);

        doThrow(new RuntimeException("This will fail with an exception, when plugin manager runs the exception handler.")).when(pluginManager).doOnAll(eq(IJobPreScheduleHook.class), jobPreScheduleActionHandlerCaptor.capture(), jobPreScheduleExceptionHandlerCaptor.capture());

        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    jobService.save(new StageIdentifier("pipelineName", 1, "1", "defaultStage", "1"), 42l, job);
                }
            });
            fail("Expected this to throw an exception");
        } catch (Exception e) {
        }

        IJobPreScheduleHook preScheduleHook = mock(IJobPreScheduleHook.class);
        verify(pluginManager).doOnAll(eq(IJobPreScheduleHook.class), jobPreScheduleActionHandlerCaptor.capture(), jobPreScheduleExceptionHandlerCaptor.capture());

        try {
            jobPreScheduleExceptionHandlerCaptor.getValue().handleException(preScheduleHook, new RuntimeException("Bundle throws exception"));
        } catch (Exception e) {
            assertThat(e.getMessage(), is("java.lang.RuntimeException: Bundle throws exception"));
        }
    }

    @Test
    public void shouldCallPostCommitHookAfterJobCompletion() {
        //given
        JobInstance completedJob = completed("dev");
        IJobPostCompletionHook jobPostCompletionHook = mock(IJobPostCompletionHook.class);

        when(jobPostCompletionHook.call(any(JobContext.class))).thenReturn(new ResponseContext(ResponseContext.ResponseCode.SUCCESS, "success"));

        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager);

        //when
        jobService.updateStateAndResult(completedJob);

        //then
        verify(pluginManager).doOnAll(eq(IJobPostCompletionHook.class), jobPostCompletionActionHandlerCaptor.capture(), jobPostCompletionExceptionHandlerCaptor.capture());

        jobPostCompletionActionHandlerCaptor.getValue().execute(jobPostCompletionHook, null);
        final ArgumentCaptor<JobContext> jobContextCaptor = ArgumentCaptor.forClass(JobContext.class);
        verify(jobPostCompletionHook).call(jobContextCaptor.capture());

        JobContext jobContext = jobContextCaptor.getValue();
        assertThat(jobContext.getPipelineName(), Is.is(completedJob.getIdentifier().getPipelineName()));
        assertThat(jobContext.getPipelineCounter(), Is.is(String.valueOf(completedJob.getIdentifier().getPipelineCounter())));
        assertThat(jobContext.getPipelineLabel(), Is.is(completedJob.getIdentifier().getPipelineLabel()));
        assertThat(jobContext.getStageName(), Is.is(completedJob.getIdentifier().getStageName()));
        assertThat(jobContext.getStageCounter(), Is.is(completedJob.getIdentifier().getStageCounter()));
        assertThat(jobContext.getJobName(), Is.is(completedJob.getName()));
        assertThat(jobContext.getJobCounter(), Is.is(String.valueOf(completedJob.getId())));
        assertThat(jobContext.getJobStatus(), Is.is(completedJob.getResult().getStatus()));
        assertThat(jobContext.getAgentUuid(), Is.is(completedJob.getAgentUuid()));

        verify(jobInstanceDao).updateStateAndResult(completedJob);
        verify(buildPropertiesService).saveCruiseProperties(completedJob);
    }

    @Test
    public void shouldNotCallPostCommitHookIfJobNotCompleted() {

        //given
        JobInstance jobInstance = JobInstanceMother.building("dev");
        IJobPostCompletionHook jobPostCompletionHook = mock(IJobPostCompletionHook.class);
        JobInstanceService jobService = new JobInstanceService(jobInstanceDao, buildPropertiesService, null, jobStatusCache, transactionTemplate, transactionSynchronizationManager,
                null, null, goConfigService, null, pluginManager);

        //when
        jobService.updateStateAndResult(jobInstance);

        //then
        verify(jobPostCompletionHook, never()).call(any(JobContext.class));
        verify(jobInstanceDao).updateStateAndResult(jobInstance);
    }
}
