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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.TimerConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;
import java.util.Set;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.groupEquals;

/**
 * @understands scheduling pipelines based on a timer
 */
@Component
public class TimerScheduler implements ConfigChangedListener {
    private static final Logger LOG = LoggerFactory.getLogger(TimerScheduler.class);

    private GoConfigService goConfigService;
    private BuildCauseProducerService buildCauseProducerService;
    private ServerHealthService serverHealthService;
    private MaintenanceModeService maintenanceModeService;
    private SystemEnvironment systemEnvironment;
    private Scheduler quartzScheduler;
    protected static final String PIPELINE_TRIGGGER_TIMER_GROUP = "PIPELINE_TRIGGGER_TIMER_GROUP";
    protected static final String BUILD_CAUSE_PRODUCER_SERVICE = "BuildCauseProducerService";
    protected static final String MAINTENANCE_MODE_SERVICE = "MaintenanceModeService";
    protected static final String PIPELINE_CONFIG = "PipelineConfig";

    @Autowired
    public TimerScheduler(Scheduler scheduler, GoConfigService goConfigService,
                          BuildCauseProducerService buildCauseProducerService,
                          ServerHealthService serverHealthService,
                          MaintenanceModeService maintenanceModeService,
                          SystemEnvironment systemEnvironment) {
        this.goConfigService = goConfigService;
        this.buildCauseProducerService = buildCauseProducerService;
        this.quartzScheduler = scheduler;
        this.serverHealthService = serverHealthService;
        this.maintenanceModeService = maintenanceModeService;
        this.systemEnvironment = systemEnvironment;
    }

    public void initialize() {
        if (systemEnvironment.isServerInStandbyMode()) {
            LOG.info("GoCD server in 'standby' mode, skipping scheduling timer triggered pipelines.");
            return;
        }

        scheduleAllJobs(goConfigService.getAllPipelineConfigs());
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                unscheduleJob(CaseInsensitiveString.str(pipelineConfig.name()));
                scheduleJob(quartzScheduler, pipelineConfig);
            }
        };
    }

    private void scheduleAllJobs(List<PipelineConfig> pipelineConfigs) {
        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            scheduleJob(quartzScheduler, pipelineConfig);
        }
    }

    private void scheduleJob(Scheduler scheduler, PipelineConfig pipelineConfig) {
        TimerConfig timer = pipelineConfig.getTimer();
        if (timer != null) {
            try {
                CronTrigger trigger = newTrigger()
                        .withIdentity(triggerKey(CaseInsensitiveString.str(pipelineConfig.name()), PIPELINE_TRIGGGER_TIMER_GROUP))
                        .withSchedule(cronSchedule(new CronExpression(timer.getTimerSpec())))
                        .build();


                JobDetail jobDetail = newJob()
                        .withIdentity(jobKey(CaseInsensitiveString.str(pipelineConfig.name()), PIPELINE_TRIGGGER_TIMER_GROUP))
                        .ofType(SchedulePipelineQuartzJob.class)
                        .usingJobData(jobDataMapFor(pipelineConfig))
                        .build();

                scheduler.scheduleJob(jobDetail, trigger);
                LOG.info("Initialized timer for pipeline {} with {}", pipelineConfig.name(), timer.getTimerSpec());
            } catch (ParseException e) {
                showPipelineError(pipelineConfig, e,
                        "Bad timer specification for timer in Pipeline: " + pipelineConfig.name(),
                        "Cannot schedule pipeline using the timer");
            } catch (SchedulerException e) {
                showPipelineError(pipelineConfig, e,
                        "Could not register pipeline '" + pipelineConfig.name() + "' with timer",
                        "");
            }
        }
    }

    private void showPipelineError(PipelineConfig pipelineConfig, Exception e, String msg, String description) {
        LOG.error(msg, e);
        serverHealthService.update(
                ServerHealthState.error(msg,
                        description,
                        HealthStateType.general(HealthStateScope.forPipeline(CaseInsensitiveString.str(pipelineConfig.name())))));
    }

    private JobDataMap jobDataMapFor(PipelineConfig pipelineConfig) {
        JobDataMap map = new JobDataMap();
        map.put(BUILD_CAUSE_PRODUCER_SERVICE, buildCauseProducerService);
        map.put(MAINTENANCE_MODE_SERVICE, maintenanceModeService);
        map.put(PIPELINE_CONFIG, pipelineConfig);
        return map;
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        unscheduleAllJobs();
        scheduleAllJobs(newCruiseConfig.getAllPipelineConfigs());
    }

    private void unscheduleAllJobs() {
        try {
            Set<JobKey> jobKeys = quartzScheduler.getJobKeys(groupEquals(PIPELINE_TRIGGGER_TIMER_GROUP));
            for (JobKey jobKey : jobKeys) {
                unscheduleJob(jobKey.getName());
            }
        } catch (SchedulerException e) {
            LOG.error("Could not unschedule quartz jobs", e);
        }
    }

    private void unscheduleJob(String pipelineName) {
        try {
            JobKey jobKey = jobKey(pipelineName, PIPELINE_TRIGGGER_TIMER_GROUP);
            if (quartzScheduler.getJobDetail(jobKey) != null) {
                quartzScheduler.unscheduleJob(triggerKey(pipelineName, PIPELINE_TRIGGGER_TIMER_GROUP));
                quartzScheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            LOG.error("Could not unschedule quartz jobs", e);
        }
    }

    public static class SchedulePipelineQuartzJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            BuildCauseProducerService buildCauseProducerService = (BuildCauseProducerService) jobDataMap.get(BUILD_CAUSE_PRODUCER_SERVICE);
            MaintenanceModeService maintenanceModeService = (MaintenanceModeService) jobDataMap.get(MAINTENANCE_MODE_SERVICE);
            PipelineConfig pipelineConfig = (PipelineConfig) jobDataMap.get(PIPELINE_CONFIG);

            if (maintenanceModeService.isMaintenanceMode()) {
                LOG.debug("[Maintenance Mode] GoCD server is in 'maintenance' mode, skipping scheduling of timer triggered pipeline: '{}'.", pipelineConfig.getName());
                return;
            }

            buildCauseProducerService.timerSchedulePipeline(pipelineConfig, new ServerHealthStateOperationResult());
        }
    }
}
