/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.*;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.core.IsNot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildCauseProducerServiceConfigRepoIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired PipelineService pipelineService;
    @Autowired private ScheduleHelper scheduleHelper;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialDatabaseUpdater materialDatabaseUpdater;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MaterialUpdateService materialUpdateService;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;
    @Autowired private ConfigMaterialUpdater materialUpdater;
    @Autowired private GoRepoConfigDataSource goRepoConfigDataSource;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private MaterialConfigConverter materialConfigConverter;
    @Autowired private ConfigCache configCache;
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private PipelineScheduler buildCauseProducer;
    @Autowired private BuildCauseProducerService buildCauseProducerService;
    @Autowired private MaterialChecker materialChecker;
    @Autowired private MaterialExpansionService materialExpansionService;

    @Autowired private MaterialUpdateCompletedTopic topic;
    @Autowired private ConfigMaterialUpdateCompletedTopic configTopic;

    @Autowired private TransactionTemplate transactionTemplate;

    private GoDiskSpaceMonitor goDiskSpaceMonitor;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    private MagicalGoConfigXmlWriter xmlWriter;

    private  ConfigTestRepo configTestRepo;
    private DiskSpaceSimulator diskSpaceSimulator;
    private HgTestRepo hgRepo;
    private HgMaterialConfig materialConfig;
    private MDUPerformanceLogger logger;
    private MaterialUpdateListener worker;
    private HgMaterial material;
    private Pipeline latestPipeline;
    private PipelineConfig pipelineConfig;
    MaterialRevisions firstRevisions;
    private String PIPELINE_NAME;
    String fileName = "pipe1.gocd.xml";

    @Before
    public void setup() throws Exception {

        diskSpaceSimulator = new DiskSpaceSimulator();
        hgRepo = new HgTestRepo("testHgRepo");

        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();

        materialConfig = hgRepo.materialConfig();
        configHelper.addConfigRepo(new ConfigRepoConfig(materialConfig,"gocd-xml"));

        logger = mock(MDUPerformanceLogger.class);

        TestingEmailSender emailSender = new TestingEmailSender();
        SystemDiskSpaceChecker mockDiskSpaceChecker = Mockito.mock(SystemDiskSpaceChecker.class);
        StageService stageService = mock(StageService.class);
        ConfigDbStateRepository configDbStateRepository = mock(ConfigDbStateRepository.class);
        goDiskSpaceMonitor = new GoDiskSpaceMonitor(goConfigService, systemEnvironment,
                serverHealthService, emailSender, mockDiskSpaceChecker, mock(ArtifactsService.class),
                stageService, configDbStateRepository);
        goDiskSpaceMonitor.initialize();

        worker = new MaterialUpdateListener(configTopic,materialDatabaseUpdater,logger,goDiskSpaceMonitor);

        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        configTestRepo = new ConfigTestRepo(hgRepo, xmlWriter);
        this.material = (HgMaterial)materialConfigConverter.toMaterial(materialConfig);

        pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build", "test");
        pipelineConfig.materialConfigs().clear();
        pipelineConfig.materialConfigs().add(materialConfig);
        PIPELINE_NAME = CaseInsensitiveString.str(pipelineConfig.name());

        configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        pipelineConfig = goConfigService.pipelineConfigNamed(pipelineConfig.name());

        pipelineScheduleQueue.clear();

        //check test setup
        Materials materials = materialConfigConverter.toMaterials(pipelineConfig.materialConfigs());
        MaterialRevisions peggedRevisions = new MaterialRevisions();
        firstRevisions = materialChecker.findLatestRevisions(peggedRevisions, materials);
        assertThat(firstRevisions.isMissingModifications(),is(false));
    }

    @After
    public void teardown() throws Exception {
        diskSpaceSimulator.onTearDown();
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    private void waitForMaterialNotInProgress() throws InterruptedException {
        // time for messages to pass through all services

        int i = 0;
        while (materialUpdateService.isInProgress(material)) {
            Thread.sleep(100);
            if(i++ > 100)
                fail("material is hung - more than 10 seconds in progress");
        }
    }

    @Test
    public void shouldSchedulePipelineWhenManuallyTriggered() throws Exception {
        configTestRepo.addCodeToRepositoryAndPush("a.java", "added code file", "some java code");
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        BuildCause cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));
    }

    @Test
    public void shouldSchedulePipeline() throws Exception {
        configTestRepo.addCodeToRepositoryAndPush("a.java", "added code file","some java code");
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();

        buildCauseProducerService.autoSchedulePipeline(PIPELINE_NAME,new ServerHealthStateOperationResult(),123);
        assertThat(scheduleHelper.waitForAnyScheduled(5).keySet(), hasItem(PIPELINE_NAME));
    }

    @Test
    public void shouldNotSchedulePipelineWhenPartIsInvalid() throws Exception {
        configTestRepo.addCodeToRepositoryAndPush(fileName, "added broken config file","bad bad config");
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();

        assertThat(goRepoConfigDataSource.latestParseHasFailedForMaterial(material.config()),is(true));

        buildCauseProducerService.autoSchedulePipeline(PIPELINE_NAME, new ServerHealthStateOperationResult(), 123);
        scheduleHelper.waitForNotScheduled(5, PIPELINE_NAME);
    }

    @Test
    public void shouldSchedulePipelineWhenPartIsInvalid_AndManuallyTriggered() throws Exception {
        List<Modification> lastPush = configTestRepo.addCodeToRepositoryAndPush(fileName, "added broken config file", "bad bad config");
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();

        assertThat(goRepoConfigDataSource.latestParseHasFailedForMaterial(material.config()),is(true));

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        BuildCause cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));

        PipelineConfig pipelineConfigAfterSchedule = goConfigService.pipelineConfigNamed(pipelineConfig.name());
        RepoConfigOrigin configOriginAfterSchedule = (RepoConfigOrigin) pipelineConfigAfterSchedule.getOrigin();

        String lastValidPushedRevision = this.firstRevisions.latestRevision();
        assertThat("revisionOfPipelineConfigOriginShouldMatchLastValidPushedCommit",
                configOriginAfterSchedule.getRevision(),is(lastValidPushedRevision));
        assertThat("buildCauseRevisionShouldMatchLastPushedCommit",
                cause.getMaterialRevisions().latestRevision(), is(lastPush.get(0).getRevision()));
    }

    @Test
    public void shouldNotSchedulePipelineWhenConfigAndMaterialRevisionsMismatch() throws Exception {
        // we will use this worker to force material update without updating config
        MaterialUpdateListener byPassWorker = new MaterialUpdateListener(topic, materialDatabaseUpdater, logger, goDiskSpaceMonitor);
        List<Modification> mod = configTestRepo.addCodeToRepositoryAndPush("a.java", "added code file", "some java code");
        byPassWorker.onMessage(new MaterialUpdateMessage(material,123));
        //now db should have been updated, but config is still old
        RepoConfigOrigin configOrigin = (RepoConfigOrigin) goConfigService.pipelineConfigNamed(new CaseInsensitiveString(PIPELINE_NAME)).getOrigin();
        assertThat(configOrigin.getRevision(),is(firstRevisions.latestRevision()));

        buildCauseProducerService.autoSchedulePipeline(PIPELINE_NAME,new ServerHealthStateOperationResult(),123);
        scheduleHelper.waitForNotScheduled(5, PIPELINE_NAME);
    }

    @Test
    // unfortunately there is no way to know why revisions would mismatch during manual trigger.
    // We already let all manual triggers to bypass revision match check
    public void shouldSchedulePipelineWhenConfigAndMaterialRevisionsMismatch_AndManuallyTriggered() throws Exception {
        // we will use this worker to force material update without updating config
        MaterialUpdateListener byPassWorker = new MaterialUpdateListener(topic, materialDatabaseUpdater, logger, goDiskSpaceMonitor);
        List<Modification> lastPush = configTestRepo.addCodeToRepositoryAndPush("a.java", "added code file", "some java code");
        byPassWorker.onMessage(new MaterialUpdateMessage(material,123));
        //now db should have been updated, but config is still old
        RepoConfigOrigin configOrigin = (RepoConfigOrigin) goConfigService.pipelineConfigNamed(new CaseInsensitiveString(PIPELINE_NAME)).getOrigin();
        assertThat(configOrigin.getRevision(),is(firstRevisions.latestRevision()));

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        BuildCause cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));

        assertThat("buildCauseRevisionShouldMatchLastPushedCommit",
                cause.getMaterialRevisions().latestRevision(), is(lastPush.get(0).getRevision()));
    }


    @Test
    public void shouldReloadPipelineConfigurationWhenManuallyTriggered() throws Exception
    {
        // we change configuration of the pipeline by pushing new stage to config repo
        pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build", "test","newStage");
        pipelineConfig.materialConfigs().clear();
        pipelineConfig.materialConfigs().add(materialConfig);

        List<Modification> mod = configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        BuildCause cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));

        PipelineConfig pipelineConfigAfterSchedule = goConfigService.pipelineConfigNamed(pipelineConfig.name());
        RepoConfigOrigin configOriginAfterSchedule = (RepoConfigOrigin) pipelineConfigAfterSchedule.getOrigin();

        String lastPushedRevision = mod.get(0).getRevision();
        assertThat("revisionOfPipelineConfigOriginShouldMatchLastPushedCommit",
                configOriginAfterSchedule.getRevision(),is(lastPushedRevision));
        assertThat("buildCauseRevisionShouldMatchLastPushedCommit",
                cause.getMaterialRevisions().latestRevision(),is(lastPushedRevision));
    }

    @Test
    public void shouldNotScheduleWhenPipelineRemovedFromConfigRepoWhenManuallyTriggered() throws Exception
    {
        configTestRepo.addCodeToRepositoryAndPush(fileName, "removed pipeline from configuration",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"38\">\n"
                + "</cruise>");

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        waitForMaterialNotInProgress();
        // config is correct
        cachedGoConfig.throwExceptionIfExists();
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), IsNot.not(hasItem(PIPELINE_NAME)));
        assertThat(goConfigService.hasPipelineNamed(pipelineConfig.name()),is(false));
    }


    @Test
    public void shouldReloadPipelineConfigurationAndUpdateNewMaterialWhenManuallyTriggered() throws Exception
    {
        GitTestRepo otherGitRepo = new GitTestRepo();

        pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build", "test");
        pipelineConfig.materialConfigs().clear();
        materialConfig = hgRepo.createMaterialConfig("dest1");
        materialConfig.setAutoUpdate(true);
        pipelineConfig.materialConfigs().add(materialConfig);
        // new material is added
        GitMaterial gitMaterial = otherGitRepo.createMaterial("dest2");
        gitMaterial.setAutoUpdate(true);
        MaterialConfig otherMaterialConfig = gitMaterial.config();
        otherMaterialConfig.setAutoUpdate(true);
        pipelineConfig.materialConfigs().add(otherMaterialConfig);

        List<Modification> mod = configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        cachedGoConfig.throwExceptionIfExists();

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(20);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        BuildCause cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));

        PipelineConfig pipelineConfigAfterSchedule = goConfigService.pipelineConfigNamed(pipelineConfig.name());
        RepoConfigOrigin configOriginAfterSchedule = (RepoConfigOrigin) pipelineConfigAfterSchedule.getOrigin();

        String lastPushedRevision = mod.get(0).getRevision();
        assertThat("revisionOfPipelineConfigOriginShouldMatchLastPushedCommit",
                configOriginAfterSchedule.getRevision(),is(lastPushedRevision));
        assertThat(pipelineConfig.materialConfigs(), hasItem(otherMaterialConfig));
        assertThat("buildCauseRevisionShouldMatchLastPushedCommit",
                cause.getMaterialRevisions().latestRevision(),is(lastPushedRevision));

        // update of commited material happened during manual trigger
        MaterialRevisions modificationsInDb = materialRepository.findLatestModification(gitMaterial);
        assertThat(modificationsInDb.latestRevision(),is(otherGitRepo.latestModification().get(0).getRevision()));
    }


    @Test
    public void shouldSchedulePipelineRerunWithSpecifiedRevisions() throws Exception
    {
        List<Modification> firstBuildModifications = configTestRepo.addCodeToRepositoryAndPush("a.java", "added first code file", "some java code");
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();
        cachedGoConfig.throwExceptionIfExists();

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        cachedGoConfig.throwExceptionIfExists();

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        BuildCause cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));

        List<Modification> secondBuildModifications = configTestRepo.addCodeToRepositoryAndPush("a.java", "added second code file", "some java code");
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();

        pipelineScheduleQueue.clear();

        // revision will be older by 1 commit -
        // formally this is scm-config-consistency violation but we let this schedule because of manual trigger
        String explicitRevision = firstBuildModifications.get(0).getRevision();
        revisions.put(materialConfig.getPipelineUniqueFingerprint(), explicitRevision);
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, new Username(new CaseInsensitiveString("Admin")),
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        cachedGoConfig.throwExceptionIfExists();

        afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by Admin"));

        PipelineConfig pipelineConfigAfterSchedule = goConfigService.pipelineConfigNamed(pipelineConfig.name());
        RepoConfigOrigin configOriginAfterSchedule = (RepoConfigOrigin) pipelineConfigAfterSchedule.getOrigin();

        String lastPushedRevision = secondBuildModifications.get(0).getRevision();
        assertThat("revisionOfPipelineConfigOriginShouldMatchLastPushedCommit",
                configOriginAfterSchedule.getRevision(),is(lastPushedRevision));
        assertThat("buildCauseRevisionShouldMatchSpecifiedRevision",
                cause.getMaterialRevisions().latestRevision(),is(explicitRevision));
    }

    @Test
    public void shouldSchedulePipelineWithSameMaterialIn2DestinationsWhenManuallyTriggered_WithSpecifiedRevisions() throws Exception
    {
        pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build", "test");
        pipelineConfig.materialConfigs().clear();
        materialConfig = hgRepo.createMaterialConfig("dest1");
        materialConfig.setAutoUpdate(true);
        // new material is added
        MaterialConfig otherMaterialConfig = hgRepo.createMaterialConfig("dest2");
        otherMaterialConfig.setAutoUpdate(true);

        pipelineConfig.materialConfigs().add(materialConfig);
        pipelineConfig.materialConfigs().add(otherMaterialConfig);

        List<Modification> firstBuildModifications = configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();
        cachedGoConfig.throwExceptionIfExists();

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, Username.ANONYMOUS,
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        cachedGoConfig.throwExceptionIfExists();

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        BuildCause cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));

        List<Modification> secondBuildModifications = configTestRepo.addCodeToRepositoryAndPush("a.java", "added code file", "some java code");
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();

        pipelineScheduleQueue.clear();

        // revision in dest 1 will be older by 1 commit - this is kind of scm-config-consistency violation
        String explicitRevision = firstBuildModifications.get(0).getRevision();
        revisions.put(materialConfig.getPipelineUniqueFingerprint(), explicitRevision);
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, new Username(new CaseInsensitiveString("Admin")),
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        cachedGoConfig.throwExceptionIfExists();

        afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(PIPELINE_NAME));
        cause = afterLoad.get(PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by Admin"));

        PipelineConfig pipelineConfigAfterSchedule = goConfigService.pipelineConfigNamed(pipelineConfig.name());
        RepoConfigOrigin configOriginAfterSchedule = (RepoConfigOrigin) pipelineConfigAfterSchedule.getOrigin();

        String lastPushedRevision = secondBuildModifications.get(0).getRevision();
        assertThat("revisionOfPipelineConfigOriginShouldMatchLastPushedCommit",
                configOriginAfterSchedule.getRevision(),is(lastPushedRevision));
        assertThat(pipelineConfigAfterSchedule.materialConfigs(), hasItem(otherMaterialConfig));
        assertThat("buildCauseRevisionShouldMatchSpecifiedRevision",
                cause.getMaterialRevisions().latestRevision(),is(explicitRevision));
    }

}
