package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.mercurial.HgCommand;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

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

        material = (HgMaterial)materialConfigConverter.toMaterial(materialConfig);
    }



    @After
    public void teardown() throws Exception {
        diskSpaceSimulator.onTearDown();
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldParseEmptyRepository() throws Exception
    {
        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        Thread.sleep(1000);
        String revision = goRepoConfigDataSource.getRevisionAtLastAttempt(materialConfig);
        assertNotNull(revision);

        PartialConfig partial = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);
        assertThat(partial.getGroups().size(), is(0));
        assertThat(partial.getEnvironments().size(), is(0));
    }


    @Test
    public void shouldNotParseAgainWhenNoChangesInMaterial() throws Exception
    {
        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        Thread.sleep(1000);
        String revision = goRepoConfigDataSource.getRevisionAtLastAttempt(materialConfig);
        assertNotNull(revision);

        PartialConfig partial = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        Thread.sleep(1000);
        PartialConfig partial2 = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);
        assertSame(partial,partial2);
    }


    @Test
    public void shouldParseAgainWhenChangesInMaterial() throws Exception
    {
        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        Thread.sleep(1000);
        String revision = goRepoConfigDataSource.getRevisionAtLastAttempt(materialConfig);
        assertNotNull(revision);
        PartialConfig partial = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);

        hgRepo.commitAndPushFile("newFile.bla","could be config file");

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        Thread.sleep(1000);
        PartialConfig partial2 = goRepoConfigDataSource.latestPartialConfigForMaterial(materialConfig);
        assertNotSame(partial, partial2);
        assertThat("originsShouldDiffer",partial2.getOrigin(),is(not(partial.getOrigin())));
    }
}
