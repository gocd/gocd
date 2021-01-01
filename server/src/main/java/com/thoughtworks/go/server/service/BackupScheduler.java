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

import com.thoughtworks.go.config.BackupConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Collections;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
public class BackupScheduler extends EntityConfigChangedListener<BackupConfig> implements ConfigChangedListener {
    private static final Logger LOG = LoggerFactory.getLogger(BackupScheduler.class);
    static final String BACKUP_SCHEDULER_TIMER_NAME = "backup";
    static final String BACKUP_SCHEDULER_TIMER_GROUP = "BACKUP_SCHEDULER_TIMER_GROUP";

    private final Scheduler quartzScheduler;
    private final GoConfigService goConfigService;
    private final ServerHealthService serverHealthService;
    private final BackupService backupService;

    @Autowired
    public BackupScheduler(Scheduler quartzScheduler,
                           GoConfigService goConfigService,
                           ServerHealthService serverHealthService,
                           BackupService backupService) {
        this.quartzScheduler = quartzScheduler;
        this.goConfigService = goConfigService;
        this.serverHealthService = serverHealthService;
        this.backupService = backupService;
    }

    public void initialize() {
        scheduleNewBackupJob(goConfigService.cruiseConfig().server().getBackupConfig());
        goConfigService.register(this);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        onEntityConfigChange(newCruiseConfig.server().getBackupConfig());
    }

    @Override
    public void onEntityConfigChange(BackupConfig newBackupConfig) {
        try {
            unscheduleExistingBackupJob();
            scheduleNewBackupJob(newBackupConfig);
        } catch (SchedulerException e) {
            LOG.error("Could not unschedule quartz jobs", e);
        }
    }

    private void scheduleNewBackupJob(BackupConfig newBackupConfig) {
        if (newBackupConfig == null || isBlank(newBackupConfig.getSchedule())) {
            return;
        }
        try {
            JobDetail jobDetail = newJob()
                    .withIdentity(jobKey())
                    .ofType(ScheduleBackupQuartzJob.class)
                    .usingJobData(jobDataMap())
                    .build();

            CronTrigger trigger = newTrigger()
                    .withIdentity(triggerKey())
                    .withSchedule(cronSchedule(new CronExpression(newBackupConfig.getSchedule())))
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            LOG.info("Initialized backup job with schedule " + newBackupConfig.getSchedule());
            clearServerHealthError();
        } catch (SchedulerException e) {
            LOG.error("Unable to schedule backup job", e);
            setServerHealthError("Unable to schedule backup job.", "Check the server log for detailed errors: " + e.getMessage());
        } catch (ParseException e) {
            LOG.error("Unable to schedule backup job", e);
            setServerHealthError("Unable to schedule backup job.", "Invalid cron syntax for backup configuration at offset " + e.getErrorOffset() + ": " + e.getMessage());
        }
    }

    private JobDataMap jobDataMap() {
        return new JobDataMap(Collections.singletonMap(ScheduleBackupQuartzJob.BACKUP_SCHEDULER_KEY, this));
    }

    private void unscheduleExistingBackupJob() throws SchedulerException {
        if (quartzScheduler.getJobDetail(jobKey()) != null) {
            quartzScheduler.unscheduleJob(triggerKey());
            quartzScheduler.deleteJob(jobKey());
        }
    }

    private void performBackup() {
        ServerBackup backup = backupService.backupViaTimer();

        if (backup.isSuccessful()) {
            clearServerHealthError();
        } else {
            setServerHealthError("Unable to perform scheduled backup.", backup.getMessage());
        }
    }


    private void setServerHealthError(String message, String description) {
        serverHealthService.update(
                ServerHealthState.error(message,
                        description,
                        HealthStateType.general(HealthStateScope.forBackupCron()))
        );
    }

    private void clearServerHealthError() {
        serverHealthService.update(
                ServerHealthState.success(HealthStateType.general(HealthStateScope.forBackupCron()))
        );
    }

    private static JobKey jobKey() {
        return JobKey.jobKey(BACKUP_SCHEDULER_TIMER_NAME, BACKUP_SCHEDULER_TIMER_GROUP);
    }

    private static TriggerKey triggerKey() {
        return TriggerKey.triggerKey(BACKUP_SCHEDULER_TIMER_NAME, BACKUP_SCHEDULER_TIMER_GROUP);
    }

    public static class ScheduleBackupQuartzJob implements Job {
        static final String BACKUP_SCHEDULER_KEY = "BACKUP_SCHEDULER_KEY";

        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

            LOG.info("Performing timer based backup");
            BackupScheduler backupScheduler = (BackupScheduler) jobDataMap.get(BACKUP_SCHEDULER_KEY);
            backupScheduler.performBackup();
        }
    }


}
