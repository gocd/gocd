/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.service.JobInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobInstanceStatusMonitor {

    @Autowired
    public JobInstanceStatusMonitor(final AgentRemoteHandler agentRemoteHandler, JobInstanceService jobInstanceService) {
        jobInstanceService.registerJobStateChangeListener(new JobStatusListener() {
            @Override
            public void jobStatusChanged(JobInstance job) {
                if (job.isRescheduled() || JobResult.Cancelled.equals(job.getResult())) {
                    agentRemoteHandler.sendCancelMessage(job.getAgentUuid());
                }
            }
        });
    }
}
