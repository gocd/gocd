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

package com.thoughtworks.go.remote.work;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.config.ArtifactPropertiesGenerator;
import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.GoControlLog;
import com.thoughtworks.go.domain.materials.MaterialAgentFactory;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.work.DefaultGoPublisher;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.messageOf;
import static java.lang.String.format;

public class BuildWork implements Work {
    private static final Log LOGGER = LogFactory.getLog(BuildWork.class);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final BuildAssignment assignment;

    private DefaultGoPublisher goPublisher;

    private TimeProvider timeProvider = new TimeProvider();
    private JobPlan plan;
    private File workingDirectory;
    private MaterialRevisions materialRevisions;
    private GoControlLog buildLog;
    private Builders builders;

    public BuildWork(BuildAssignment assignment) {
        this.assignment = assignment;
    }

    private void initialize(BuildRepositoryRemote remoteBuildRepository,
                            GoArtifactsManipulator goArtifactsManipulator, AgentRuntimeInfo agentRuntimeInfo, TaskExtension taskExtension) {
        plan = assignment.getPlan();
        agentRuntimeInfo.busy(new AgentBuildingInfo(plan.getIdentifier().buildLocatorForDisplay(),
                plan.getIdentifier().buildLocator()));
        workingDirectory = assignment.getWorkingDirectory();
        materialRevisions = assignment.materialRevisions();
        buildLog = new GoControlLog(this.workingDirectory + "/cruise-output");
        goPublisher = new DefaultGoPublisher(goArtifactsManipulator, plan.getIdentifier(),
                remoteBuildRepository, agentRuntimeInfo);

        builders = new Builders(assignment.getBuilders(), goPublisher, buildLog, taskExtension);
    }

    public void doWork(AgentIdentifier agentIdentifier, BuildRepositoryRemote remoteBuildRepository, GoArtifactsManipulator goArtifactsManipulator,
                       EnvironmentVariableContext environmentVariableContext, AgentRuntimeInfo agentRuntimeInfo,
                       PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension, TaskExtension taskExtension) {
        initialize(remoteBuildRepository, goArtifactsManipulator, agentRuntimeInfo, taskExtension);
        environmentVariableContext.addAll(assignment.initialEnvironmentVariableContext());
        try {
            JobResult result = build(environmentVariableContext, agentIdentifier, packageAsRepositoryExtension, scmExtension);
            reportCompletion(result);
        } catch (InvalidAgentException e) {
            LOGGER.error("Agent UUID changed in the middle of the build.", e);
        } catch (Exception e) {
            reportFailure(e);
        } finally {
            goPublisher.stop();
        }
    }

    private void reportFailure(Exception e) {
        try{
            goPublisher.reportErrorMessage(messageOf(e), e);
        }
        catch (Exception reportException) {
            LOGGER.error(format("Unable to report error message - %s.", messageOf(e)), reportException);
        }
        reportCompletion(JobResult.Failed);
    }

    private void reportCompletion(JobResult result) {
        try {
            builders.waitForCancelTasks();
            if (result == null) {
                goPublisher.reportCurrentStatus(JobState.Completed);
                goPublisher.reportCompletedAction();
            } else {
                goPublisher.reportCompleted(result);
            }
        } catch (Exception ex) {
            LOGGER.error("New error occured during error handling:\n"
                    + "build will be rescheduled when agent starts asking for work again", ex);
        }
    }

    private JobResult build(EnvironmentVariableContext environmentVariableContext, AgentIdentifier agentIdentifier,
                            PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension) throws Exception {
        if (this.goPublisher.isIgnored()) {
            this.goPublisher.reportAction("Job is cancelled");
            return null;
        }

        goPublisher.consumeLineWithPrefix(format("Current Time: %s\n", dateFormat.format(timeProvider.currentTime())));

        prepareJob(agentIdentifier, packageAsRepositoryExtension, scmExtension);
        setupEnvrionmentContext(environmentVariableContext);
        plan.applyTo(environmentVariableContext);

        if (this.goPublisher.isIgnored()) {
            this.goPublisher.reportAction("Job is cancelled");
            return null;
        }

        JobResult result = buildJob(environmentVariableContext);
        completeJob(result);
        return result;
    }

