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

package com.thoughtworks.go.remote;

import static java.lang.String.format;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.BuildRepositoryService;
import com.thoughtworks.go.server.service.AgentWithDuplicateUUIDException;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.JobStatusTopic;
import org.apache.log4j.Logger;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BuildRepositoryRemoteImpl {
    private static final Logger LOGGER = Logger.getLogger(BuildRepositoryRemoteImpl.class);

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
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(info + " ping received.");
        }
        try {
            agentService.updateRuntimeInfo(info);
            return new AgentInstruction(agentService.findAgentAndRefreshStatus(info.getUUId()).isCancelled());
        } catch (AgentWithDuplicateUUIDException agentException) {
            throw wrappedException(agentException);
        } catch (Exception e) {
            LOGGER.error("Error occurred in " + info + " ping.", e);
            throw wrappedException(e);
        }
    }

    public void reportCurrentStatus(final AgentRuntimeInfo agentRuntimeInfo, final JobIdentifier jobIdentifier, final JobState state) {
        handleFailuresDuringReporting(agentRuntimeInfo, jobIdentifier, "status", state.toString(), new ReportingAction() {
            @Override public void call() throws Exception {
                //TODO: may be i don't belong here, ping already updates agent runtime info
                agentService.updateRuntimeInfo(agentRuntimeInfo);
                buildRepositoryService.updateStatusFromAgent(jobIdentifier, state, agentRuntimeInfo.getUUId());
                jobStatusTopic.post(new JobStatusMessage(jobIdentifier, state, agentRuntimeInfo.getUUId()));
            }
        });
    }


    public void reportCompleting(final AgentRuntimeInfo agentRuntimeInfo, final JobIdentifier jobIdentifier, final JobResult result) {
        handleFailuresDuringReporting(agentRuntimeInfo, jobIdentifier, "result", result.toString(), new ReportingAction() {
            @Override public void call() throws Exception {
                //TODO: may be i don't belong here, ping already updates agent runtime info
                agentService.updateRuntimeInfo(agentRuntimeInfo);
                buildRepositoryService.completing(jobIdentifier, result, agentRuntimeInfo.getUUId());
            }
        });
    }

    public void reportCompleted(final AgentRuntimeInfo agentRuntimeInfo, final JobIdentifier jobIdentifier, final JobResult result) {
        final JobState state = JobState.Completed;

        handleFailuresDuringReporting(agentRuntimeInfo, jobIdentifier, "status and result", String.format("%s, %s",state, result), new ReportingAction() {
            @Override public void call() throws Exception {

                //TODO: may be i don't belong here, ping already updates agent runtime info
                agentService.updateRuntimeInfo(agentRuntimeInfo);

                buildRepositoryService.completing(jobIdentifier, result, agentRuntimeInfo.getUUId());

                buildRepositoryService.updateStatusFromAgent(jobIdentifier, state, agentRuntimeInfo.getUUId());
                jobStatusTopic.post(new JobStatusMessage(jobIdentifier, state, agentRuntimeInfo.getUUId()));
            }
        });
    }

    private void handleFailuresDuringReporting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, final String thingBeingReported, String changedValue, ReportingAction action) {
        String agentDebugString = agentRuntimeInfo.agentInfoDebugString();
        LOGGER.info(format("[%s] is reporting %s [%s] for [%s]", agentDebugString, thingBeingReported, changedValue, jobIdentifier.toFullString()));
        try {
            action.call();
        } catch (AgentWithDuplicateUUIDException agentException) {
            throw wrappedException(agentException);
        } catch (Exception e) {
            LOGGER.error(format("Exception occurred when [%s] tries to report " + thingBeingReported + " [%s] for [%s]",
                    agentDebugString, changedValue, jobIdentifier.toFullString()), e);
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
            LOGGER.info(format("[Agent Cookie] Agent [%s] at location [%s] asked for a new cookie, assigned [%s]", identifier, location, cookie));
            return cookie;
        } catch (Exception e) {
            LOGGER.error(String.format("[Agent Cookie] Agent [%s] at location [%s] could not get a cookie.", identifier, location), e);
            throw wrappedException(e);
        }
    }

    private interface ReportingAction {
        void call() throws Exception;
    }
}
