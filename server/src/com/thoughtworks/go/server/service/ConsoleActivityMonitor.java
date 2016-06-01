/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsoleActivityMonitor {
    private static final Logger LOGGER = Logger.getLogger(ConsoleActivityMonitor.class);

    private final TimeProvider timeProvider;
    private final JobInstanceService jobInstanceService;
    private final ServerHealthService serverHealthService;
    private final GoConfigService goConfigService;
    private ConsoleService consoleService;
    private final ConcurrentMap<JobIdentifier, Long> jobLastActivityMap;
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
        warningThreshold = systemEnvironment.getUnresponsiveJobWarningThreshold();
        jobInstanceService.registerJobStateChangeListener(new ActiveJobListener(this));
    }

    public void populateActivityMap() {
        long now = timeProvider.currentTimeMillis();
        for (JobIdentifier jobIdentifier : jobInstanceService.allBuildingJobs()) {
            jobLastActivityMap.put(jobIdentifier, now);
        }
        LOGGER.info(String.format("Found '%s' building jobs. Added them with '%s' as the last heard time", jobLastActivityMap.size(), new DateTime(now)));
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
        for (Map.Entry<JobIdentifier, Long> jobTimeEntry : jobLastActivityMap.entrySet()) {
            long difference = currentTime - jobTimeEntry.getValue();
            JobIdentifier jobIdentifier = jobTimeEntry.getKey();
            if (shouldCancelHungJob(jobIdentifier, difference)) {
                scheduleService.cancelJob(jobIdentifier);
                try {
                    consoleService.appendToConsoleLog(jobIdentifier,
                            String.format("Go cancelled this job as it has not generated any console output for more than %s minute(s)", inMinutes(jobTerminationThreshold(jobIdentifier))));
                } catch (Exception e) {
                    LOGGER.error(String.format("Failed to update console log with reason for cancelling hung job '%s'", jobIdentifier.buildLocator()), e);
                }
                this.jobLastActivityMap.remove(jobIdentifier);
                removeHungJobWarning(jobIdentifier);
                LOGGER.info(String.format("Cancelled hung job '%s' as it was hung for more than '%s' minutes", jobIdentifier.buildLocator(), inMinutes(difference)));
            } else if (difference > warningThreshold) {
                LOGGER.info(String.format("Job '%s' has not updated console log for more than '%s' minutes", jobIdentifier.buildLocator(), inMinutes(difference)));
                removeHungJobWarning(jobIdentifier);
                addJobHungWarning(jobIdentifier, difference);
            }
        }
    }

    private void addJobHungWarning(JobIdentifier jobIdentifier, long difference) {
        String namespacedJob = String.format("%s/%s/%s", jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName());
        serverHealthService.update(ServerHealthState.warningWithHtml(
                String.format("Job '%s' is not responding", namespacedJob),
                String.format("Job <a href='/go/tab/build/detail/%s'>%s</a> is currently running but has not shown any console activity in the last %s minute(s). This job may be hung.",
                        jobIdentifier.buildLocator(), namespacedJob, inMinutes(difference)),
                HealthStateType.general(HealthStateScope.forJob(jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName()))));
    }

    private String inMinutes(long difference) {
        return String.valueOf(difference / 1000 / 60);
    }

    private void removeHungJobWarning(JobIdentifier jobIdentifier) {
        serverHealthService.removeByScope(HealthStateScope.forJob(jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName()));
    }

    private boolean shouldCancelHungJob(JobIdentifier jobIdentifier, long difference) {
        return goConfigService.canCancelJobIfHung(jobIdentifier) && difference > jobTerminationThreshold(jobIdentifier);
    }

    private long jobTerminationThreshold(JobIdentifier jobIdentifier) {
        return goConfigService.getUnresponsiveJobTerminationThreshold(jobIdentifier);
    }

    static final class ActiveJobListener implements JobStatusListener {
        private final ConsoleActivityMonitor consoleActivityMonitor;

        private ActiveJobListener(ConsoleActivityMonitor consoleActivityMonitor) {
            this.consoleActivityMonitor = consoleActivityMonitor;
        }

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