    private void prepareJob(AgentIdentifier agentIdentifier, PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension) {
        goPublisher.reportAction("Start to prepare");
        goPublisher.reportCurrentStatus(JobState.Preparing);

        createWorkingDirectoryIfNotExist(workingDirectory);
        if (!plan.shouldFetchMaterials()) {
            goPublisher.consumeLineWithPrefix("Skipping material update since stage is configured not to fetch materials");
            return;
        }

        ProcessOutputStreamConsumer<GoPublisher, GoPublisher> consumer = new ProcessOutputStreamConsumer<GoPublisher, GoPublisher>(goPublisher, goPublisher);
        MaterialAgentFactory materialAgentFactory = new MaterialAgentFactory(consumer, workingDirectory, agentIdentifier, packageAsRepositoryExtension, scmExtension);

        materialRevisions.getMaterials().cleanUp(workingDirectory, consumer);

        goPublisher.consumeLineWithPrefix("Start to update materials.\n");

        for (MaterialRevision revision : materialRevisions.getRevisions()) {
            materialAgentFactory.createAgent(revision).prepare();
        }
    }

    private EnvironmentVariableContext setupEnvrionmentContext(EnvironmentVariableContext context) {
        context.setProperty("GO_SERVER_URL", new SystemEnvironment().getPropertyImpl("serviceUrl"), false);
        context.setProperty("GO_TRIGGER_USER", assignment.getBuildApprover() , false);
        plan.getIdentifier().populateEnvironmentVariables(context);
        materialRevisions.populateEnvironmentVariables(context, workingDirectory);
        return context;
    }

    private JobResult buildJob(EnvironmentVariableContext environmentVariableContext) {
        goPublisher.reportCurrentStatus(JobState.Building);
        goPublisher.reportAction("Start to build");
        return execute(environmentVariableContext);
    }

    private void completeJob(JobResult result) throws SocketTimeoutException {
        if (goPublisher.isIgnored()) {
            return;
        }

        goPublisher.consumeLineWithPrefix(format("Current job status: %s.\n", RunIfConfig.fromJobResult(result.toLowerCase())));

        goPublisher.reportCurrentStatus(JobState.Completing);
        goPublisher.reportAction("Start to create properties");
        harvestProperties(goPublisher);

        goPublisher.reportAction("Start to upload");
        plan.publishArtifacts(goPublisher, workingDirectory);
    }

    private JobResult execute(EnvironmentVariableContext environmentVariableContext) {
        Date now = new Date();

        // collect project information
        // TODO - #2409
        buildLog.addContent(new Element("info"));

        JobResult result = builders.build(environmentVariableContext);

        goPublisher.reportCompleting(result);

        try {
            buildLog.writeLogFile(now);
        } catch (IOException e) {
            throw bomb("Failed to write log file", e);
        }

        buildLog.reset();
        return result;
    }

    private List<ArtifactPropertiesGenerator> getArtifactPropertiesGenerators() {
        return plan.getPropertyGenerators();
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
        builders.cancel(environmentVariableContext);
    }

    // only for test
    public BuildAssignment getAssignment() {
        return assignment;
    }

    public JobIdentifier identifierForLogging() {
        if (assignment == null || assignment.getPlan() == null || assignment.getPlan().getIdentifier() == null) {
            return JobIdentifier.invalidIdentifier("Unknown", "Unknown", "Unknown", "Unknown", "Unknown");
        }
        return assignment.getPlan().getIdentifier();
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
        if (plan.shouldCleanWorkingDir() && buildWorkingDirectory.exists()) {
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
