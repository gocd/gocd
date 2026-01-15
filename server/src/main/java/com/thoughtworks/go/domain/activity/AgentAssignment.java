/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentAssignment implements JobStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentAssignment.class);

    private final Map<String, JobInstance> jobsByAgentUuid = new ConcurrentHashMap<>();
    private final JobInstanceDao jobInstanceDao;

    @Autowired
    public AgentAssignment(JobInstanceDao jobInstanceDao) {
        this.jobInstanceDao = jobInstanceDao;
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        if (!job.isAssignedToAgent()) {
            LOGGER.debug("Ignoring job not yet assigned [{}]", job);
        } else if (job.getState().isInactiveOnAgent()) {
            jobsByAgentUuid.remove(job.getAgentUuid());
            LOGGER.debug("Agent assignment removed for job [{}]", job);
        } else if (job.getState().isActiveOnAgent()) {
            jobsByAgentUuid.put(job.getAgentUuid(), job);
            LOGGER.debug("Agent assignment added for job [{}]", job);
        }
    }

    public JobInstance latestActiveJobOnAgent(String agentUuid) {
        if (agentUuid == null) {
            return null;
        }
        return jobsByAgentUuid.computeIfAbsent(agentUuid, uuid -> {
            JobInstance job = jobInstanceDao.getLatestInProgressBuildByAgentUuid(uuid);
            LOGGER.debug("Getting AgentAssignment for agent with UUID [{}], got: {}", uuid, job);
            return job;
        });
    }

    @TestOnly
    public void clear() {
        jobsByAgentUuid.clear();
    }

    @TestOnly
    public int size() {
        return jobsByAgentUuid.size();
    }
}
