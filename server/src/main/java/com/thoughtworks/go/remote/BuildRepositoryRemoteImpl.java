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
package com.thoughtworks.go.remote;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.JobStatusTopic;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.AgentWithDuplicateUUIDException;
import com.thoughtworks.go.server.service.BuildRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.stereotype.Component;

@Component
public class BuildRepositoryRemoteImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildRepositoryRemoteImpl.class);

    private AgentService agentService;
    private BuildRepositoryService buildRepositoryService;
    private JobStatusTopic jobStatusTopic;

    @Autowired
    BuildRepositoryRemoteImpl(BuildRepositoryService buildRepositoryService, AgentService agentService, JobStatusTopic jobStatusTopic) {
        this.buildRepositoryService = buildRepositoryService;
        this.agentService = agentService;
        this.jobStatusTopic = jobStatusTopic;
    }

    public AgentInstruction ping(AgentRuntimeInfo info) {
        LOGGER.trace("{} ping received.", info);
        try {
            agentService.updateRuntimeInfo(info);
            AgentInstance agentInstance = agentService.findAgentAndRefreshStatus(info.getUUId());

            return agentInstance.agentInstruction();
        } catch (AgentWithDuplicateUUIDException agentException) {
            throw wrappedException(agentException);
        } catch (Exception e) {
            LOGGER.error("Error occurred in {} ping.", info, e);
            throw wrappedException(e);
        }
    }

    public void reportCurrentStatus(final AgentRuntimeInfo agentRuntimeInfo, final JobIdentifier jobIdentifier, final JobState state) {
        handleFailuresDuringReporting(agentRuntimeInfo, jobIdentifier, "status", state.toString(), () -> {
            //TODO: may be i don't belong here, ping already updates agent runtime info
            agentService.updateRuntimeInfo(agentRuntimeInfo);
            buildRepositoryService.updateStatusFromAgent(jobIdentifier, state, agentRuntimeInfo.getUUId());
            jobStatusTopic.post(new JobStatusMessage(jobIdentifier, state, agentRuntimeInfo.getUUId()));
        });
    }


    public void reportCompleting(final AgentRuntimeInfo agentRuntimeInfo, final JobIdentifier jobIdentifier, final JobResult result) {
        handleFailuresDuringReporting(agentRuntimeInfo, jobIdentifier, "result", result.toString(), () -> {
            //TODO: may be i don't belong here, ping already updates agent runtime info
            agentService.updateRuntimeInfo(agentRuntimeInfo);
            buildRepositoryService.completing(jobIdentifier, result, agentRuntimeInfo.getUUId());
        });
    }

    public void reportCompleted(final AgentRuntimeInfo agentRuntimeInfo, final JobIdentifier jobIdentifier, final JobResult result) {
        final JobState state = JobState.Completed;

        handleFailuresDuringReporting(agentRuntimeInfo, jobIdentifier, "status and result", String.format("%s, %s", state, result), () -> {

            //TODO: may be i don't belong here, ping already updates agent runtime info
            agentService.updateRuntimeInfo(agentRuntimeInfo);

            buildRepositoryService.completing(jobIdentifier, result, agentRuntimeInfo.getUUId());

            buildRepositoryService.updateStatusFromAgent(jobIdentifier, state, agentRuntimeInfo.getUUId());
            jobStatusTopic.post(new JobStatusMessage(jobIdentifier, state, agentRuntimeInfo.getUUId()));
        });
    }

    private void handleFailuresDuringReporting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, final String thingBeingReported, String changedValue, ReportingAction action) {
        String agentDebugString = agentRuntimeInfo.agentInfoDebugString();
        LOGGER.info("[{}] is reporting {} [{}] for [{}]", agentDebugString, thingBeingReported, changedValue, jobIdentifier.toFullString());
        try {
            action.call();
        } catch (AgentWithDuplicateUUIDException agentException) {
            throw wrappedException(agentException);
        } catch (Exception e) {
            LOGGER.error("Exception occurred when [{}] tries to report {} [{}] for [{}]", agentDebugString, thingBeingReported, changedValue, jobIdentifier.toFullString(), e);
            throw wrappedException(e);
        }
    }

    public boolean isIgnored(JobIdentifier jobIdentifier) {
        try {
            return buildRepositoryService.isCancelledOrRescheduled(jobIdentifier.getBuildId());
        } catch (Exception e) {
            throw wrappedException(e);
        }
    }

    private RemoteAccessException wrappedException(Exception e) {
        return new RemoteAccessException(e.getMessage(), e);
    }

    public String getCookie(AgentIdentifier identifier, String location) {
        try {
            String cookie = agentService.assignCookie(identifier);
            LOGGER.info("[Agent Cookie] Agent [{}] at location [{}] asked for a new cookie, assigned [{}]", identifier, location, cookie);
            return cookie;
        } catch (Exception e) {
            LOGGER.error("[Agent Cookie] Agent [{}] at location [{}] could not get a cookie.", identifier, location, e);
            throw wrappedException(e);
        }
    }

    private interface ReportingAction {
        void call() throws Exception;
    }
}
