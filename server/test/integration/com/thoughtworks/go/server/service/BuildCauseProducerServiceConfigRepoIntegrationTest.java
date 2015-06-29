package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
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
import org.hamcrest.CoreMatchers;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalMatchers.not;
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
    @Autowired private MergedGoConfig mergedGoConfig;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private PipelineScheduler buildCauseProducer;
    @Autowired private BuildCauseProducerService buildCauseProducerService;
    @Autowired private MaterialChecker materialChecker;
    @Autowired private MaterialExpansionService materialExpansionService;

    @Autowired private MaterialUpdateCompletedTopic topic;
    @Autowired private ConfigMaterialUpdateCompletedTopic configTopic;

    @Autowired private TransactionTemplate transactionTemplate;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    private MetricsProbeService metricsProbeService;
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
    private String PIPELINE_NAME;

    @Before
    public void setup() throws Exception {
        metricsProbeService = mock(MetricsProbeService.class);
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
        GoDiskSpaceMonitor goDiskSpaceMonitor = new GoDiskSpaceMonitor(goConfigService, systemEnvironment,
                serverHealthService, emailSender, mockDiskSpaceChecker, mock(ArtifactsService.class),
                stageService, configDbStateRepository);
        goDiskSpaceMonitor.initialize();

        worker = new MaterialUpdateListener(configTopic,materialDatabaseUpdater,logger,goDiskSpaceMonitor);

        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        configTestRepo = new ConfigTestRepo(hgRepo, xmlWriter);
        this.material = (HgMaterial)materialConfigConverter.toMaterial(materialConfig);

        String fileName = "pipe1.gocd.xml";

        pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages("pipe1", "build", "test");
        pipelineConfig.materialConfigs().clear();
        pipelineConfig.materialConfigs().add(materialConfig);
        PIPELINE_NAME = pipelineConfig.name().toString();

        configTestRepo.addPipelineToRepositoryAndPush(fileName, pipelineConfig);

        materialUpdateService.updateMaterial(material);
        // time for messages to pass through all services
        waitForMaterialNotInProgress();

        pipelineConfig = goConfigService.pipelineConfigNamed(pipelineConfig.name());

        pipelineScheduleQueue.clear();

        //check test setup
        Materials materials = materialConfigConverter.toMaterials(pipelineConfig.materialConfigs());
        MaterialRevisions peggedRevisions = new MaterialRevisions();
        MaterialRevisions revisions = materialChecker.findLatestRevisions(peggedRevisions, materials);
        assertThat(revisions.isMissingModifications(),is(false));
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

}
