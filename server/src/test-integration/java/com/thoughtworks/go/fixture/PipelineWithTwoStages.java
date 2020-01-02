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
package com.thoughtworks.go.fixture;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.svn.SubversionRevision;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static com.thoughtworks.go.utils.CommandUtils.exec;

public class PipelineWithTwoStages implements PreCondition {
    private SvnCommand svnClient;
    public String groupName = PipelineConfigs.DEFAULT_GROUP;
    public final String pipelineName;
    public final String devStage = "dev";
    public final String ftStage = "ft";

    protected DatabaseAccessHelper dbHelper;
    protected GoConfigFileHelper configHelper;
    private File workingFolder;
    public static final String JOB_FOR_DEV_STAGE = "foo";
    public static final String JOB_FOR_FT_STAGE = "bar";
    public String[] jobsOfDevStage = {JOB_FOR_DEV_STAGE};
    public static final String DEFAULT_MATERIAL = "defaultMaterial";
    protected MaterialRepository materialRepository;
    protected final TransactionTemplate transactionTemplate;
    public static final String DEV_STAGE_SECOND_JOB = "foo2";
    public static final String DEV_STAGE_THIRD_JOB = "foo3";
    private final TemporaryFolder temporaryFolder;

    public PipelineWithTwoStages(MaterialRepository materialRepository, TransactionTemplate transactionTemplate, TemporaryFolder temporaryFolder) {
        this.materialRepository = materialRepository;
        this.transactionTemplate = transactionTemplate;
        this.temporaryFolder = temporaryFolder;
        this.pipelineName = "pipeline_" + UUID.randomUUID();
    }

    public PipelineWithTwoStages usingConfigHelper(GoConfigFileHelper configHelper) {
        this.configHelper = configHelper;
        return this;
    }

    public PipelineWithTwoStages usingDbHelper(DatabaseAccessHelper dbHelper) {
        this.dbHelper = dbHelper;
        return this;
    }

    public PipelineWithTwoStages usingThreeJobs() {
        jobsOfDevStage = new String[]{JOB_FOR_DEV_STAGE, DEV_STAGE_SECOND_JOB, DEV_STAGE_THIRD_JOB};
        return this;
    }

    public PipelineWithTwoStages usingTwoJobs() {
        jobsOfDevStage = new String[]{JOB_FOR_DEV_STAGE, DEV_STAGE_SECOND_JOB};
        return this;
    }


