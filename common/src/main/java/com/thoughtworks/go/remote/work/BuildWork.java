/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.agent.plugin.consolelog.ConsoleLogRequestProcessor;
import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.MaterialAgentFactory;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.artifact.ArtifactsPublisher;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.util.command.*;
import com.thoughtworks.go.work.DefaultGoPublisher;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.agent.plugin.consolelog.ConsoleLogRequest.*;
import static com.thoughtworks.go.domain.JobState.*;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.messageOf;
import static java.lang.String.format;

public class BuildWork implements Work {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildWork.class);
    private final BuildAssignment assignment;
    private final String consoleLogCharset;

    private transient Builders builders;
    private transient File workingDirectory;
    private transient TimeProvider timeProvider;
    private transient DefaultGoPublisher goPublisher;
    private transient MaterialRevisions materialRevisions;
    private transient ArtifactsPublisher artifactsPublisher;
    private transient MaterialAgentFactory materialAgentFactory;
    private transient PluginRequestProcessorRegistry pluginRequestProcessorRegistry;

    public BuildWork(BuildAssignment assignment, String consoleLogCharset) {
        this.assignment = assignment;
        this.consoleLogCharset = consoleLogCharset;
    }

    private void initialize(AgentWorkContext agentWorkContext) {
        final JobIdentifier jobIdentifier = assignment.getJobIdentifier();
        final AgentIdentifier agentIdentifier = agentWorkContext.getAgentIdentifier();
        final AgentRuntimeInfo agentRuntimeInfo = agentWorkContext.getAgentRuntimeInfo();
        pluginRequestProcessorRegistry = agentWorkContext.getPluginRequestProcessorRegistry();

        agentRuntimeInfo.busy(new AgentBuildingInfo(jobIdentifier.buildLocatorForDisplay(), jobIdentifier.buildLocator()));
        this.timeProvider = new TimeProvider();
        this.workingDirectory = assignment.getWorkingDirectory();
        this.materialRevisions = assignment.materialRevisions();
        this.goPublisher = new DefaultGoPublisher(agentWorkContext.getArtifactsManipulator(), jobIdentifier, agentWorkContext.getRepositoryRemote(), agentRuntimeInfo, consoleLogCharset);
        this.artifactsPublisher = new ArtifactsPublisher(goPublisher, agentWorkContext.getArtifactExtension(), assignment.getArtifactStores(), workingDirectory);
        this.builders = new Builders(assignment.getBuilders(), goPublisher, agentWorkContext.getTaskExtension(), agentWorkContext.getArtifactExtension());

        this.materialAgentFactory = new MaterialAgentFactory(workingDirectory, agentIdentifier, agentWorkContext.getPackageRepositoryExtension(), agentWorkContext.getScmExtension());
    }

    public void doWork(EnvironmentVariableContext environmentVariableContext, AgentWorkContext agentWorkContext) {
        initialize(agentWorkContext);
        try {
            registerConsoleLogProcessors();
            JobResult result = build(environmentVariableContext);
            reportCompletion(result);
        } catch (InvalidAgentException e) {
            LOGGER.error("Agent UUID changed in the middle of the build.", e);
        } catch (Exception e) {
            reportFailure(e);
        } finally {
            removeConsoleLogProcessors();
            goPublisher.stop();
        }
    }

    private void registerConsoleLogProcessors() {
        LOGGER.debug("Adding console log processors.");
        final ConsoleLogRequestProcessor consoleLogRequestProcessor = new ConsoleLogRequestProcessor(goPublisher);
        pluginRequestProcessorRegistry.registerProcessorFor(ARTIFACT_PLUGIN_CONSOLE_LOG.requestName(), consoleLogRequestProcessor);
        pluginRequestProcessorRegistry.registerProcessorFor(SCM_PLUGIN_CONSOLE_LOG.requestName(), consoleLogRequestProcessor);
        pluginRequestProcessorRegistry.registerProcessorFor(TASK_PLUGIN_CONSOLE_LOG.requestName(), consoleLogRequestProcessor);
    }

    private void removeConsoleLogProcessors() {
        LOGGER.debug("Removing console log processors.");
        pluginRequestProcessorRegistry.removeProcessorFor(ARTIFACT_PLUGIN_CONSOLE_LOG.requestName());
        pluginRequestProcessorRegistry.removeProcessorFor(SCM_PLUGIN_CONSOLE_LOG.requestName());
        pluginRequestProcessorRegistry.removeProcessorFor(TASK_PLUGIN_CONSOLE_LOG.requestName());
    }

    private void reportFailure(Exception e) {
        try {
            goPublisher.reportErrorMessage(messageOf(e), e);
        } catch (Exception reportException) {
            LOGGER.error("Unable to report error message - %s.", messageOf(e), reportException);
        }
        reportCompletion(JobResult.Failed);
    }

    private void reportCompletion(JobResult result) {
        try {
            builders.waitForCancelTasks();
            if (result == null) {
                goPublisher.reportCurrentStatus(Completed);
                goPublisher.reportCompletedAction();
            } else {
                goPublisher.reportCompleted(result);
            }
        } catch (Exception ex) {
            LOGGER.error("New error occurred during error handling:\n"
                    + "build will be rescheduled when agent starts asking for work again", ex);
        }
    }

    private JobResult build(EnvironmentVariableContext environmentVariableContext) {
        if (this.goPublisher.isIgnored()) {
            this.goPublisher.reportAction("Job is cancelled");
            return null;
        }

        goPublisher.consumeLineWithPrefix(format("Job Started: %s\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(timeProvider.currentTime())));

        prepareJob();

        setupEnvironmentContext(environmentVariableContext);

        dumpEnvironmentVariables(environmentVariableContext);

        if (this.goPublisher.isIgnored()) {
            this.goPublisher.reportAction("Job is cancelled");
            return null;
        }

        return completeJob(buildJob(environmentVariableContext, consoleLogCharset), environmentVariableContext);
    }

    private void dumpEnvironmentVariables(EnvironmentVariableContext environmentVariableContext) {
        Set<String> processLevelEnvVariables = ProcessManager.getInstance().environmentVariableNames();
        List<String> report = environmentVariableContext.report(processLevelEnvVariables);
        ConsoleOutputStreamConsumer safeOutput = new LabeledOutputStreamConsumer(DefaultGoPublisher.PREP, DefaultGoPublisher.PREP_ERR, safeOutputStreamConsumer(environmentVariableContext));
        for (int i = 0; i < report.size(); i++) {
            String line = report.get(i);
            safeOutput.stdOutput((i == report.size() - 1) ? line + "\n" : line);
        }
    }

    private SafeOutputStreamConsumer safeOutputStreamConsumer(EnvironmentVariableContext environmentVariableContext) {
        SafeOutputStreamConsumer consumer = new SafeOutputStreamConsumer(processOutputStreamConsumer());
        consumer.addSecrets(environmentVariableContext.secrets());
        return consumer;
    }

    private void prepareJob() {
        goPublisher.reportAction(DefaultGoPublisher.PREP, "Start to prepare");
        goPublisher.reportCurrentStatus(Preparing);

        createWorkingDirectoryIfNotExist(workingDirectory);
        if (!assignment.shouldFetchMaterials()) {
            goPublisher.taggedConsumeLineWithPrefix(DefaultGoPublisher.PREP, "Skipping material update since stage is configured not to fetch materials");
            return;
        }

        ConsoleOutputStreamConsumer consumer = new LabeledOutputStreamConsumer(DefaultGoPublisher.PREP, DefaultGoPublisher.PREP_ERR, processOutputStreamConsumer());

        materialRevisions.getMaterials().cleanUp(workingDirectory, consumer);

        goPublisher.taggedConsumeLineWithPrefix(DefaultGoPublisher.PREP, "Start to update materials.\n");

        for (MaterialRevision revision : materialRevisions.getRevisions()) {
            materialAgentFactory.createAgent(revision, consumer).prepare();
        }
    }

    private ProcessOutputStreamConsumer<GoPublisher, GoPublisher> processOutputStreamConsumer() {
        return new ProcessOutputStreamConsumer<>(goPublisher, goPublisher);
    }

    private void setupEnvironmentContext(EnvironmentVariableContext context) {
        context.setProperty("GO_SERVER_URL", new SystemEnvironment().getPropertyImpl("serviceUrl"), false);
        context.addAll(assignment.initialEnvironmentVariableContext());
        materialRevisions.populateAgentSideEnvironmentVariables(context, workingDirectory);
    }

    private JobResult buildJob(EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
        goPublisher.reportCurrentStatus(Building);
        goPublisher.reportAction("Start to build");
        return execute(environmentVariableContext, consoleLogCharset);
    }

    private JobResult completeJob(JobResult result, EnvironmentVariableContext environmentVariableContext) {
        if (goPublisher.isIgnored()) {
            return result;
        }

        String tag = result.isPassed() ? DefaultGoPublisher.JOB_PASS : DefaultGoPublisher.JOB_FAIL;
        goPublisher.taggedConsumeLineWithPrefix(tag, format("Current job status: %s", RunIfConfig.fromJobResult(result.toLowerCase())));

        goPublisher.reportCurrentStatus(Completing);
        goPublisher.reportAction("Start to create properties");
        harvestProperties(goPublisher);

        goPublisher.reportAction(DefaultGoPublisher.PUBLISH, "Start to upload");

        try {
            artifactsPublisher.publishArtifacts(assignment.getArtifactPlans(), environmentVariableContext);
        } catch (Exception e) {
            LOGGER.error(null, e);
            goPublisher.taggedConsumeLineWithPrefix(DefaultGoPublisher.PUBLISH_ERR, e.getMessage());
            return JobResult.Failed;
        }

        return result;
    }

    private JobResult execute(EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
        JobResult result = builders.build(environmentVariableContext, consoleLogCharset);
        goPublisher.reportCompleting(result);
        return result;
    }

    private List<ArtifactPropertiesGenerator> getArtifactPropertiesGenerators() {
        return assignment.getPropertyGenerators();
    }


    private void harvestProperties(DefaultGoPublisher publisher) {
        List<ArtifactPropertiesGenerator> generators = getArtifactPropertiesGenerators();
        for (ArtifactPropertiesGenerator generator : generators) {
            generator.generate(publisher, workingDirectory);
        }
    }

    public String description() {
        return "Running build ...";
    }

    public void cancel(EnvironmentVariableContext environmentVariableContext, AgentRuntimeInfo agentruntimeInfo) {
        agentruntimeInfo.cancel();
        builders.cancel(environmentVariableContext, consoleLogCharset);
    }

    public BuildAssignment getAssignment() {
        return assignment;
    }

    public JobIdentifier identifierForLogging() {
        if (assignment == null || assignment.getJobIdentifier() == null) {
            return JobIdentifier.invalidIdentifier("Unknown", "Unknown", "Unknown", "Unknown", "Unknown");
        }
        return assignment.getJobIdentifier();
    }

    public String toString() {
        return "BuildWork["
                + assignment.toString()
                + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BuildWork work = (BuildWork) o;

        if (assignment != null ? !assignment.equals(work.assignment) : work.assignment != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (assignment != null ? assignment.hashCode() : 0);
        result = 31 * result + (goPublisher != null ? goPublisher.hashCode() : 0);
        result = 31 * result + (timeProvider != null ? timeProvider.hashCode() : 0);
        return result;
    }

    private void createWorkingDirectoryIfNotExist(File buildWorkingDirectory) {
        if (assignment.shouldCleanWorkingDir() && buildWorkingDirectory.exists()) {
            try {
                FileUtils.cleanDirectory(buildWorkingDirectory);
                goPublisher.consumeLineWithPrefix("Cleaning working directory \"" + buildWorkingDirectory.getAbsolutePath() + "\" since stage is configured to clean working directory");
            } catch (IOException e) {
                bomb("Clean working directory is set to true. Unable to clean working directory for agent: " + buildWorkingDirectory.getAbsolutePath() + ", with error: " + e.getMessage());
            }
        }
        if (!buildWorkingDirectory.exists()) {
            if (!buildWorkingDirectory.mkdirs()) {
                bomb("Unable to create working directory for agent: " + buildWorkingDirectory.getAbsolutePath());
            }
        }
    }
}
