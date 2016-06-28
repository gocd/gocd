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

package com.thoughtworks.go.work;

import java.io.File;

import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.ConsoleOutputTransmitter;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.String.format;

public class DefaultGoPublisher implements GoPublisher {
    private GoArtifactsManipulator manipulator;
    private JobIdentifier jobIdentifier;
    private AgentIdentifier agentIdentifier;
    private BuildRepositoryRemote remoteBuildRepository;
    private final AgentRuntimeInfo agentRuntimeInfo;
    private ConsoleOutputTransmitter consoleOutputTransmitter;
    private static final Log LOG = LogFactory.getLog(DefaultGoPublisher.class);
    private String currentWorkingDirectory = SystemUtil.currentWorkingDirectory();

    public DefaultGoPublisher(GoArtifactsManipulator manipulator, JobIdentifier jobIdentifier,
                              BuildRepositoryRemote remoteBuildRepository,
                              AgentRuntimeInfo agentRuntimeInfo) {
        this.manipulator = manipulator;
        this.jobIdentifier = jobIdentifier;
        this.agentIdentifier = agentRuntimeInfo.getIdentifier();
        this.remoteBuildRepository = remoteBuildRepository;
        this.agentRuntimeInfo = agentRuntimeInfo;
        init();
    }

    //do not put the logic into the constructor it is really hard to stub.
    protected void init() {
        consoleOutputTransmitter = manipulator.createConsoleOutputTransmitter(jobIdentifier, agentIdentifier);
    }

    @Override
    public void setProperty(Property property) {
        manipulator.setProperty(jobIdentifier, property);
    }

    @Override
    public void upload(File fileToUpload, String destPath) {
        manipulator.publish(this, destPath, fileToUpload, jobIdentifier);
    }

    public void fetch(FetchArtifactBuilder fetchArtifact) {
        manipulator.fetch(this, fetchArtifact);
    }

    @Override
    public void consumeLine(String line) {
        consoleOutputTransmitter.consumeLine(line);
    }

    public void flushToServer() {
        consoleOutputTransmitter.flushToServer();
    }

    public void stop() {
        LOG.info("Stopping Transmission for " + jobIdentifier.toFullString());
        consoleOutputTransmitter.stop();
    }

    public void reportCurrentStatus(JobState state) {
        LOG.info(format("%s is reporting status [%s] to Go Server for %s", agentIdentifier, state,
                jobIdentifier.toFullString()));
        remoteBuildRepository.reportCurrentStatus(agentRuntimeInfo, jobIdentifier, state);
    }

    public void reportCompleting(JobResult result) {
        LOG.info(String.format("%s is reporting build result [%s] to Go Server for %s", agentIdentifier, result,
                jobIdentifier.toFullString()));
        remoteBuildRepository.reportCompleting(agentRuntimeInfo, jobIdentifier, result);
    }

    public void reportCompleted(JobResult result) {
        LOG.info(String.format("%s is reporting build result [%s] to Go Server for %s", agentIdentifier, result,
                jobIdentifier.toFullString()));
        remoteBuildRepository.reportCompleted(agentRuntimeInfo, jobIdentifier, result);
        reportCompletedAction();
    }

    public void reportCompletedAction() {
        reportAction("Job completed");
    }

    public boolean isIgnored() {
        return remoteBuildRepository.isIgnored(jobIdentifier);
    }

    public void reportAction(String action) {
        String message = String.format("[%s] %s %s on %s [%s]", GoConstants.PRODUCT_NAME, action, jobIdentifier.buildLocatorForDisplay(),
                agentIdentifier.getHostName(), currentWorkingDirectory);
        if (LOG.isDebugEnabled()) {
            LOG.debug(message);
        }
        consumeLine(message);
    }

    @Override
    public void consumeLineWithPrefix(String message) {
        consumeLine(String.format("[%s] %s", GoConstants.PRODUCT_NAME, message));
    }

    @Override
    public void reportErrorMessage(String message, Exception e) {
        LOG.error(message, e);
        consumeLine(message);
    }
}
