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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class AgentAssignment implements JobStatusListener {
    private Map<String, JobInstance> map = Collections.synchronizedMap(new HashMap<>());
    private final JobInstanceDao jobInstanceDao;
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentAssignment.class);

    @Autowired
    public AgentAssignment(JobInstanceDao jobInstanceDao) {
        this.jobInstanceDao = jobInstanceDao;
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        if (job.getState() == JobState.Rescheduled || job.getState() == JobState.Completed) {
            map.remove(job.getAgentUuid());
            LOGGER.debug("Removed agent assignment for job [{}]", job);
        }
        if (job.getState().isActiveOnAgent()) {
            map.put(job.getAgentUuid(), job);
            LOGGER.debug("Agent assignment added for job [{}]", job);
        }
    }

    public JobInstance latestActiveJobOnAgent(String agentUuid) {
        if (!map.containsKey(agentUuid)) {
            JobInstance job = jobInstanceDao.getLatestInProgressBuildByAgentUuid(agentUuid);
            LOGGER.debug("Getting AgentAssignment for agent with UUID [{}], got: {}", agentUuid, job);
            map.put(agentUuid, job);
        }
        return map.get(agentUuid);
    }

    public void clear() {
        map.clear();
    }
}
