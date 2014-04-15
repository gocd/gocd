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

package com.thoughtworks.go.domain.activity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentAssignment implements JobStatusListener {
    private Map<String, JobInstance> map = Collections.synchronizedMap(new HashMap<String, JobInstance>());
    private final JobInstanceDao jobInstanceDao;
    private static final Logger LOGGER = Logger.getLogger(AgentAssignment.class);

    @Autowired
    public AgentAssignment(JobInstanceDao jobInstanceDao) {
        this.jobInstanceDao = jobInstanceDao;
    }

    public void jobStatusChanged(JobInstance job) {
        if (job.getState() == JobState.Rescheduled || job.getState() == JobState.Completed) {
            map.remove(job.getAgentUuid());
            LOGGER.debug(String.format("Removed agent assignment for job [%s]", job));
        }
        if (job.getState().isActiveOnAgent()) {
            map.put(job.getAgentUuid(), job);
            LOGGER.debug(String.format("Agent assignment added for job [%s]", job));
        }
    }

    public JobInstance latestActiveJobOnAgent(String agentUuid) {
        if (!map.containsKey(agentUuid)) {
            JobInstance job = jobInstanceDao.getLatestInProgressBuildByAgentUuid(agentUuid);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Getting AgentAssignment for agent with UUID [%s], got: %s", agentUuid, job));
            }
            map.put(agentUuid, job);
        }
        return map.get(agentUuid);
    }

    public void clear() {
        map.clear();
    }
}
