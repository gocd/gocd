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
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Collections;

import static com.thoughtworks.go.server.service.BackupScheduler.BACKUP_SCHEDULER_TIMER_GROUP;
import static com.thoughtworks.go.server.service.BackupScheduler.BACKUP_SCHEDULER_TIMER_NAME;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupSchedulerTest {
    private Scheduler scheduler;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private ServerHealthService serverHealthService;
    @Mock
    private BackupService backupService;
    private BackupScheduler backupScheduler;
    private StdSchedulerFactory stdSchedulerFactory;

    @BeforeEach
    void setup() throws SchedulerException {
        stdSchedulerFactory = new StdSchedulerFactory();
        scheduler = stdSchedulerFactory.getScheduler();
        backupScheduler = new BackupScheduler(scheduler, goConfigService, serverHealthService, backupService);
    }

    @AfterEach
    void teardown() throws SchedulerException {
        scheduler.shutdown();
    }

    @Test
    void initialize_shouldRegisterJobsWithSchedulerAndAsAListenerOnConfigService() throws SchedulerException {
        ServerConfig serverConfig = new ServerConfig();
        BackupConfig backupConfig = new BackupConfig()
                .setSchedule("0 0 12 * * ?");
        serverConfig.setBackupConfig(backupConfig);

        BasicCruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.setServerConfig(serverConfig);
        when(goConfigService.cruiseConfig()).thenReturn(newCruiseConfig);

        backupScheduler.initialize();
        verify(goConfigService).register(backupScheduler);

        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(TriggerKey.triggerKey(BACKUP_SCHEDULER_TIMER_NAME, BACKUP_SCHEDULER_TIMER_GROUP));
        assertThat(trigger.getCronExpression()).isEqualTo(backupConfig.getSchedule());
        assertThat(trigger.getJobKey()).isEqualTo(JobKey.jobKey(BACKUP_SCHEDULER_TIMER_NAME, BACKUP_SCHEDULER_TIMER_GROUP));

        JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey(BACKUP_SCHEDULER_TIMER_NAME, BACKUP_SCHEDULER_TIMER_GROUP));
        assertThat(jobDetail.getJobDataMap()).isEqualTo(new JobDataMap(Collections.singletonMap(BackupScheduler.ScheduleBackupQuartzJob.BACKUP_SCHEDULER_KEY, backupScheduler)));
        assertThat(jobDetail.getJobClass()).isEqualTo(BackupScheduler.ScheduleBackupQuartzJob.class);
    }

    @Test
    void shouldUpdateServerHealthStatusWhenCronSpecCantBeParsed() {
        ServerConfig serverConfig = new ServerConfig();
        BackupConfig backupConfig = new BackupConfig()
                .setSchedule("bad cron");
        serverConfig.setBackupConfig(backupConfig);

        BasicCruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.setServerConfig(serverConfig);
        when(goConfigService.cruiseConfig()).thenReturn(newCruiseConfig);

        backupScheduler.initialize();

        verify(serverHealthService).update(
                ServerHealthState.error("Unable to schedule backup job.",
                        "Invalid cron syntax for backup configuration at offset 0: Illegal characters for this position: 'BAD'",
                        HealthStateType.general(HealthStateScope.forBackupCron())));
    }

    @Test
    void shouldUpdateServerHealthStatusWhenJobCannotBeScheduledWithScheduler() throws SchedulerException {
        ServerConfig serverConfig = new ServerConfig();
        BackupConfig backupConfig = new BackupConfig()
                .setSchedule("0 0 12 * * ?");
        serverConfig.setBackupConfig(backupConfig);

        BasicCruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.setServerConfig(serverConfig);
        when(goConfigService.cruiseConfig()).thenReturn(newCruiseConfig);

        scheduler.shutdown();

        backupScheduler.initialize();

        verify(serverHealthService).update(
                ServerHealthState.error("Unable to schedule backup job.",
                        "Check the server log for detailed errors: The Scheduler has been shutdown.",
                        HealthStateType.general(HealthStateScope.forBackupCron())));
    }

    @Test
    void shouldRescheduleTimerTriggerWhenBackupConfigChangesOnFullConfigSave() throws SchedulerException {
        ServerConfig serverConfig = new ServerConfig();
        BackupConfig backupConfig = new BackupConfig()
                .setSchedule("0 0 12 * * ?");
        serverConfig.setBackupConfig(backupConfig);

        BasicCruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.setServerConfig(serverConfig);
        when(goConfigService.cruiseConfig()).thenReturn(newCruiseConfig);

        backupScheduler.initialize();

        serverConfig.setBackupConfig(new BackupConfig()
                .setSchedule("0 0 13 * * ?"));
        backupScheduler.onConfigChange(newCruiseConfig);

        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(TriggerKey.triggerKey(BACKUP_SCHEDULER_TIMER_NAME, BACKUP_SCHEDULER_TIMER_GROUP));
        assertThat(trigger.getCronExpression()).isEqualTo("0 0 13 * * ?");
    }

    @Test
    void shouldRescheduleTimerTriggerWhenBackupConfigChangesOnEntityUpdate() throws SchedulerException {
        ServerConfig serverConfig = new ServerConfig();
        BackupConfig backupConfig = new BackupConfig()
                .setSchedule("0 0 12 * * ?");
        serverConfig.setBackupConfig(backupConfig);

        BasicCruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.setServerConfig(serverConfig);
        when(goConfigService.cruiseConfig()).thenReturn(newCruiseConfig);

        backupScheduler.initialize();

        backupScheduler.onEntityConfigChange(new BackupConfig()
                .setSchedule("0 0 13 * * ?"));

        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(TriggerKey.triggerKey(BACKUP_SCHEDULER_TIMER_NAME, BACKUP_SCHEDULER_TIMER_GROUP));
        assertThat(trigger.getCronExpression()).isEqualTo("0 0 13 * * ?");
    }
}
