/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.TimerConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.quartz.*;

import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfigWithTimer;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;

public class TimerSchedulerTest {
    private SchedulerFactory schedulerFactory;
    private Scheduler scheduler;

    @Before public void setup() throws Exception {
        schedulerFactory = mock(SchedulerFactory.class);
        scheduler = mock(Scheduler.class);
    }

    @After public void teardown() throws Exception {
        Mockito.verifyNoMoreInteractions(schedulerFactory, scheduler);
    }

    @Test
    public void shouldRegisterJobsWithSchedulerForEachPipelineWithTimerOnInit() throws Exception {
        GoConfigService goConfigService = mock(GoConfigService.class);
        List<PipelineConfig> pipelineConfigs = asList(
                pipelineConfigWithTimer("uat", "0 15 10 ? * MON-FRI"),
                pipelineConfig("dist"));
        when(goConfigService.getAllPipelineConfigs()).thenReturn(pipelineConfigs);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, null, null);
        timerScheduler.initialize();

        JobDetail expectedJob = JobBuilder.newJob()
                .ofType(TimerScheduler.SchedulePipelineQuartzJob.class)
                .withIdentity(jobKey("uat", "CruiseTimers"))
                .build();
        Trigger expectedTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey("uat", "CruiseTimers"))
                .withSchedule(cronSchedule("0 15 10 ? * MON-FRI"))
                .build();
        verify(scheduler).scheduleJob(expectedJob, expectedTrigger);
    }

    @Test
    public void shouldUpdateServerHealthStatusWhenCronSpecCantBeParsed() throws Exception {
        GoConfigService goConfigService = mock(GoConfigService.class);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfigWithTimer("uat", "bad cron spec!!!")));

        ServerHealthService serverHealthService = mock(ServerHealthService.class);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, null, serverHealthService);
        timerScheduler.initialize();

        verify(serverHealthService).update(
                ServerHealthState.error("Bad timer specification for timer in Pipeline: uat", "Cannot schedule pipeline using the timer",
                        HealthStateType.general(HealthStateScope.forPipeline("uat"))));
    }

    @Test
    public void shouldScheduleOtherPipelinesEvenIfOneHasAnInvalidCronSpec() throws Exception {
        GoConfigService goConfigService = mock(GoConfigService.class);
        List<PipelineConfig> pipelineConfigs = asList(
                pipelineConfigWithTimer("uat", "---- bad cron spec!"),
                pipelineConfigWithTimer("dist", "0 15 10 ? * MON-FRI"));
        when(goConfigService.getAllPipelineConfigs()).thenReturn(pipelineConfigs);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, null, mock(ServerHealthService.class));
        timerScheduler.initialize();

        JobDetail expectedJob =JobBuilder.newJob()
                .ofType(TimerScheduler.SchedulePipelineQuartzJob.class)
                .withIdentity(jobKey("dist", "CruiseTimers"))
                .build();

        Trigger expectedTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey("dist", "CruiseTimers"))
                .withSchedule(cronSchedule("0 15 10 ? * MON-FRI"))
                .build();
        verify(scheduler).scheduleJob(expectedJob, expectedTrigger);

    }

    @Test
    public void shouldUpdateServerHealthStatusWhenPipelineCantBeAddedToTheQuartzScheduler() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenThrow(
                new SchedulerException("scheduling failed!"));

        SchedulerFactory schedulerFactory = mock(SchedulerFactory.class);

        GoConfigService goConfigService = mock(GoConfigService.class);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfigWithTimer("uat", "* * * * * ?")));

        ServerHealthService serverHealthService = mock(ServerHealthService.class);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, null, serverHealthService);
        timerScheduler.initialize();

        verify(serverHealthService).update(
                ServerHealthState.error("Could not register pipeline 'uat' with timer", "",
                        HealthStateType.general(HealthStateScope.forPipeline("uat"))));

    }

    @Test
    public void shouldRegisterAsACruiseConfigChangeListener() throws Exception {
        GoConfigService goConfigService = mock(GoConfigService.class);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, null, null);
        timerScheduler.initialize();

        verify(goConfigService).register(timerScheduler);
    }

    @Test
    public void shouldRescheduleTimerTriggerPipelineWhenItsConfigChanges() throws SchedulerException {
        GoConfigService goConfigService = mock(GoConfigService.class);

        String pipelineName = "timer-based-pipeline";
        when(scheduler.getJobDetail(jobKey(pipelineName, TimerScheduler.QUARTZ_GROUP))).thenReturn(mock(JobDetail.class));
        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, null, null);
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());
        timerScheduler.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener= (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);

        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString(pipelineName));
        when(pipelineConfig.getTimer()).thenReturn(new TimerConfig("* * * * * ?", true));
        ArgumentCaptor<JobDetail> jobDetailArgumentCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<CronTrigger> triggerArgumentCaptor = ArgumentCaptor.forClass(CronTrigger.class);
        when(scheduler.scheduleJob(jobDetailArgumentCaptor.capture(), triggerArgumentCaptor.capture())).thenReturn(new Date());
        pipelineConfigChangeListener.onEntityConfigChange(pipelineConfig);

        assertThat(jobDetailArgumentCaptor.getValue().getKey().getName(), is(pipelineName));
        assertThat(triggerArgumentCaptor.getValue().getCronExpression(), is("* * * * * ?"));

        verify(scheduler).getJobDetail(jobKey(pipelineName, TimerScheduler.QUARTZ_GROUP));

        verify(scheduler).unscheduleJob(triggerKey(pipelineName, TimerScheduler.QUARTZ_GROUP));
        verify(scheduler).deleteJob(jobKey(pipelineName, TimerScheduler.QUARTZ_GROUP));
        verify(scheduler).scheduleJob(jobDetailArgumentCaptor.getValue(), triggerArgumentCaptor.getValue());
    }

}
