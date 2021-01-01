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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.domain.ServerMaintenanceMode;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.util.List;

import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfigWithTimer;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

public class TimerSchedulerQuartzIntegrationTest {
    private StdSchedulerFactory quartzSchedulerFactory;
    private Scheduler scheduler;
    private MaintenanceModeService maintenanceModeService;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = new SystemEnvironment();
        maintenanceModeService = new MaintenanceModeService(new TimeProvider(), systemEnvironment);
        quartzSchedulerFactory = new StdSchedulerFactory();
        scheduler = quartzSchedulerFactory.getScheduler();
        scheduler.start();
    }

    @After
    public void tearDown() throws SchedulerException {
        quartzSchedulerFactory.getScheduler().shutdown();
    }

    @Test
    public void shouldExecuteScheduledJobsWhenInvokedFromQuartz() throws InterruptedException {
        PipelineConfig uat = pipelineConfigWithTimer("uat", "* * * * * ?");
        PipelineConfig dist = pipelineConfigWithTimer("dist", "* * * * * ?");
        List<PipelineConfig> pipelineConfigs = asList(uat, dist);

        GoConfigService goConfigService = mock(GoConfigService.class);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(pipelineConfigs);

        BuildCauseProducerService buildCauseProducerService = mock(BuildCauseProducerService.class);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, buildCauseProducerService, null, maintenanceModeService, systemEnvironment);
        timerScheduler.initialize();

        pauseForScheduling();
        verify(buildCauseProducerService, atLeastOnce()).timerSchedulePipeline(eq(uat), any(
                ServerHealthStateOperationResult.class));
        verify(buildCauseProducerService, atLeastOnce()).timerSchedulePipeline(eq(dist), any(ServerHealthStateOperationResult.class));
    }

    @Test
    public void shouldNotExecuteScheduledJobsWhenServerIsInMaintenanceMode() throws InterruptedException {
        PipelineConfig uat = pipelineConfigWithTimer("uat", "* * * * * ?");
        PipelineConfig dist = pipelineConfigWithTimer("dist", "* * * * * ?");
        List<PipelineConfig> pipelineConfigs = asList(uat, dist);

        GoConfigService goConfigService = mock(GoConfigService.class);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(pipelineConfigs);

        BuildCauseProducerService buildCauseProducerService = mock(BuildCauseProducerService.class);
        ServerMaintenanceMode serverMaintenanceMode = new ServerMaintenanceMode();
        serverMaintenanceMode.setMaintenanceMode(true);
        maintenanceModeService.update(serverMaintenanceMode);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, buildCauseProducerService, null, maintenanceModeService, systemEnvironment);
        timerScheduler.initialize();

        pauseForScheduling();
        verify(buildCauseProducerService, never()).timerSchedulePipeline(eq(uat), any(ServerHealthStateOperationResult.class));
        verify(buildCauseProducerService, never()).timerSchedulePipeline(eq(dist), any(ServerHealthStateOperationResult.class));
    }

    @Test
    public void shouldUpdateJobsInTheQuartzSchedulerOnConfigChange() throws InterruptedException {
        PipelineConfig uat = pipelineConfigWithTimer("uat", "* * * * * ?");
        PipelineConfig dist = pipelineConfigWithTimer("dist", "* * * * * ?");
        List<PipelineConfig> pipelineConfigs = asList(uat, dist);

        GoConfigService goConfigService = mock(GoConfigService.class);
        when(goConfigService.getAllPipelineConfigs()).thenReturn(pipelineConfigs);

        BuildCauseProducerService buildCauseProducerService = mock(BuildCauseProducerService.class);

        TimerScheduler timerScheduler = new TimerScheduler(scheduler, goConfigService, buildCauseProducerService, null, maintenanceModeService, systemEnvironment);
        timerScheduler.initialize();

        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.getGroups().add(new BasicPipelineConfigs(uat));
        timerScheduler.onConfigChange(cruiseConfig);

        pauseForScheduling();
        verify(buildCauseProducerService, atLeastOnce()).timerSchedulePipeline(eq(uat), any(ServerHealthStateOperationResult.class));
    }

    private void pauseForScheduling() throws InterruptedException {
        Thread.sleep(1000);
    }

}
