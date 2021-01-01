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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.service.ElasticAgentPluginService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

@Component
public class JobStatusListener implements GoMessageListener<JobStatusMessage> {
    private final JobStatusTopic jobStatusTopic;
    private JobInstanceService jobInstanceService;
    private StageService stageService;
    private final StageStatusTopic stageStatusTopic;
    private final ElasticAgentPluginService elasticAgentPluginService;
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;

    @Autowired
    public JobStatusListener(JobStatusTopic jobStatusTopic,
                             StageService stageService,
                             StageStatusTopic stageStatusTopic,
                             ElasticAgentPluginService elasticAgentPluginService,
                             JobInstanceSqlMapDao jobInstanceSqlMapDao,
                             JobInstanceService jobInstanceService) {
        this.jobStatusTopic = jobStatusTopic;
        this.stageService = stageService;
        this.stageStatusTopic = stageStatusTopic;
        this.elasticAgentPluginService = elasticAgentPluginService;
        this.jobInstanceSqlMapDao = jobInstanceSqlMapDao;
        this.jobInstanceService = jobInstanceService;
    }

    public void init() {
        jobStatusTopic.addListener(this);
    }

    @Override
    public void onMessage(final JobStatusMessage message) {
        if (message.getJobState().isCompleted()) {
            JobInstance jobInstance = jobInstanceService.buildByIdWithTransitions(message.getJobIdentifier().getBuildId());
            if (jobInstance.getState() == JobState.Rescheduled) {
                return;
            }

            final Stage stage = stageService.findStageWithIdentifier(message.getStageIdentifier());
            if (stage.isCompleted()) {
                //post a stage status change message to send email notification about stage completion
                stageStatusTopic.post(new StageStatusMessage(message.getStageIdentifier(), stage.stageState(), stage.getResult()));
            }
            JobInstance job = stage.findJob(message.getJobIdentifier().getBuildName());
            job.setPlan(jobInstanceSqlMapDao.loadPlan(job.getId()));
            job.setAgentUuid(ofNullable(job.getAgentUuid()).orElse(message.getAgentUuid()));

            //send job-completion message to elastic agent plugin
            elasticAgentPluginService.jobCompleted(job);

            //remove job plan related information from DB
            jobInstanceSqlMapDao.deleteJobPlanAssociatedEntities(job);
        }
    }
}
