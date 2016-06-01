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

import java.util.ArrayList;
import static java.util.Collections.sort;
import java.util.List;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import com.thoughtworks.go.server.ui.JobInstanceModel;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobPresentationService {

    private final AgentService agentService;
    private final JobDurationStrategy jobDurationStrategy;
    private static Logger LOGGER = Logger.getLogger(JobPresentationService.class);


    @Autowired JobPresentationService(JobDurationStrategy jobDurationStrategy, AgentService agentService) {
        this.jobDurationStrategy = jobDurationStrategy;
        this.agentService = agentService;
    }

    public List<JobInstanceModel> jobInstanceModelFor(JobInstances jobInstances) {
        ArrayList<JobInstanceModel> models = new ArrayList<>();
        for (JobInstance jobInstance : jobInstances) {
            AgentInstance agentInstance = jobInstance.isAssignedToAgent() ? agentService.findAgentAndRefreshStatus(jobInstance.getAgentUuid()) : null;
            models.add(new JobInstanceModel(jobInstance, jobDurationStrategy, agentInstance));
        }
        sort(models, JobInstanceModel.JOB_MODEL_COMPARATOR);
        return models;
    }
}