    public Material getMaterial() {
        try {
            return new SvnMaterial(svnClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSetUp() throws Exception {
        configHelper.initializeConfigFile();

        addToSetup();
    }

    public void addToSetup() throws Exception {
        TestRepo svnTestRepo = new SvnTestRepo(temporaryFolder);
        svnClient = new SvnCommand(null, svnTestRepo.projectRepositoryUrl());


        MaterialConfigs materialConfigs = MaterialConfigsMother.mockMaterialConfigs(svnTestRepo.projectRepositoryUrl());
        SvnMaterialConfig svnMaterialConfig = (SvnMaterialConfig) materialConfigs.first();
        svnMaterialConfig.setName(new CaseInsensitiveString(DEFAULT_MATERIAL));
        svnMaterialConfig.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "default-folder"));
        configHelper.addPipelineWithGroup(groupName, pipelineName, materialConfigs, devStage, jobsOfDevStage);
        configHelper.addStageToPipeline(pipelineName, ftStage, JOB_FOR_FT_STAGE);
        configHelper.setPipelineLabelTemplate(pipelineName, "label-${COUNT}");
        dbHelper.onSetUp();
    }

    @Override
    public void onTearDown() throws Exception {
        configHelper.initializeConfigFile();
        dbHelper.onTearDown();
        if (workingFolder != null) {
            FileUtils.deleteQuietly(workingFolder);
        }
    }

    public Pipeline schedulePipeline() {
        return schedulePipeline(modifySomeFiles(pipelineConfig()));
    }

    public Pipeline schedulePipeline(final BuildCause buildCause) {
        return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                materialRepository.save(buildCause.getMaterialRevisions());
                return PipelineMother.schedule(pipelineConfig(), buildCause);
            }
        });
    }

    public PipelineConfig pipelineConfig() {
        return configHelper.currentConfig().pipelineConfigByName(new CaseInsensitiveString(pipelineName));
    }

    public StageConfig devStage() {
        return pipelineConfig().findBy(new CaseInsensitiveString(devStage));
    }

    public StageConfig ftStage() {
        return pipelineConfig().findBy(new CaseInsensitiveString(ftStage));
    }

    public Pipeline createPipelineWithFirstStageScheduled() {
        Pipeline mostRecent = dbHelper.getPipelineDao().mostRecentPipeline(pipelineName);
        bombIf(mostRecent.getStages().byName(devStage).isActive(),
                "Can not schedule new pipeline: the first stage is still running");

        Pipeline pipeline = schedulePipeline();
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

    public Pipeline createPipelineWithFirstStageAssigned() {
        return createPipelineWithFirstStageAssigned("uuid");
    }


    public Pipeline createPipelineWithFirstStageAssigned(String agentId) {
        Pipeline pipeline = createPipelineWithFirstStageScheduled();
        JobInstances instances = pipeline.getStages().byName(devStage).getJobInstances();
        for (JobInstance instance : instances) {
            dbHelper.assignToAgent(instance, agentId);
        }
        return dbHelper.getPipelineDao().loadPipeline(pipeline.getId());
    }

    public Pipeline createPipelineWithFirstStageScheduled(MaterialRevisions materialRevisions) {
        Pipeline pipeline = schedulePipeline(BuildCause.createWithModifications(materialRevisions, ""));
        dbHelper.save(pipeline);
        return pipeline;
    }

    public Pipeline createPipelineWithFirstStagePassedAndSecondStageRunning() {
        Pipeline pipeline = createPipelineWithFirstStageScheduled();
        dbHelper.passStage(pipeline.getFirstStage());
        dbHelper.scheduleStage(pipeline, pipelineConfig().findBy(new CaseInsensitiveString(ftStage)));
        return dbHelper.getPipelineDao().mostRecentPipeline(pipeline.getName());
    }

    public Pipeline createPipelineWithFirstStagePassedAndSecondStageHasNotStarted() {
        Pipeline pipeline = createPipelineWithFirstStageScheduled();
        dbHelper.passStage(pipeline.getFirstStage());
        return pipeline;
    }

    public Pipeline createPipelineWithFirstStageFailedAndSecondStageHasNotStarted() {
        Pipeline pipeline = createPipelineWithFirstStageScheduled();
        dbHelper.failStage(pipeline.getFirstStage());
        return pipeline;
    }

    public Pipeline createdPipelineWithAllStagesPassed() {
        return createdPipelineWithAllStagesCompleted(JobResult.Passed);
    }

    public Pipeline createdPipelineWithAllStagesCompleted(JobResult result) {
        Pipeline pipeline = createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        scheduleAndCompleteFollowingStages(pipeline, result);

        return latestPipelineWithIdentifiers();
    }

    private Pipeline latestPipelineWithIdentifiers() {
        Pipeline pipeline;
        pipeline = dbHelper.getPipelineDao().mostRecentPipeline(pipelineName);

        //TODO: #2318 - pipeline loaded from DB should contain identifiers
        for (Stage stage : pipeline.getStages()) {
            stage.setIdentifier(new StageIdentifier(pipeline, stage));
            for (JobInstance jobInstance : stage.getJobInstances()) {
                jobInstance.setIdentifier(new JobIdentifier(pipeline, stage, jobInstance));
            }
        }
        return pipeline;
    }

    protected void scheduleAndCompleteFollowingStages(Pipeline pipeline, JobResult result) {
        Stage ft = new InstanceFactory().createStageInstance(ftStage(), new DefaultSchedulingContext("anyone"), "md5-test", new TimeProvider());
        ft.setOrderId(pipeline.getFirstStage().getOrderId() + 1);
        dbHelper.getStageDao().saveWithJobs(pipeline, ft);
        dbHelper.completeStage(ft, result);
    }

    public void createNewCheckin() throws IOException {
        ensureWorkingCopyExist();
        String fileName = "readme" + UUID.randomUUID() + ".txt";
        File newFile = new File(workingFolder, fileName);

        try {
            newFile.createNewFile();
            exec("svn", "add", newFile.getAbsolutePath());
            exec("svn", "ci", "-m \"test\"", newFile.getAbsolutePath());
        } catch (IOException e) {
            bomb(e);
        }
    }

    private void ensureWorkingCopyExist() throws IOException {
        if (workingFolder == null || !workingFolder.exists()) {
            workingFolder = temporaryFolder.newFolder();
            svnClient.checkoutTo(inMemoryConsumer(), workingFolder, SubversionRevision.HEAD);
        }
    }

    public String pipelineLabel() {
        return dbHelper.getPipelineDao().mostRecentPipeline(pipelineName).getLabel();
    }

    public Integer pipelineCounter() {
        return dbHelper.getPipelineDao().mostRecentPipeline(pipelineName).getCounter();
    }

    public void createPipelineHistory() {
        createdPipelineWithAllStagesPassed();
    }

    public void configLabelTemplateUsingMaterialRevision() {
        configHelper.setPipelineLabelTemplate(pipelineName, String.format("label-${%s}", DEFAULT_MATERIAL));
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setRunOnAllAgentsForSecondStage() {
        configHelper.setRunOnAllAgents(pipelineName, ftStage, JOB_FOR_FT_STAGE, true);
    }

    public void addJobAgentMetadata(JobAgentMetadata jobAgentMetadata) {
        dbHelper.addJobAgentMetadata(jobAgentMetadata);
    }
}
