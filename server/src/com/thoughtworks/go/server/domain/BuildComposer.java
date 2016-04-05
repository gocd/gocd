/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPropertiesGenerator;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobState.*;

public class BuildComposer {
    private BuildAssignment assignment;

    public BuildComposer(BuildAssignment assignment) {
        this.assignment = assignment;
    }

    public BuildCommand compose() {
        return BuildCommand.compose(
                echoWithPrefix("Job Started: ${date}"),
                prepare(),
                build(),
                reportAction("Job completed").runIf("any"))
                .setOnCancel(BuildCommand.compose(
                        reportAction("Job is canceled"),
                        reportAction("Job completed")));

    }

    private BuildCommand prepare() {
        return BuildCommand.compose(
                reportAction("Start to prepare"),
                reportCurrentStatus(Preparing),
                refreshWorkingDir(),
                updateMaterials());
    }

    private BuildCommand build() {
        return BuildCommand.compose(
                reportAction("Start to build"),
                reportCurrentStatus(Building),
                setupSecrets(),
                setupEnvironmentVariables(),
                runBuilders(),
                BuildCommand.compose(
                        reportCompleting(),
                        reportCurrentStatus(Completing),
                        harvestProperties(),
                        uploadArtifacts()).setRunIfRecurisvely("any"));
    }


    private BuildCommand harvestProperties() {
        List<ArtifactPropertiesGenerator> generators = assignment.getPlan().getPropertyGenerators();
        List<BuildCommand> commands = new ArrayList<>();

        for (ArtifactPropertiesGenerator generator : generators) {
            BuildCommand command = BuildCommand.generateProperty(generator.getName(), generator.getSrc(), generator.getXpath()).setWorkingDirectory(workingDirectory());
            commands.add(command);
        }
        return BuildCommand.compose(
                reportAction("Start to create properties"),
                BuildCommand.compose(commands));
    }


    private BuildCommand runBuilders() {
        List<BuildCommand> commands = new ArrayList<>();
        for (Builder builder : assignment.getBuilders()) {
            commands.add(runSingleBuilder(builder));
        }
        return BuildCommand.compose(commands);
    }

    private BuildCommand runSingleBuilder(Builder builder) {
        String runIfConfig = builder.resolvedRunIfConfig().toString();
        return BuildCommand.compose(
                echoWithPrefix("Current job status: passed."),
                echoWithPrefix("Current job status: failed.").runIf("failed"),
                echoWithPrefix("Start to execute task: %s.", builder.getDescription()).runIf(runIfConfig),
                builder.buildCommand()
                        .runIf(runIfConfig)
                        .setOnCancel(runCancelTask(builder.getCancelBuilder()))).runIf(runIfConfig);
    }

    private BuildCommand runCancelTask(Builder cancelBuilder) {
        if (cancelBuilder == null) {
            return null;
        }
        return BuildCommand.compose(
                echoWithPrefix("Start to execute cancel task: %s", cancelBuilder.getDescription()),
                cancelBuilder.buildCommand(),
                echoWithPrefix("Task is canceled"));
    }

    private BuildCommand uploadArtifacts() {
        List<BuildCommand> commands = new ArrayList<>();
        for (ArtifactPlan ap : assignment.getPlan().getArtifactPlans()) {
            commands.add(uploadArtifact(ap.getSrc(), ap.getDest(), ap.getArtifactType().isTest())
                    .setWorkingDirectory(workingDirectory()));
        }

        return BuildCommand.compose(
                reportAction("Start to upload"),
                BuildCommand.compose(commands),
                generateTestReport());
    }

    private BuildCommand generateTestReport() {
        List<String> srcs = new ArrayList<>();
        for (ArtifactPlan ap : assignment.getPlan().getArtifactPlans()) {
            if (ap.getArtifactType() == ArtifactType.unit) {
                srcs.add(ap.getSrc());
            }
        }
        return srcs.isEmpty() ? noop() : BuildCommand.generateTestReport(srcs, "testoutput").setWorkingDirectory(workingDirectory());
    }


    private BuildCommand setupSecrets() {
        List<EnvironmentVariableContext.EnvironmentVariable> secrets = environmentVariableContext().getSecureEnvironmentVariables();
        ArrayList<BuildCommand> commands = new ArrayList<>();
        for (EnvironmentVariableContext.EnvironmentVariable secret : secrets) {
            commands.add(secret(secret.value()));
        }
        return BuildCommand.compose(commands);
    }

    private BuildCommand setupEnvironmentVariables() {
        EnvironmentVariableContext context = environmentVariableContext();
        ArrayList<BuildCommand> commands = new ArrayList<>();
        commands.add(export("GO_SERVER_URL"));
        for (String property : context.getPropertyKeys()) {
            commands.add(export(property, context.getProperty(property), context.isPropertySecure(property)));
        }
        return BuildCommand.compose(commands);
    }

    private BuildCommand reportAction(String action) {
        return echoWithPrefix("%s %s on ${agent.hostname} [${agent.location}]", action, getJobIdentifier().buildLocatorForDisplay());
    }

    private BuildCommand updateMaterials() {
        if (!assignment.getPlan().shouldFetchMaterials()) {
            return echoWithPrefix("Skipping material update since stage is configured not to fetch materials");
        }

        MaterialRevisions materialRevisions = assignment.materialRevisions();
        Materials materials = materialRevisions.getMaterials();
        return BuildCommand.compose(
                materials.cleanUpCommand(workingDirectory()),
                echoWithPrefix("Start to update materials \n"),
                materialRevisions.updateToCommand(workingDirectory()));
    }

    private BuildCommand refreshWorkingDir() {
        return BuildCommand.compose(
                cleanWorkingDir(),
                mkdirs(workingDirectory()).setTest(test("-nd", workingDirectory())));
    }

    private BuildCommand cleanWorkingDir() {
        if (!assignment.getPlan().shouldCleanWorkingDir()) {
            return noop();
        }
        return BuildCommand.compose(
                cleandir(workingDirectory()),
                echoWithPrefix("Cleaning working directory \"$%s\" since stage is configured to clean working directory", workingDirectory())
        ).setTest(test("-d", workingDirectory()));
    }

    private String workingDirectory() {
        return assignment.getWorkingDirectory().getPath();
    }

    private JobIdentifier getJobIdentifier() {
        return assignment.getPlan().getIdentifier();
    }

    private EnvironmentVariableContext environmentVariableContext() {
        JobPlan plan = assignment.getPlan();
        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.addAll(assignment.initialEnvironmentVariableContext());
        context.setProperty("GO_TRIGGER_USER", assignment.getBuildApprover() , false);
        getJobIdentifier().populateEnvironmentVariables(context);
        assignment.materialRevisions().populateEnvironmentVariables(context, new File(workingDirectory()));
        plan.applyTo(context);
        return context;
    }
}
