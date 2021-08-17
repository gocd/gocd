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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.matchers.UploadEntry;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.matchers.ConsoleOutMatcher.*;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
@EnableRuleMigrationSupport
public class BuildWorkArtifactUploadingTest {
    private static final String JOB_NAME = "one";
    private static final String STAGE_NAME = "first";
    private static final String PIPELINE_NAME = "cruise";
    private static final String AGENT_UUID = "uuid";
    private EnvironmentVariableContext environmentVariableContext;
    private SvnMaterial svnMaterial;
    private SvnTestRepo repo;

    private SystemEnvironment systemEnvironment = new SystemEnvironment();

    File buildWorkingDirectory;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    private PluginRequestProcessorRegistry pluginRequestProcessorRegistry;

    @BeforeEach
    public void setUp() throws IOException {
        buildWorkingDirectory = new File("tmp" + UUID.randomUUID());
        environmentVariableContext = new EnvironmentVariableContext();
        repo = new SvnTestRepo(temporaryFolder);
        SvnCommand command = new SvnCommand(null, repo.end2endRepositoryUrl());

        PipelineConfigMother.createPipelineConfig(PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        svnMaterial = new SvnMaterial(command);
        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");
    }

    @AfterEach
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        FileUtils.deleteQuietly(buildWorkingDirectory);
        new SystemEnvironment().clearProperty("serviceUrl");
    }

    @Test
    public void shouldUploadEachMatchedFile() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "**/*.png", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans, new String[]{"logs/pic/fail.png", "logs/pic/pass.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(), manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic/pass.png"), "mypic/logs/pic"));
        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic/fail.png"), "mypic/logs/pic"));
    }

    @Test
    public void shouldUploadMatchedFolder() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "**/*", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic/fail.png", "logs/pic/pass.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries, not(uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic/fail.png"), "mypic/logs/pic")));
        assertThat(entries, not(uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic/pass.png"), "mypic/logs/pic")));
        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic"), "mypic/logs"));
        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/README"), "mypic"));
    }

    @Test
    public void shouldNotUploadFileContainingFolderAgain() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "logs/pic/*.png", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic/fail.png", "logs/pic/pass.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic/pass.png"), "mypic"));
        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic/fail.png"), "mypic"));
        assertThat(entries, not(uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic"), "mypic")));
    }

    @Test
    public void shouldUploadFolderWhenMatchedWithWildCards() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "logs/pic-*", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries, not(uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-1/pass.png"), "mypic")));
        assertThat(entries, not(uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-1/fail.png"), "mypic")));
        assertThat(entries, not(uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-2/cancel.png"), "mypic")));
        assertThat(entries, not(uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-2/complete.png"), "mypic")));
        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-1"), "mypic"));
        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-2"), "mypic"));
    }

    @Test
    public void shouldUploadFolderWhenDirectMatch() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "logs/pic-1", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-1"), "mypic"));
    }

    @Test
    public void shouldUploadFolderWhenTrimedPathDirectMatch() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "logs/pic-1 ", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(),
                manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries, uploadFileToDestination(new File(buildWorkingDirectory.getPath() + "/logs/pic-1"), "mypic"));
    }

    @Test
    public void shouldFailBuildWhenNothingMatched() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "logs/picture", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();
        BuildRepositoryRemoteStub repository = new BuildRepositoryRemoteStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, repository, manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries.size(), is(0));
        assertThat(repository.states, containsResult(JobState.Building));
        assertThat(repository.states, containsResult(JobState.Completing));
        assertThat(repository.results, containsResult(JobResult.Failed));
        assertThat(manipulator.consoleOut(), printedRuleDoesNotMatchFailure(buildWorkingDirectory.getPath(), "logs/picture"));
    }

    @Test
    public void shouldFailBuildWhenSourceDirectoryDoesNotExist() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "not-Exist-Folder", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();
        BuildRepositoryRemoteStub repository = new BuildRepositoryRemoteStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, repository, manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries.size(), is(0));
        assertThat(repository.states, containsResult(JobState.Building));
        assertThat(repository.states, containsResult(JobState.Completing));
        assertThat(repository.results, containsResult(JobResult.Failed));
        assertThat(manipulator.consoleOut(), printedRuleDoesNotMatchFailure(buildWorkingDirectory.getPath(), "not-Exist-Folder"));
    }

    @Test
    public void shouldFailBuildWhenNothingMatchedUsingMatcherStartDotStart() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "target/pkg/*.*", "MYDEST"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans, new String[]{"target/pkg/"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub();
        BuildRepositoryRemoteStub repository = new BuildRepositoryRemoteStub();

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, repository, manipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();

        assertThat(entries.size(), is(0));
        assertThat(repository.states, containsResult(JobState.Building));
        assertThat(repository.states, containsResult(JobState.Completing));
        assertThat(repository.results, containsResult(JobResult.Failed));
        assertThat(manipulator.consoleOut(), printedRuleDoesNotMatchFailure(buildWorkingDirectory.getPath(), "target/pkg/*.*"));
    }


    @Test
    public void shouldReportUploadFailuresWhenTheyHappen() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "**/*.png", "mypic"));

        BuildAssignment buildAssigment = createAssignment(artifactPlans,
                new String[]{"logs/pic/pass.png", "logs/pic-1/pass.png"});

        BuildWork work = new BuildWork(buildAssigment, systemEnvironment.consoleLogCharset());
        GoArtifactsManipulatorStub manipulator = new GoArtifactsManipulatorStub(
                new ArrayList<>(), new HttpServiceStub(), new URLService(), new ZipUtilThatRunsOutOfMemory());

        AgentIdentifier agentIdentifier = new AgentIdentifier("somename", "127.0.0.1", AGENT_UUID);
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, new FakeBuildRepositoryRemote(), manipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        List<UploadEntry> entries = manipulator.uploadEntries();
        assertThat(entries.isEmpty(), is(true));
        assertThat(manipulator.consoleOut(), containsString("Failed to upload [**/*.png]"));
    }

    private class ZipUtilThatRunsOutOfMemory extends ZipUtil {
        @Override
        public File zip(File source, File destFile, int level) {
            throw new OutOfMemoryError("#2824");
        }
    }

    private BuildAssignment createAssignment(List<ArtifactPlan> artifactPlans, String[] fileToCreate) throws IOException {
        MaterialRevisions materialRevisions = materialRevisions();
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "");
        List<Builder> builders = new ArrayList<>();
        builders.add(new CreateFileBuilder(fileToCreate));
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), artifactPlans, -1, new JobIdentifier(PIPELINE_NAME, -2, "1", STAGE_NAME, "1", JOB_NAME), null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        return BuildAssignment.create(plan, buildCause, builders, buildWorkingDirectory, new EnvironmentVariableContext(), new ArtifactStores());
    }


    private MaterialRevisions materialRevisions() throws IOException {
        MaterialRevision svnRevision = new MaterialRevision(this.svnMaterial,
                ModificationsMother.oneModifiedFile(repo.end2ndRepositoryLatestRevision()));
        return new MaterialRevisions(svnRevision);
    }


    public class CreateFileBuilder extends Builder {
        private final String[] files;

        public CreateFileBuilder(String[] files) {
            super(new RunIfConfigs(), new NullBuilder(), "");
            this.files = files;
        }

        @Override
        public void build(DefaultGoPublisher publisher,
                          EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, String consoleLogCharset) throws CruiseControlException {
            try {
                FileUtil.createFilesByPath(buildWorkingDirectory, files);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
