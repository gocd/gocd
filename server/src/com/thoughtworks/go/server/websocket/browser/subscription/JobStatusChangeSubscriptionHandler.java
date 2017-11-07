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

package com.thoughtworks.go.server.websocket.browser.subscription;

import com.google.gson.Gson;
import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.presentation.models.JobStatusJsonPresentationModel;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

@Component
public class JobStatusChangeSubscriptionHandler implements WebSocketSubscriptionHandler {

    private JobInstanceService jobInstanceService;
    private JobStatusCache cache;
    private final AgentService agentService;
    private final StageService stageService;
    private final HashMap<JobIdentifier, JobState> jobStates = new HashMap<JobIdentifier, JobState>();
    @Autowired
    public JobStatusChangeSubscriptionHandler(JobInstanceService jobInstanceService, JobStatusCache cache, AgentService agentService, StageService stageService) {
        this.jobInstanceService = jobInstanceService;
        this.cache = cache;
        this.agentService = agentService;
        this.stageService = stageService;
    }

    @Override
    public void start(SubscriptionMessage message, BrowserWebSocket socket) throws Exception {
        message.start(socket, this);
    }

    @Override
    public boolean isAuthorized(SubscriptionMessage message, SecurityService securityService, Username currentUser) {
        return message.isAuthorized(securityService, currentUser);
    }

    @Override
    public Class getType() {
        return JobStatusChange.class;
    }

    public void sendCurrentJobInstance(JobIdentifier jobIdentifier, BrowserWebSocket socket) throws IOException {
        sendJobInstance(getjobStatusJson(jobIdentifier), socket);
    }

    public JobInstance getjobInstance(JobIdentifier identifier) {
        JobConfigIdentifier jobConfigIdentifier = new JobConfigIdentifier(identifier.getPipelineName(), identifier.getStageName(), identifier.getBuildName());
        return cache.currentJob(jobConfigIdentifier);
    }


    public List getjobStatusJson(JobIdentifier identifier) {
        JobConfigIdentifier jobConfigIdentifier = new JobConfigIdentifier(identifier.getPipelineName(), identifier.getStageName(), identifier.getBuildName());
        JobInstance jobInstance = cache.currentJob(jobConfigIdentifier);
        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(jobInstance,
                agentService.findAgentObjectByUuid(jobInstance.getAgentUuid()),
                stageService.getBuildDuration(identifier.getPipelineName(), identifier.getStageName(), jobInstance));
        return createBuildInfo(presenter);
    }

    private List createBuildInfo(JobStatusJsonPresentationModel presenter) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("building_info", presenter.toJsonHash());
        List jsonList = new ArrayList();
        jsonList.add(info);
        return jsonList;
    }

    public void registerJobStateChangeListener(JobIdentifier jobIdentifier, BrowserWebSocket socket) {
        jobStates.put(jobIdentifier, JobState.Unknown);
        jobInstanceService.registerJobStateChangeListener(new JobStatusListener() {
            @Override
            public void jobStatusChanged(JobInstance job)  {
                if(!jobStates.get(jobIdentifier).getStatus().equals(job.getState())) {
                    try {
                        sendJobInstance(getjobStatusJson(jobIdentifier), socket);
                        jobStates.put(jobIdentifier, job.getState());
                        if(job.isCompleted()) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void sendJobInstance(List json, BrowserWebSocket socket) throws IOException {
        socket.send(ByteBuffer.wrap(new Gson().toJson(json).getBytes()));
    }
}
