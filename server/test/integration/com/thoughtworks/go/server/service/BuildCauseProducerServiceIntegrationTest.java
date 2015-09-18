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

package com.thoughtworks.go.server.service;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialDatabaseUpdater;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildCauseProducerServiceIntegrationTest {
    private static final String STAGE_NAME = "dev";

    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private PipelineScheduler buildCauseProducer;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired PipelineService pipelineService;
    @Autowired private ScheduleHelper scheduleHelper;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialDatabaseUpdater materialDatabaseUpdater;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;
    @Autowired private PipelinePauseService pipelinePauseService;
    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private BuildCauseProducerService service;

    @Autowired private TransactionTemplate transactionTemplate;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    public Subversion repository;
    public static SvnTestRepo svnRepository;
    private Pipeline latestPipeline;

    private static final String MINGLE_PIPELINE_NAME = "mingle";
    private static final String GO_PIPELINE_NAME = "go";
    private static final String GO_PIPELINE_UPSTREAM = "go-parent";
    public DiskSpaceSimulator diskSpaceSimulator;
    private PipelineConfig goPipelineConfig;
    private PipelineConfig goParentPipelineConfig;
    private MaterialRevisions svnMaterialRevs;
    private PipelineConfig mingleConfig;

    @Before
    public void setup() throws Exception {
        diskSpaceSimulator = new DiskSpaceSimulator();
        new HgTestRepo("testHgRepo");

        svnRepository = new SvnTestRepo("testSvnRepo");

        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();

        repository = new SvnCommand(null, svnRepository.projectRepositoryUrl());

        goParentPipelineConfig = configHelper.addPipeline(GO_PIPELINE_UPSTREAM, STAGE_NAME, new MaterialConfigs(new GitMaterialConfig("foo-bar")), "unit");

        goPipelineConfig = configHelper.addPipeline(GO_PIPELINE_NAME, STAGE_NAME, repository, "unit");

        svnMaterialRevs = new MaterialRevisions();
        SvnMaterial svnMaterial = SvnMaterial.createSvnMaterialWithMock(repository);
        svnMaterialRevs.addRevision(svnMaterial, svnMaterial.latestModification(null, new ServerSubprocessExecutionContext(goConfigService)));

        final MaterialRevisions materialRevisions = new MaterialRevisions();
        SvnMaterial anotherSvnMaterial = SvnMaterial.createSvnMaterialWithMock(repository);
        materialRevisions.addRevision(anotherSvnMaterial, anotherSvnMaterial.latestModification(null, subprocessExecutionContext));

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(svnMaterialRevs);
            }
        });

        BuildCause buildCause = BuildCause.createWithModifications(svnMaterialRevs, "");

        mingleConfig = configHelper.addPipeline(MINGLE_PIPELINE_NAME, STAGE_NAME, repository, new Filter(new IgnoredFiles("**/*.doc")), "unit", "functional");
        latestPipeline = PipelineMother.schedule(this.mingleConfig, buildCause);
        latestPipeline = pipelineDao.saveWithStages(latestPipeline);
        dbHelper.passStage(latestPipeline.getStages().first());
        pipelineScheduleQueue.clear();
    }

    @After
    public void teardown() throws Exception {
        diskSpaceSimulator.onTearDown();
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    @Test public void manualSchedulePipeline_canProduceShouldNotgetIntoCyclicLoopWithTriggerMonitor() throws Exception {
        OperationResult operationResult = new ServerHealthStateOperationResult();
        buildCauseProducer.manualProduceBuildCauseAndSave(MINGLE_PIPELINE_NAME, Username.ANONYMOUS, new ScheduleOptions(), operationResult);
        scheduleHelper.waitForAnyScheduled(5);
        assertThat(operationResult.canContinue(),is(true));
    }


    @Test
    public void shouldNotSchedulePipelineIfTheChangesAreIgnored() throws Exception {
        String ignoredFile = "a.doc";
        svnRepository.checkInOneFile(ignoredFile);
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), not(hasItem(MINGLE_PIPELINE_NAME)));
    }

    @Test
    public void shouldSchedulePipeline() throws Exception {
        checkinFile(SvnMaterial.createSvnMaterialWithMock(repository), "a.java", svnRepository);
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(MINGLE_PIPELINE_NAME));
    }

    @Test
    public void should_NOT_schedulePipeline_whenOneOfTheMaterialsHasNoModificationsPresent() throws Exception {
        Pipeline latestGoInstance = PipelineMother.schedule(goPipelineConfig, BuildCause.createManualForced(svnMaterialRevs, new Username(new CaseInsensitiveString("loser"))));
        latestGoInstance = pipelineDao.saveWithStages(latestGoInstance);
        dbHelper.passStage(latestGoInstance.getStages().first());
        configHelper.addMaterialToPipeline(GO_PIPELINE_NAME, new DependencyMaterialConfig(new CaseInsensitiveString(GO_PIPELINE_UPSTREAM), new CaseInsensitiveString(STAGE_NAME)));
        svnRepository.checkInOneFile("a.java");
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(GO_PIPELINE_NAME);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), not(hasItem(GO_PIPELINE_NAME)));
    }


    @Test
    public void shouldNotSchedulePipelineWithManualFirstStageForAutomaticBuild() throws Exception {
        configHelper.configureStageAsManualApproval(MINGLE_PIPELINE_NAME, STAGE_NAME);
        svnRepository.checkInOneFile("a.java");
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), not(hasItem(MINGLE_PIPELINE_NAME)));
    }

    @Test
    public void shouldSchedulePipelineWithManualFirstStageWhenManuallyTriggered() throws Exception {
        configHelper.configureStageAsManualApproval(MINGLE_PIPELINE_NAME, STAGE_NAME);

        svnRepository.checkInOneFile("a.java");
        materialDatabaseUpdater.updateMaterial(svnRepository.material());

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(MINGLE_PIPELINE_NAME, Username.ANONYMOUS, new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());

        Map<String, BuildCause> afterLoad = scheduleHelper.waitForAnyScheduled(5);
        assertThat(afterLoad.keySet(), hasItem(MINGLE_PIPELINE_NAME));
        BuildCause cause = afterLoad.get(MINGLE_PIPELINE_NAME);
        assertThat(cause.getBuildCauseMessage(), containsString("Forced by anonymous"));
    }

    @Test
    public void shouldScheduleANewPipelineWhenManuallyTrigeredWithNoChanges() throws Exception {
        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(MINGLE_PIPELINE_NAME, Username.ANONYMOUS, new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()),
                new ServerHealthStateOperationResult());
        assertThat(scheduleHelper.waitForAnyScheduled(5).keySet(), hasItem(MINGLE_PIPELINE_NAME));
    }

    @Test
    public void shouldStopAutoSchedulingIfDiskSpaceIsLessThanMinimum() throws Exception {
        diskSpaceSimulator.simulateDiskFull();

        scheduleHelper.autoSchedulePipelinesWithRealMaterials();

        assertThat(serverHealthService.getLogsAsText(),
                containsString("Go Server has run out of artifacts disk space. Scheduling has been stopped"));
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), not(hasItem(MINGLE_PIPELINE_NAME)));
    }

    @Test
    public void shouldStopManualSchedulingIfDiskSpaceIsLessThanMinimum() throws Exception {
        diskSpaceSimulator.simulateDiskFull();

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(MINGLE_PIPELINE_NAME, Username.ANONYMOUS, new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()),
                new ServerHealthStateOperationResult());

        assertThat(serverHealthService.getLogsAsText(),
                containsString("Go Server has run out of artifacts disk space. Scheduling has been stopped"));
        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), not(hasItem(MINGLE_PIPELINE_NAME)));
    }

    @Test
    public void shouldUnderstandChangedMaterial_forCompatibleRevisionsBeingSelectedForChangedMaterials_whenTriggeringTheFirstTime() throws Exception {
        DependencyMaterialConfig mingleMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(MINGLE_PIPELINE_NAME), new CaseInsensitiveString(STAGE_NAME));
        String mingleDownstreamPipelineName = "down_of_mingle";
        SvnMaterial svn = SvnMaterial.createSvnMaterialWithMock(repository);

        runAndPassWith(svn, "foo.c", svnRepository);

        svnRepository.checkInOneFile("bar.c");
        materialDatabaseUpdater.updateMaterial(svn);

        configHelper.addPipeline(mingleDownstreamPipelineName, STAGE_NAME, new MaterialConfigs(svn.config(), mingleMaterialConfig), "unit");

        pipelineTimeline.update();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(mingleDownstreamPipelineName);

        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(mingleDownstreamPipelineName));
        BuildCause downstreamBuildCause = pipelineScheduleQueue.toBeScheduled().get(mingleDownstreamPipelineName);
        for (MaterialRevision materialRevision : downstreamBuildCause.getMaterialRevisions()) {
            assertThat("material revision " + materialRevision + " was marked as not changed", materialRevision.isChanged(), is(true));
        }
        assertThat(downstreamBuildCause.getMaterialRevisions().getRevisions().size(), is(2));
    }

    @Test
    public void shouldUnderstandChangedMaterial_forBisectAfterBisect() throws Exception {
        SvnMaterial svn = SvnMaterial.createSvnMaterialWithMock(repository);

        runAndPassWith(svn, "foo.c", svnRepository);
        MaterialRevisions revsAfterFoo = checkinFile(svn, "foo_other.c", svnRepository);
        runAndPassWith(svn, "bar.c", revsAfterFoo, svnRepository);
        MaterialRevisions revsAfterBar = checkinFile(svn, "bar_other.c", svnRepository);
        runAndPassWith(svn, "baz.c", revsAfterBar, svnRepository);

        runAndPass(revsAfterFoo);
        String revisionForFingerPrint = revsAfterBar.findRevisionForFingerPrint(svn.getFingerprint()).getRevision().getRevision();
        scheduleHelper.manuallySchedulePipelineWithRealMaterials(MINGLE_PIPELINE_NAME, new Username(new CaseInsensitiveString("loser")), m(mingleConfig.materialConfigs().get(0).getPipelineUniqueFingerprint(), revisionForFingerPrint));

        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(MINGLE_PIPELINE_NAME));
        BuildCause bisectAfterBisectBuildCause = pipelineScheduleQueue.toBeScheduled().get(MINGLE_PIPELINE_NAME);
        for (MaterialRevision materialRevision : bisectAfterBisectBuildCause.getMaterialRevisions()) {
            assertThat("material revision " + materialRevision + " should have been considered not changed.", materialRevision.isChanged(), is(false));
        }
        assertThat(bisectAfterBisectBuildCause.getMaterialRevisions().getRevisions().size(), is(1));
    }

    @Test
    public void shouldUnderstandChangedMaterial_forManual_triggerWithOptions_DoneWithANewRevision() throws Exception {
        SvnMaterial svn = SvnMaterial.createSvnMaterialWithMock(repository);

        MaterialRevisions revsAfterFoo = checkinFile(svn, "foo.c", svnRepository);

        String revisionForFingerPrint = revsAfterFoo.findRevisionForFingerPrint(svn.getFingerprint()).getRevision().getRevision();
        scheduleHelper.manuallySchedulePipelineWithRealMaterials(MINGLE_PIPELINE_NAME, new Username(new CaseInsensitiveString("loser")), m(MaterialsMother.createMaterialFromMaterialConfig(mingleConfig.materialConfigs().get(0)).getPipelineUniqueFingerprint(), revisionForFingerPrint));

        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(MINGLE_PIPELINE_NAME));
        BuildCause bisectAfterBisectBuildCause = pipelineScheduleQueue.toBeScheduled().get(MINGLE_PIPELINE_NAME);
        for (MaterialRevision materialRevision : bisectAfterBisectBuildCause.getMaterialRevisions()) {
            assertThat("material revision " + materialRevision + " should have been considered changed.", materialRevision.isChanged(), is(true));
        }
        assertThat(bisectAfterBisectBuildCause.getMaterialRevisions().getRevisions().size(), is(1));
    }

    @Test
    public void should_NOT_markAsChangedWhenMaterialIsReIntroducedWithSameRevisionsToPipeline() throws Exception {
        SvnMaterial svn1 = SvnMaterial.createSvnMaterialWithMock(repository);
        svn1.setFolder("another_repo");
        mingleConfig = configHelper.replaceMaterialForPipeline(MINGLE_PIPELINE_NAME, svn1.config());
        runAndPassWith(svn1, "foo.c", svnRepository);

        SvnTestRepo svn2Repository = new SvnTestRepo("testSvnRepo2");
        Subversion repository2 = new SvnCommand(null, svn2Repository.projectRepositoryUrl());
        SvnMaterial svn2 = SvnMaterial.createSvnMaterialWithMock(repository2);
        svn2.setFolder("boulder");

        checkinFile(svn2, "bar.c", svn2Repository);

        mingleConfig = configHelper.addMaterialToPipeline(MINGLE_PIPELINE_NAME, svn2.config());

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);

        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(MINGLE_PIPELINE_NAME));
        BuildCause mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(MINGLE_PIPELINE_NAME);
        verifyChanged(svn2, mingleBuildCause, true);
        verifyChanged(svn1, mingleBuildCause, false);//this should not have changed, as foo.c was already built in the previous instance

        runAndPass(mingleBuildCause.getMaterialRevisions());

        mingleConfig = configHelper.replaceMaterialForPipeline(MINGLE_PIPELINE_NAME, svn1.config());
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);

        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(MINGLE_PIPELINE_NAME));
        mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(MINGLE_PIPELINE_NAME);
        verifyChanged(svn1, mingleBuildCause, false);//this should not have changed, as foo.c was already built in the previous instance
        runAndPassWith(svn1, "baz.c", svnRepository);

        mingleConfig = configHelper.addMaterialToPipeline(MINGLE_PIPELINE_NAME, svn2.config());

        checkinFile(svn1, "quux.c", svnRepository);

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);

        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(MINGLE_PIPELINE_NAME));
        mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(MINGLE_PIPELINE_NAME);
        verifyChanged(svn2, mingleBuildCause, false);
        verifyChanged(svn1, mingleBuildCause, true);
    }

    @Test
    public void should_produceBuildCause_whenMaterialConfigurationChanges() throws Exception {
        SvnMaterial svn1 = SvnMaterial.createSvnMaterialWithMock(repository);
        mingleConfig = configHelper.replaceMaterialForPipeline(MINGLE_PIPELINE_NAME, svn1.config());
        runAndPassWith(svn1, "foo.c", svnRepository);

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);

        BuildCause mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(MINGLE_PIPELINE_NAME);
        assertThat(mingleBuildCause, is(nullValue()));

        svn1.setFolder("another_repo");
        mingleConfig = configHelper.replaceMaterialForPipeline(MINGLE_PIPELINE_NAME, svn1.config());

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(MINGLE_PIPELINE_NAME);

        assertThat(pipelineScheduleQueue.toBeScheduled().keySet(), hasItem(MINGLE_PIPELINE_NAME));
        mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(MINGLE_PIPELINE_NAME);
        verifyChanged(svn1, mingleBuildCause, false);//because material configuration changed, and not actual revisions
    }

    @Test
    public void shouldNotAutoSchedulePausedPipeline() throws Exception {
        ScheduleTestUtil u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1", "h2"};
        u.checkinInOrder(hg, hg_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(hg));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(hg));
        pipelinePauseService.pause(p1.config.name().toString(), "pausing p1", Username.ANONYMOUS);
        ServerHealthStateOperationResult p1Result = new ServerHealthStateOperationResult();
        service.autoSchedulePipeline(p1.config.name().toString(), p1Result, 1234);
        assertThat(p1Result.canContinue(), is(false));

        ServerHealthStateOperationResult p2Result = new ServerHealthStateOperationResult();
        service.autoSchedulePipeline(p2.config.name().toString(), p2Result, 1234);
        assertThat(p2Result.canContinue(), is(true));
    }

    private void verifyChanged(Material material, BuildCause bc, final boolean changed) {
        MaterialRevision svn2MaterialRevision = bc.getMaterialRevisions().findRevisionForFingerPrint(material.getFingerprint());
        assertThat("material revision " + svn2MaterialRevision + " was marked as" + (changed ? " not" : "") + " changed", svn2MaterialRevision.isChanged(), is(changed));
    }

    private MaterialRevisions runAndPassWith(SvnMaterial svn, final String checkinFile, final SvnTestRepo svnRepository) throws Exception {
        return runAndPassWith(svn, checkinFile, null, svnRepository);
    }

    private MaterialRevisions runAndPassWith(SvnMaterial svn, String checkinFile, MaterialRevisions revsAfterFoo, final SvnTestRepo svnRepository) throws Exception {
        MaterialRevisions newRevs = checkinFile(svn, checkinFile, svnRepository);
        if (revsAfterFoo != null) {
            for (MaterialRevision newRev : newRevs) {
                newRev.addModifications(revsAfterFoo.getModifications(newRev.getMaterial()));
            }
        }
        runAndPass(newRevs);
        return newRevs;
    }

    private void runAndPass(MaterialRevisions mingleRev) {
        BuildCause buildCause = BuildCause.createWithModifications(mingleRev, "boozer");
        latestPipeline = PipelineMother.schedule(mingleConfig, buildCause);
        latestPipeline = pipelineDao.saveWithStages(latestPipeline);
        dbHelper.passStage(latestPipeline.getStages().first());
    }

    private MaterialRevisions checkinFile(SvnMaterial svn, String checkinFile, final SvnTestRepo svnRepository) throws Exception {
        svnRepository.checkInOneFile(checkinFile);
        materialDatabaseUpdater.updateMaterial(svn);
        return materialRepository.findLatestModification(svn);
    }

}
