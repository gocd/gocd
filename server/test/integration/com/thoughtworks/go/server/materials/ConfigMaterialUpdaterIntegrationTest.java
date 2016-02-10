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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ConfigMaterialUpdaterIntegrationTest {
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

    @Autowired private MaterialUpdateCompletedTopic topic;
    @Autowired private ConfigMaterialUpdateCompletedTopic configTopic;

    @Autowired private TransactionTemplate transactionTemplate;

    private MDUPerformanceLogger logger;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    public DiskSpaceSimulator diskSpaceSimulator;

    private MaterialConfig materialConfig;

    private MaterialUpdateListener worker;
    private HgTestRepo hgRepo;
    private HgMaterial material;
    private MagicalGoConfigXmlWriter xmlWriter;

    private  ConfigTestRepo configTestRepo;

    @Before
    public void setup() throws Exception {
        diskSpaceSimulator = new DiskSpaceSimulator();
        hgRepo = new HgTestRepo("testHgRepo");

        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();

        materialConfig = new HgMaterialConfig(hgRepo.projectRepositoryUrl(),"testHgRepo");
        configHelper.addConfigRepo(new ConfigRepoConfig(materialConfig,"gocd-xml"));

        logger = mock(MDUPerformanceLogger.class);

        TestingEmailSender emailSender = new TestingEmailSender();
        SystemDiskSpaceChecker mockDiskSpaceChecker = Mockito.mock(SystemDiskSpaceChecker.class);
        StageService stageService = mock(StageService.class);
        ConfigDbStateRepository configDbStateRepository = mock(ConfigDbStateRepository.class);
        GoDiskSpaceMonitor goDiskSpaceMonitor = new GoDiskSpaceMonitor(goConfigService, systemEnvironment,
                serverHealthService, emailSender, mockDiskSpaceChecker, mock(ArtifactsService.class),
                stageService, configDbStateRepository);
        goDiskSpaceMonitor.initialize();

        worker = new MaterialUpdateListener(configTopic,materialDatabaseUpdater,logger,goDiskSpaceMonitor);

        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        configTestRepo = new ConfigTestRepo(hgRepo, xmlWriter);
        this.material = configTestRepo.getMaterial();
    }



    @After
    public void teardown() throws Exception {
        diskSpaceSimulator.onTearDown();
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldBeInProgressUntilParsedWhenValid() throws Exception
    {
        materialUpdateService.updateMaterial(material);

        // because first result of parsing is left in repo data source
        // and then message is posted to take material from 'in progress' state

        assertInProgressState();
    }
    @Test
    public void shouldBeInProgressUntilParsedWhenInvalid() throws Exception
    {
        configTestRepo.addCodeToRepositoryAndPush("bogus.gocd.xml", "added bad config file",
                "<?xml ve\"?>\n"
                + "<cru>\n"
                + "</cruise>");

        materialUpdateService.updateMaterial(material);

        // because first result of parsing is left in repo data source
        // and then message is posted to take material from 'in progress' state

        assertInProgressState();
    }

    private void assertInProgressState() throws InterruptedException {
        int i = 0;
        while (goRepoConfigDataSource.getRevisionAtLastAttempt(materialConfig) == null) {
            if(!materialUpdateService.isInProgress(material))
                Assert.fail("should be still in progress");

            Thread.sleep(1);
            if(i++ > 10000)
                fail("material is hung - more than 10 seconds in progress");
        }
    }

    @Test
    public void shouldParseEmptyRepository() throws Exception
    {
        materialUpdateService.updateMaterial(material);
        waitForMaterialNotInProgress();

        String revision = goRepoConfigDataSource.getRevisionAtLastAttempt(materialConfig);
        assertNotNull(revision);

        PartialConfig partial = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);
        assertThat(partial.getGroups().size(), is(0));
        assertThat(partial.getEnvironments().size(), is(0));
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
    public void shouldNotParseAgainWhenNoChangesInMaterial() throws Exception
    {
        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();
        String revision = goRepoConfigDataSource.getRevisionAtLastAttempt(materialConfig);
        assertNotNull(revision);

        PartialConfig partial = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();
        PartialConfig partial2 = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);
        assertSame(partial,partial2);
    }


    @Test
    public void shouldParseAgainWhenChangesInMaterial() throws Exception
    {
        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();
        String revision = goRepoConfigDataSource.getRevisionAtLastAttempt(materialConfig);
        assertNotNull(revision);
        PartialConfig partial = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);

        hgRepo.commitAndPushFile("newFile.bla","could be config file");

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();
        PartialConfig partial2 = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);
        assertNotSame(partial, partial2);
        assertThat("originsShouldDiffer", partial2.getOrigin(), is(not(partial.getOrigin())));
    }

    @Test
    public void shouldParseAndLoadValidPartialConfig() throws Exception
    {
        String fileName = "pipe1.gocd.xml";

        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipelineConfig = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();
        PartialConfig partial = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);
        assertNotNull(partial);
        assertThat(partial.getGroups().get(0).size(),is(1));
        assertThat(partial.getGroups().get(0).get(0), is(pipelineConfig));
    }

    @Test
    public void shouldMergePipelineFromValidConfigRepository() throws Exception
    {
        String fileName = "pipe1.gocd.xml";

        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipelineConfig = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);

        materialUpdateService.updateMaterial(material);
        Assert.assertThat(materialUpdateService.isInProgress(material),is(true));
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        cachedGoConfig.forceReload();

        assertThat(goConfigService.hasPipelineNamed(pipelineConfig.name()), is(true));
        assertThat(goConfigService.pipelineConfigNamed(pipelineConfig.name()), is(pipelineConfig));
    }

    @Test
    public void shouldCheckoutNewMaterial() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipelineConfig = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        configTestRepo.addPipelineToRepositoryAndPush("pipe1.gocd.xml", pipelineConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        File flyweightDir = materialRepository.folderFor(material);
        Assert.assertThat(flyweightDir.exists(),is(true));
        Assert.assertThat(new File(flyweightDir,"pipe1.gocd.xml").exists(),is(true));
    }


    @Test
    public void shouldCheckoutChangedInExistingMaterial() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipelineConfig = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        configTestRepo.addPipelineToRepositoryAndPush("pipe1.gocd.xml", pipelineConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        configTestRepo.addPipelineToRepositoryAndPush("pipe2.gocd.xml", pipelineConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        File flyweightDir = materialRepository.folderFor(material);
        Assert.assertThat(flyweightDir.exists(),is(true));
        Assert.assertThat(new File(flyweightDir,"pipe1.gocd.xml").exists(),is(true));
        Assert.assertThat("shouldContainFilesAddedLater",new File(flyweightDir,"pipe2.gocd.xml").exists(),is(true));
    }

    @Test
    public void shouldNotMergeFromInvalidConfigRepository_AndShouldKeepLastValidPart() throws Exception
    {
        String fileName = "pipe1.gocd.xml";

        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipelineConfig = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        cachedGoConfig.forceReload();

        assertThat(goConfigService.hasPipelineNamed(pipelineConfig.name()), is(true));
        assertThat(goConfigService.pipelineConfigNamed(pipelineConfig.name()),is(pipelineConfig));

        configTestRepo.addCodeToRepositoryAndPush("badPipe.gocd.xml", "added bad config file", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"38\">\n"
                + "<pipelines group=\"changed\">\n"
                + "  <pipeline name=\"badPipe\">\n"
                + "    <materials>\n"
                + "      <svn url=\"file:///tmp/foo\" />\n"
                + "      <svn url=\"file:///tmp/foo\" />\n"
                + "    </materials>\n"
                + "  </pipeline>\n"
                + "</pipelines>"
                + "</cruise>");
        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        cachedGoConfig.forceReload();
        // but we still have the old part
        assertThat(goConfigService.hasPipelineNamed(pipelineConfig.name()), is(true));
        assertThat(goConfigService.pipelineConfigNamed(pipelineConfig.name()), is(pipelineConfig));
        // and no trace of badPipe
        assertThat(goConfigService.hasPipelineNamed(new CaseInsensitiveString("badPipe")), is(false));
    }

}
