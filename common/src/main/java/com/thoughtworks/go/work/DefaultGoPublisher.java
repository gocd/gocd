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
package com.thoughtworks.go.work;

import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.ConsoleOutputTransmitter;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.thoughtworks.go.domain.JobState.*;
import static java.lang.String.format;

public class DefaultGoPublisher implements GoPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGoPublisher.class);
    private final AgentRuntimeInfo agentRuntimeInfo;
    private final String consoleLogCharset;
    private GoArtifactsManipulator manipulator;
    private JobIdentifier jobIdentifier;
    private AgentIdentifier agentIdentifier;
    private BuildRepositoryRemote remoteBuildRepository;
    private ConsoleOutputTransmitter consoleOutputTransmitter;
    private String currentWorkingDirectory = SystemUtil.currentWorkingDirectory();

    public DefaultGoPublisher(GoArtifactsManipulator manipulator, JobIdentifier jobIdentifier,
                              BuildRepositoryRemote remoteBuildRepository,
                              AgentRuntimeInfo agentRuntimeInfo, String consoleLogCharset) {
        this.manipulator = manipulator;
        this.jobIdentifier = jobIdentifier;
        this.agentIdentifier = agentRuntimeInfo.getIdentifier();
        this.remoteBuildRepository = remoteBuildRepository;
        this.agentRuntimeInfo = agentRuntimeInfo;
        this.consoleLogCharset = consoleLogCharset;
        init();
    }

    //do not put the logic into the constructor it is really hard to stub.
    protected void init() {
        consoleOutputTransmitter = manipulator.createConsoleOutputTransmitter(jobIdentifier, agentIdentifier, consoleLogCharset);
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
        taggedConsumeLine(null, line);
    }

    @Override
    public void stop() {
        LOG.info("Stopping Transmission for {}", jobIdentifier.toFullString());
        consoleOutputTransmitter.stop();
    }

    private void reportCurrentStatus(JobState state) {
        consoleOutputTransmitter.flushToServer();
        LOG.info("{} is reporting status [{}] to Go Server for {}", agentIdentifier, state, jobIdentifier.toFullString());
        remoteBuildRepository.reportCurrentStatus(agentRuntimeInfo, jobIdentifier, state);
    }

    public void reportCompleting(JobResult result) {
        consoleOutputTransmitter.flushToServer();
        LOG.info("{} is reporting build result [{}] to Go Server for {}", agentIdentifier, result, jobIdentifier.toFullString());
        remoteBuildRepository.reportCompleting(agentRuntimeInfo, jobIdentifier, result);
    }

    public void reportCompleted(JobResult result) {
        if (result != null) {
            LOG.info("{} is reporting build result [{}] to Go Server for {}", agentIdentifier, result, jobIdentifier.toFullString());
            reportCompletedAction();
            consoleOutputTransmitter.flushToServer();
            remoteBuildRepository.reportCompleted(agentRuntimeInfo, jobIdentifier, result);
        } else {
            reportCompletedAction();
            reportCurrentStatus(Completed);
        }
    }

    private void reportCompletedAction() {
        reportAction(COMPLETED, "Job completed");
    }

    public boolean isIgnored() {
        return remoteBuildRepository.isIgnored(agentRuntimeInfo, jobIdentifier);
    }

    private void reportAction(String tag, String action) {
        String message = String.format("[%s] %s %s on %s [%s]", GoConstants.PRODUCT_NAME, action, jobIdentifier.buildLocatorForDisplay(),
                agentIdentifier.getHostName(), currentWorkingDirectory);
        LOG.debug(message);
        taggedConsumeLine(tag, message);
    }

    @Override
    public void consumeLineWithPrefix(String message) {
        taggedConsumeLineWithPrefix(NOTICE, message);
    }

    @Override
    public void taggedConsumeLineWithPrefix(String tag, String message) {
        taggedConsumeLine(tag, String.format("[%s] %s", GoConstants.PRODUCT_NAME, message));
    }

    @Override
    public void reportErrorMessage(String message, Exception e) {
        LOG.error(message, e);
        taggedConsumeLine(ERR, message);
    }

    @Override
    public void taggedConsumeLine(String tag, String line) {
        consoleOutputTransmitter.taggedConsumeLine(tag, line);
    }

    public void reportPreparing() {
        reportAction(PREP, "Start to prepare");
        reportCurrentStatus(Preparing);
    }

    public void reportStartingToBuild() {
        reportAction(NOTICE, "Start to build");
        reportCurrentStatus(Building);
    }

    public void reportCompleting(JobResult result, String tag) {
        taggedConsumeLineWithPrefix(tag, format("Current job status: %s", RunIfConfig.fromJobResult(result.toLowerCase())));
        reportCurrentStatus(Completing);
    }

    public void reportJobCancelled() {
        reportAction(NOTICE, "Job is cancelled");
        consoleOutputTransmitter.flushToServer();
    }

    public void reportBeginToPublishArtifacts() {
        reportAction(PUBLISH, "Start to upload");
    }

    public void reportCreatingProperties() {
        reportAction(NOTICE, "Start to create properties");
    }
}
