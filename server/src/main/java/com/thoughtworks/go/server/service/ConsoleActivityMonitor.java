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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.thoughtworks.go.serverhealth.HealthStateScope.forJob;
import static java.lang.String.format;
import static java.lang.String.valueOf;

@Component
public class ConsoleActivityMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleActivityMonitor.class);

    private final TimeProvider timeProvider;
    private final JobInstanceService jobInstanceService;
    private final ServerHealthService serverHealthService;
    private final GoConfigService goConfigService;
    private ConsoleService consoleService;
    private final ConcurrentMap<JobIdentifier, Long> jobLastActivityMap;
    private final ConcurrentMap<JobIdentifier, Long> jobScheduledMap;
    private final long warningThreshold;

    @Autowired
    public ConsoleActivityMonitor(TimeProvider timeProvider, SystemEnvironment systemEnvironment, JobInstanceService jobInstanceService, ServerHealthService serverHealthService,
                                  GoConfigService goConfigService, ConsoleService consoleService) {
        this.timeProvider = timeProvider;
        this.jobInstanceService = jobInstanceService;
        this.serverHealthService = serverHealthService;
        this.goConfigService = goConfigService;
        this.consoleService = consoleService;
        this.jobLastActivityMap = new ConcurrentHashMap<>();
        this.jobScheduledMap = new ConcurrentHashMap<>();
        this.warningThreshold = systemEnvironment.getUnresponsiveJobWarningThreshold();
        jobInstanceService.registerJobStateChangeListener(new ActiveJobListener(this));
        jobInstanceService.registerJobStateChangeListener(new ScheduledJobListener(this));
    }

    public void populateActivityMap() {
        long now = timeProvider.currentTimeMillis();
        for (JobInstance jobInstance : jobInstanceService.allRunningJobs()) {
            JobIdentifier jobIdentifier = jobInstance.getIdentifier();
            if (jobInstance.getState().isScheduled()) {
                jobScheduledMap.put(jobIdentifier, now);
            } else if (jobInstance.getState().isActiveOnAgent()) {
                jobLastActivityMap.put(jobIdentifier, now);
            }
        }
        LOGGER.info("Found '{}' building jobs. Added them with '{}' as the last heard time", jobLastActivityMap.size(), new DateTime(now));
        LOGGER.info("Found '{}' scheduled jobs. Added them with '{}' as the last heard time", jobScheduledMap.size(), new DateTime(now));
    }

    public void consoleUpdatedFor(JobIdentifier jobIdentifier) {
        long now = timeProvider.currentTimeMillis();
        Long previously = jobLastActivityMap.replace(jobIdentifier, now);
        if (previously != null && now - previously > warningThreshold) {
            removeHungJobWarning(jobIdentifier);
        }
    }

    public void cancelUnresponsiveJobs(ScheduleService scheduleService) {
        long currentTime = timeProvider.currentTimeMillis();
        checkForHungJobs(scheduleService, this.jobLastActivityMap, currentTime, runningJobMessages());
        checkForHungJobs(scheduleService, this.jobScheduledMap, currentTime, scheduledJobMessages());
    }

    private void checkForHungJobs(ScheduleService scheduleService, ConcurrentMap<JobIdentifier, Long> jobActivityMap, long currentTime, LogMessages messages) {
        for (Map.Entry<JobIdentifier, Long> jobTimeEntry : jobActivityMap.entrySet()) {
            long difference = currentTime - jobTimeEntry.getValue();
            JobIdentifier jobIdentifier = jobTimeEntry.getKey();
            if (shouldCancelHungJob(jobIdentifier, difference)) {
                scheduleService.cancelJob(jobIdentifier);
                try {
                    consoleService.appendToConsoleLog(jobIdentifier, messages.consoleMessage(inMinutes(jobTerminationThreshold(jobIdentifier))));
                } catch (Exception e) {
                    LOGGER.error("Failed to update console log with reason for cancelling hung job '{}'", jobIdentifier.buildLocator(), e);
                }
                jobActivityMap.remove(jobIdentifier);
                removeHungJobWarning(jobIdentifier);
                LOGGER.info("Cancelled hung job '{}' as it was hung for more than '{}' minutes", jobIdentifier.buildLocator(), inMinutes(difference));
            } else if (difference > warningThreshold) {
                LOGGER.info("Job '{}' hung for more than '{}' minutes", jobIdentifier.buildLocator(), inMinutes(difference));
                removeHungJobWarning(jobIdentifier);
                addJobHungWarning(jobIdentifier, difference, messages);
            }
        }
    }

    private void addJobHungWarning(JobIdentifier jobIdentifier, long difference, LogMessages messages) {
        String namespacedJob = format("%s/%s/%s", jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName());
        serverHealthService.update(ServerHealthState.warningWithHtml(
                format("Job '%s' is not responding", namespacedJob),
                messages.hungWarningMessage(jobIdentifier.buildLocator(), namespacedJob, inMinutes(difference)),
                HealthStateType.general(forJob(jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName()))));
    }

    private String inMinutes(long difference) {
        return valueOf(difference / 1000 / 60);
    }

    private void removeHungJobWarning(JobIdentifier jobIdentifier) {
        serverHealthService.removeByScope(forJob(jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName()));
    }

    private boolean shouldCancelHungJob(JobIdentifier jobIdentifier, long difference) {
        return goConfigService.canCancelJobIfHung(jobIdentifier) && difference > jobTerminationThreshold(jobIdentifier);
    }

    private long jobTerminationThreshold(JobIdentifier jobIdentifier) {
        return goConfigService.getUnresponsiveJobTerminationThreshold(jobIdentifier);
    }

    private interface LogMessages {
        String consoleMessage(String difference);

        String hungWarningMessage(String buildLocator, String namespacedJob, String difference);
    }

    private LogMessages scheduledJobMessages() {
        return new LogMessages() {
            @Override
            public String consoleMessage(String difference) {
                return format("Go cancelled this job as it was not assigned an agent for more than %s minute(s)", difference);
            }

            @Override
            public String hungWarningMessage(String buildLocator, String namespacedJob, String difference) {
                return format("Job <a href='/go/tab/build/detail/%s'>%s</a> is currently running but was not assigned an agent in the last %s minute(s). This job may be hung.", buildLocator, namespacedJob, difference);
            }
        };
    }

    private LogMessages runningJobMessages() {
        return new LogMessages() {
            @Override
            public String consoleMessage(String difference) {
                return format("Go cancelled this job as it has not shown any console activity for more than %s minute(s)", difference);
            }

            @Override
            public String hungWarningMessage(String buildLocator, String namespacedJob, String difference) {
                return format("Job <a href='/go/tab/build/detail/%s'>%s</a> is currently running but has not shown any console activity in the last %s minute(s). This job may be hung.", buildLocator, namespacedJob, difference);
            }
        };
    }

    static final class ScheduledJobListener implements JobStatusListener {
        private final ConsoleActivityMonitor consoleActivityMonitor;

        private ScheduledJobListener(ConsoleActivityMonitor consoleActivityMonitor) {
            this.consoleActivityMonitor = consoleActivityMonitor;
        }

        @Override
        public void jobStatusChanged(JobInstance job) {
            JobIdentifier identifier = job.getIdentifier();
            if (job.getState().isScheduled()) {
                consoleActivityMonitor.jobScheduledMap.putIfAbsent(identifier, consoleActivityMonitor.timeProvider.currentTimeMillis());
            } else if (job.getState().isActiveOnAgent() || job.isCompleted()) {
                Long timestamp = consoleActivityMonitor.jobScheduledMap.remove(identifier);
                if (timestamp != null) {
                    consoleActivityMonitor.removeHungJobWarning(identifier);
                }
            }
        }
    }

    static final class ActiveJobListener implements JobStatusListener {
        private final ConsoleActivityMonitor consoleActivityMonitor;

        private ActiveJobListener(ConsoleActivityMonitor consoleActivityMonitor) {
            this.consoleActivityMonitor = consoleActivityMonitor;
        }

        @Override
        public void jobStatusChanged(JobInstance job) {
            JobIdentifier identifier = job.getIdentifier();
            if (job.getState().isBuilding()) {
                consoleActivityMonitor.jobLastActivityMap.putIfAbsent(identifier, consoleActivityMonitor.timeProvider.currentTimeMillis());
            } else if (job.isCompleted() || job.isRescheduled()) {
                consoleActivityMonitor.jobLastActivityMap.remove(identifier);
                consoleActivityMonitor.removeHungJobWarning(identifier);
            }
        }
    }
}
