/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.server.domain.Agent;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import com.thoughtworks.go.server.ui.JobInstanceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.sort;

@Service
public class JobPresentationService {

    private final AgentService agentService;
    private final JobDurationStrategy jobDurationStrategy;
    private static Logger LOGGER = LoggerFactory.getLogger(JobPresentationService.class);


    @Autowired
    JobPresentationService(JobDurationStrategy jobDurationStrategy, AgentService agentService) {
        this.jobDurationStrategy = jobDurationStrategy;
        this.agentService = agentService;
    }

    public List<JobInstanceModel> jobInstanceModelFor(JobInstances jobInstances) {
        ArrayList<JobInstanceModel> models = new ArrayList<>();
        for (JobInstance jobInstance : jobInstances) {
            AgentInstance agentInstance = jobInstance.isAssignedToAgent() ? agentService.findAgentAndRefreshStatus(jobInstance.getAgentUuid()) : null;
            JobInstanceModel model;
            if (null != agentInstance && !agentInstance.isNullAgent()) {
                model = new JobInstanceModel(jobInstance, jobDurationStrategy, agentInstance);
            } else if (jobInstance.getAgentUuid() != null) {
                Agent agent = agentService.findAgentObjectByUuid(jobInstance.getAgentUuid());
                model = new JobInstanceModel(jobInstance, jobDurationStrategy, agent);
            } else {
                model = new JobInstanceModel(jobInstance, jobDurationStrategy);
            }
            models.add(model);
        }
        sort(models, JobInstanceModel.JOB_MODEL_COMPARATOR);
        return models;
    }
}
