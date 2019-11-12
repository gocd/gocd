/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.service.ScheduleTestUtil;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;
import static com.thoughtworks.go.helper.MaterialsMother.svnMaterial;
import static com.thoughtworks.go.helper.ModificationsMother.*;
import static com.thoughtworks.go.server.dao.PersistentObjectMatchers.hasSameId;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PipelineSqlMapDaoIntegrationTest {
    @Autowired
    private StageDao stageDao;
    @Autowired
    private PipelineSqlMapDao pipelineDao;
    @Autowired
    private JobInstanceDao jobInstanceDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private GoCache goCache;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PipelinePauseService pipelinePauseService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private InstanceFactory instanceFactory;
    @Autowired
    private DependencyMaterialUpdateNotifier notifier;

    private String md5 = "md5-test";
    private ScheduleTestUtil u;
    private GoConfigFileHelper configHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        goCache.clear();
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        notifier.disableUpdates();
    }

    @After
    public void teardown() throws Exception {
        notifier.enableUpdates();
        dbHelper.onTearDown();

    }

    private Pipeline schedulePipelineWithStages(PipelineConfig pipelineConfig) throws Exception {
        BuildCause buildCause = BuildCause.createWithModifications(modifyOneFile(pipelineConfig), "");
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(DEFAULT_APPROVED_BY), "md5-test", new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        long pipelineId = pipeline.getId();
        assertIsInserted(pipelineId);
        return pipeline;
    }

    private void savePipeline(final Pipeline pipeline) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline.getBuildCause().getMaterialRevisions());
                pipelineDao.saveWithStages(pipeline);
            }
        });
    }

    private void schedulePipelineWithoutCounter(PipelineConfig pipelineConfig) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""),
                new DefaultSchedulingContext(
                        "anyone"), "md5-test", new TimeProvider());
        save(pipeline);
        for (Stage stage : pipeline.getStages()) {
            stageDao.saveWithJobs(pipeline, stage);
        }
    }

    private void save(final Pipeline pipeline) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline.getBuildCause().getMaterialRevisions());
                pipelineDao.save(pipeline);
            }
        });
    }

    @Test
    public void shouldLoadNaturalOrder() throws SQLException {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createManualForced(), new Stage("dev", new JobInstances(new JobInstance("unit")), "anonymous", null, "manual", new TimeProvider()));
        savePipeline(pipeline);
        dbHelper.updateNaturalOrder(pipeline.getId(), 2.5);
        PipelineInstanceModel loaded = pipelineDao.loadHistory("Test").get(0);
        PipelineInstanceModel loadedById = pipelineDao.loadHistory(pipeline.getId());
        assertThat(loadedById.getNaturalOrder(), is(2.5));
        assertThat(loaded.getNaturalOrder(), is(2.5));
        assertThat(loaded.isBisect(), is(true));
    }

    @Test
    public void shouldLoadStageResult() throws SQLException {
        Stage stage = new Stage("dev", new JobInstances(new JobInstance("unit")), "anonymous", null, "manual", new TimeProvider());
        stage.building();
        Pipeline pipeline = new Pipeline("Test", BuildCause.createManualForced(), stage);
        savePipeline(pipeline);
        PipelineInstanceModel loaded = pipelineDao.loadHistory("Test").get(0);
        assertThat(loaded.getStageHistory().get(0).getResult(), is(StageResult.Unknown));
        PipelineInstanceModel loadedById = pipelineDao.loadHistory(pipeline.getId());
        assertThat(loadedById.getStageHistory().get(0).getResult(), is(StageResult.Unknown));
    }

    @Test
    public void shouldLoadStageIdentifier() throws SQLException {
        Stage stage = new Stage("dev", new JobInstances(new JobInstance("unit")), "anonymous", null, "manual", new TimeProvider());
        stage.building();
        Pipeline pipeline = new Pipeline("Test", BuildCause.createManualForced(), stage);
        savePipeline(pipeline);
        PipelineInstanceModel loaded = pipelineDao.loadHistory("Test").get(0);
        StageInstanceModel historicalStage = loaded.getStageHistory().get(0);
        assertThat(historicalStage.getIdentifier(), is(new StageIdentifier("Test", loaded.getCounter(), loaded.getLabel(), "dev", historicalStage.getCounter())));
        PipelineInstanceModel loadedById = pipelineDao.loadHistory(pipeline.getId());
        StageInstanceModel historicalStageModelById = loadedById.getStageHistory().get(0);
        assertThat(historicalStageModelById.getIdentifier(), is(new StageIdentifier("Test", loadedById.getCounter(), loadedById.getLabel(), "dev", historicalStage.getCounter())));
    }

    @Test
    public void shouldDefaultCounterToZero() {
        assertThat(pipelineDao.getCounterForPipeline("pipeline-with-no-history"), is(0));
    }

    @Test
    public void shouldReserveModificationsOrder() throws Exception {
        MaterialRevisions materialRevisions = ModificationsMother.multipleModifications();
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithModifications(materialRevisions, ""));
        save(pipeline);

        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        ModificationsCollector mods = new ModificationsCollector();
        loaded.getBuildCause().getMaterialRevisions().accept(mods);
        assertEquals(ModificationsMother.TODAY_CHECKIN, mods.first().getModifiedTime());
    }

    @Test
    public void shouldLoadModificationsWithChangedFlagApplied() throws Exception {
        MaterialRevisions materialRevisions = ModificationsMother.multipleModifications();
        materialRevisions.getMaterialRevision(0).markAsChanged();
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithModifications(materialRevisions, ""));
        save(pipeline);

        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        assertThat(loaded.getBuildCause().getMaterialRevisions().getMaterialRevision(0).isChanged(), is(true));
    }

    @Test
    public void shouldLoadModificationsWithNoModifiedFiles() throws Exception {
        List<Modification> modifications = new ArrayList<>();
        Modification modification = ModificationsMother.oneModifiedFile(ModificationsMother.nextRevision());
        modifications.add(modification);
        modifications.add(new Modification(MOD_USER, MOD_COMMENT, "foo@bar.com", YESTERDAY_CHECKIN, ModificationsMother.nextRevision()));
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();
        MaterialRevisions revisions = new MaterialRevisions();
        revisions.addRevision(svnMaterial, modifications);

        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithModifications(revisions, ""));
        savePipeline(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        ModificationsCollector summary = new ModificationsCollector();
        loaded.getBuildCause().getMaterialRevisions().accept(summary);
        assertThat(summary.numberOfModifications(), is(2));
    }

    @Test
    public void shouldSupportModifiedFileWithVeryLongName() {
        Modification modification = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK();

        Pipeline pipeline = new Pipeline("Test",
                BuildCause.createWithModifications(ModificationsMother.createSvnMaterialRevisions(modification), ""));
        savePipeline(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        Modification loadedModification = loaded.getBuildCause().getMaterialRevisions().getMaterialRevision(
                0).getModification(0);
        assertEquals(modification, loadedModification);
    }

    @Test
    public void shouldTruncateFileNameIfItIsTooLong() {
        Modification modification = ModificationsMother.withModifiedFileWhoseNameLengthIsMoreThanOneK();

        Pipeline pipeline = new Pipeline("Test",
                BuildCause.createWithModifications(ModificationsMother.createSvnMaterialRevisions(modification), ""));
        savePipeline(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        Modification loadedModification = loaded.getBuildCause().getMaterialRevisions().getMaterialRevision(
                0).getModification(0);
        assertThat(loadedModification.getModifiedFiles().get(0).getFileName().length(),
                is(ModifiedFile.MAX_NAME_LENGTH));
    }

    @Test
    public void shouldPersistBuildCauseMessage() throws SQLException {
        MaterialRevisions materialRevisions = ModificationsMother.multipleModifications();
        BuildCause buildCause = BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS);
        Pipeline pipeline = new Pipeline("Test", buildCause);
        save(pipeline);
        Pipeline loaded = pipelineDao.mostRecentPipeline("Test");
        assertThat(loaded.getBuildCauseMessage(), is(buildCause.getBuildCauseMessage()));
    }

    @Test
    public void shouldReturnNullWhileLoadingMostRecentPipelineForNoPipelineFound() throws SQLException {
        Pipeline loaded = pipelineDao.mostRecentPipeline("Test");
        assertThat(loaded, is(instanceOf(NullPipeline.class)));
    }

    @Test
    public void shouldPersistBuildCauseEnvironmentVariables() {
        MaterialRevisions materialRevisions = ModificationsMother.multipleModifications();
        BuildCause buildCause = BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS);
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.add(new EnvironmentVariable("VAR1", "value one"));
        environmentVariables.add(new EnvironmentVariable("VAR2", "value two"));
        buildCause.addOverriddenVariables(environmentVariables);
        Pipeline pipeline = new Pipeline("Test", buildCause);
        save(pipeline);
        Pipeline loaded = pipelineDao.mostRecentPipeline("Test");
        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("VAR1", "value one");
        variables.add("VAR2", "value two");
        assertThat(loaded.getBuildCause().getVariables(), is(variables));
        assertThat(loaded.scheduleTimeVariables(), is(variables));
    }

    @Test
    public void shouldPersistMaterialsWithRealPassword() {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        addRevision(materialRevisions, MaterialsMother.svnMaterial("http://username:password@localhost"));
        addRevision(materialRevisions, MaterialsMother.hgMaterial("http://username:password@localhost"));
        addRevision(materialRevisions, new GitMaterial("git://username:password@localhost"));
        addRevision(materialRevisions, new P4Material("localhost:1666", "view"));

        BuildCause buildCause = BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS);
        Pipeline pipeline = new Pipeline("Test", buildCause);
        save(pipeline);
        Pipeline loaded = pipelineDao.mostRecentPipeline("Test");
        Materials materials = loaded.getMaterials();
        for (Material material : materials) {
            assertThat(((ScmMaterial) material).getUrl(), not(containsString("******")));
        }
    }

    private void addRevision(MaterialRevisions materialRevisions, Material material) {
        materialRevisions.addRevision(material, ModificationsMother.multipleModificationList());
    }


    @Test
    public void shouldSchedulePipelineWithModifications() throws Exception {
        Pipeline pipeline = schedulePipelineWithStages(
                PipelineMother.withMaterials("mingle", "dev", BuildPlanMother.withBuildPlans("functional", "unit"))
        );
        assertModifications(pipeline);
    }

    private Matcher<PipelineInstanceModels> hasPipeline(final Pipeline pipeline) {
        return new BaseMatcher<PipelineInstanceModels>() {

            @Override
            public boolean matches(Object o) {
                if (o instanceof PipelineInstanceModels) {
                    PipelineInstanceModels pipelineInstanceModels = (PipelineInstanceModels) o;
                    for (PipelineInstanceModel pipelineInstanceModel : pipelineInstanceModels) {
                        if (pipelineInstanceModel.getName().equals(pipeline.getName()) && pipelineInstanceModel.getCounter().equals(pipeline.getCounter())) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected pipline " + pipeline.getName() + " with counter " + pipeline.getCounter());
            }
        };
    }

    private void ensureBuildCauseIsLoadedFor(PipelineInstanceModel upstream) {
        assertThat(StringUtils.isEmpty(upstream.getRevisionOfLatestModification()), is(false));
    }

    private void saveRev(final Material material, final Modification modification) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.saveMaterialRevision(new MaterialRevision(material, modification));
            }
        });
    }

    private PipelineConfig pipelineConfigFor(final String pipelineName, final String dependentOnPipeline, final String dependentOnStage) {
        PipelineConfig config = PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "cruiseStage");
        config.materialConfigs().clear();
        config.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString(dependentOnPipeline), new CaseInsensitiveString(dependentOnStage)));
        return config;
    }

    private void assertPipelineEquals(Pipeline expected, PipelineInstanceModel actual) {
        assertThat(actual.getName(), is(expected.getName()));
        assertThat(actual.getCounter(), is(expected.getCounter()));
    }

    //TODO FIXME sorted by Id is not good.
    // - Comment by Bobby: Sorted by Id is exactly what we want here. Please discuss.
    @Test
    public void shouldLoadMostRecentPipeline() throws Exception {
        String stageName = "dev";
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", stageName);
        Pipeline mingle = schedulePipelineWithStages(mingleConfig);

        Pipeline mostRecentPipeline = pipelineDao.mostRecentPipeline(mingle.getName());
        assertThat(mostRecentPipeline, hasSameId(mingle));
        assertThat(mostRecentPipeline.getBuildCause().getMaterialRevisions().totalNumberOfModifications(), is(1));
    }

    @Test
    public void shouldLoadMostRecentPipelineWithSingleStage() throws Exception {
        String stageName = "dev";
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", stageName);
        Pipeline mingle = schedulePipelineWithStages(mingleConfig);

        Pipeline mostRecentPipeline = pipelineDao.mostRecentPipeline(mingle.getName());

        assertThat(mostRecentPipeline, hasSameId(mingle));
        assertThat(mostRecentPipeline.getStages().size(), is(1));
        assertThat(mostRecentPipeline.getFirstStage().getName(), is(stageName));
        assertThat(mostRecentPipeline.getBuildCause().getMaterialRevisions().totalNumberOfModifications(), is(1));
    }

    @Test
    public void shouldLoadMostRecentPipelineWithMutipleSameStage() throws Exception {
        String stageName = "dev";
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", stageName);
        Pipeline mingle = schedulePipelineWithStages(mingleConfig);

        Stage newInstance = rescheduleStage(stageName, mingleConfig, mingle);

        Pipeline mostRecentPipeline = pipelineDao.mostRecentPipeline(mingle.getName());

        assertThat(mostRecentPipeline, hasSameId(mingle));
        assertThat("Asserting number of stages. Stages are:\n\t" + mostRecentPipeline.getStages(),
                mostRecentPipeline.getStages().size(), is(1));
        assertThat(mostRecentPipeline.getFirstStage().getId(), is(newInstance.getId()));
        assertThat(mostRecentPipeline.getBuildCause().getMaterialRevisions().totalNumberOfModifications(), is(1));
    }

    private Stage rescheduleStage(String stageName, PipelineConfig mingleConfig, Pipeline pipeline) {
        Stage newInstance = instanceFactory.createStageInstance(mingleConfig.findBy(new CaseInsensitiveString(stageName)), new DefaultSchedulingContext("anyone"), md5, new TimeProvider());
        return stageDao.saveWithJobs(pipeline, newInstance);
    }

    @Test
    public void shouldLoadPipelineHistories() throws Exception {
        String dev = "dev";
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", dev);
        Pipeline mingle = schedulePipelineWithStages(mingleConfig);
        Stage firstStage = mingle.getFirstStage();
        Pipeline mingle2 = schedulePipelineWithStages(mingleConfig);

        JobInstance instance = firstStage.getJobInstances().first();
        jobInstanceDao.ignore(instance);

        PipelineInstanceModels pipelineHistories = pipelineDao.loadHistory(mingle.getName(), 10, 0);
        assertThat(pipelineHistories.size(), is(2));
        StageInstanceModels stageHistories = pipelineHistories.first().getStageHistory();
        assertThat(stageHistories.size(), is(1));

        StageInstanceModel history = stageHistories.first();
        assertThat(history.getName(), is(dev));
        assertThat(history.getApprovalType(), is(GoConstants.APPROVAL_SUCCESS));
        assertThat(history.getBuildHistory().size(), is(2));

        assertThat(pipelineHistories.get(1).getName(), is("mingle"));
    }

    @Test
    public void shouldReturnEmptyListWhenThereIsNoPipelineHistory() throws Exception {
        PipelineInstanceModels pipelineHistories = pipelineDao.loadHistory("something not exist", 10, 0);
        assertThat(pipelineHistories.size(), is(0));
    }

    @Test
    public void shouldLoadPipelineHistoryOnlyForSuppliedPipeline() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        PipelineConfig otherConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("other", "dev");

        schedulePipelineWithStages(mingleConfig);
        schedulePipelineWithStages(otherConfig);
        schedulePipelineWithStages(mingleConfig);
        schedulePipelineWithStages(otherConfig);
        schedulePipelineWithStages(mingleConfig);

        PipelineInstanceModels pipelineHistories = pipelineDao.loadHistory("mingle", 10, 0);

        assertThat(pipelineHistories.get(0).getName(), is("mingle"));
        assertThat(pipelineHistories.get(1).getName(), is("mingle"));
        assertThat(pipelineHistories.get(2).getName(), is("mingle"));
        assertThat(pipelineHistories.size(), is(3));
    }

    @Test
    public void shouldSupportPipelinesWithoutCounterWhenLoadHistory() {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        schedulePipelineWithoutCounter(mingleConfig);

        PipelineInstanceModels models = pipelineDao.loadHistory(CaseInsensitiveString.str(mingleConfig.name()), 10, 0);
        assertThat(models.size(), is(1));
    }

    @Test
    public void shouldLoadAllActivePipelines() throws Exception {
        PipelineConfig twistConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("twist", "dev", "ft");
        Pipeline twistPipeline = dbHelper.newPipelineWithAllStagesPassed(twistConfig);
        List<CaseInsensitiveString> allPipelineNames = goConfigDao.load().getAllPipelineNames();
        if (!allPipelineNames.contains(new CaseInsensitiveString("twist"))) {
            goConfigDao.addPipeline(twistConfig, "pipelinesqlmapdaotest");
        }

        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev", "ft");
        if (!allPipelineNames.contains(new CaseInsensitiveString("mingle"))) {
            goConfigDao.addPipeline(mingleConfig, "pipelinesqlmapdaotest");
        }


        Pipeline firstPipeline = dbHelper.newPipelineWithAllStagesPassed(mingleConfig);
        Pipeline secondPipeline = dbHelper.newPipelineWithFirstStagePassed(mingleConfig);
        dbHelper.scheduleStage(secondPipeline, mingleConfig.get(1));
        Pipeline thirdPipeline = dbHelper.newPipelineWithFirstStageScheduled(mingleConfig);

        PipelineInstanceModels pipelineHistories = pipelineDao.loadActivePipelines();
        assertThat(pipelineHistories.size(), is(3));
        assertThat(pipelineHistories.get(0).getId(), is(thirdPipeline.getId()));
        assertThat(pipelineHistories.get(1).getId(), is(secondPipeline.getId()));
        assertThat(pipelineHistories.get(2).getId(), is(twistPipeline.getId()));
        assertThat(pipelineHistories.get(0).getBuildCause().getMaterialRevisions().isEmpty(), is(false));
    }

    @Test
    public void shouldLoadAllActivePipelinesPresentInConfigOnly() throws Exception {
        PipelineConfig twistConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("twist", "dev", "ft");
        Pipeline twistPipeline = dbHelper.newPipelineWithAllStagesPassed(twistConfig);
        List<CaseInsensitiveString> allPipelineNames = goConfigDao.load().getAllPipelineNames();
        if (!allPipelineNames.contains(new CaseInsensitiveString("twist"))) {
            goConfigDao.addPipeline(twistConfig, "pipelinesqlmapdaotest");
        }
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev", "ft");

        dbHelper.newPipelineWithAllStagesPassed(mingleConfig);
        Pipeline minglePipeline = dbHelper.newPipelineWithFirstStagePassed(mingleConfig);

        PipelineInstanceModels pipelineHistories = pipelineDao.loadActivePipelines();
        assertThat(pipelineHistories.size(), is(1));
        assertThat(pipelineHistories.get(0).getId(), is(twistPipeline.getId()));
        assertThat(pipelineHistories.get(0).getBuildCause().getMaterialRevisions().isEmpty(), is(false));

        if (!allPipelineNames.contains(new CaseInsensitiveString("mingle"))) {
            goConfigDao.addPipeline(mingleConfig, "pipelinesqlmapdaotest");
        }

        pipelineHistories = pipelineDao.loadActivePipelines();
        assertThat(pipelineHistories.size(), is(2));
        assertThat(pipelineHistories.get(0).getId(), is(minglePipeline.getId()));
        assertThat(pipelineHistories.get(1).getId(), is(twistPipeline.getId()));
        assertThat(pipelineHistories.get(1).getBuildCause().getMaterialRevisions().isEmpty(), is(false));
    }

    @Test
    public void loadAllActivePipelinesPresentInConfigOnlyShouldBeCaseInsensitive() throws Exception {
        PipelineConfig twistConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("twist", "dev", "ft");
        Pipeline twistPipeline = dbHelper.newPipelineWithAllStagesPassed(twistConfig);
        List<CaseInsensitiveString> allPipelineNames = goConfigDao.load().getAllPipelineNames();
        if (!allPipelineNames.contains(new CaseInsensitiveString("twist"))) {
            PipelineConfig pipelineConfigWithDifferentCase = PipelineMother.createPipelineConfig("TWIST", twistConfig.materialConfigs(), "dev", "ft");
            goConfigDao.addPipeline(pipelineConfigWithDifferentCase, "pipelinesqlmapdaotest");
        }
        PipelineInstanceModels pipelineHistories = pipelineDao.loadActivePipelines();
        assertThat(pipelineHistories.size(), is(1));
        assertThat(pipelineHistories.get(0).getId(), is(twistPipeline.getId()));
        assertThat(pipelineHistories.get(0).getBuildCause().getMaterialRevisions().isEmpty(), is(false));

    }

    @Test
    public void shouldLoadAllActivePipelinesPresentInConfigAndAlsoTheScheduledStagesOfPipelinesNotInConfig() throws Exception {
        PipelineConfig twistConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("twist", "dev", "ft");
        Pipeline twistPipeline = dbHelper.newPipelineWithAllStagesPassed(twistConfig);
        List<CaseInsensitiveString> allPipelineNames = goConfigDao.load().getAllPipelineNames();
        if (!allPipelineNames.contains(new CaseInsensitiveString("twist"))) {
            goConfigDao.addPipeline(twistConfig, "pipelinesqlmapdaotest");
        }
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev", "ft");
        if (!allPipelineNames.contains(new CaseInsensitiveString("mingle"))) {
            goConfigDao.addPipeline(mingleConfig, "pipelinesqlmapdaotest");
        }
        dbHelper.newPipelineWithAllStagesPassed(mingleConfig);
        Pipeline secondPipeline = dbHelper.newPipelineWithFirstStagePassed(mingleConfig);
        dbHelper.scheduleStage(secondPipeline, mingleConfig.get(1));
        Pipeline thirdPipeline = dbHelper.newPipelineWithFirstStageScheduled(mingleConfig);

        PipelineInstanceModels pipelineHistories = pipelineDao.loadActivePipelines();
        assertThat(pipelineHistories.size(), is(3));
        assertThat(pipelineHistories.get(0).getId(), is(thirdPipeline.getId()));
        assertThat(pipelineHistories.get(1).getId(), is(secondPipeline.getId()));
        assertThat(pipelineHistories.get(2).getId(), is(twistPipeline.getId()));
        assertThat(pipelineHistories.get(0).getBuildCause().getMaterialRevisions().isEmpty(), is(false));
    }


    @Test
    public void shouldLoadAllActivePipelinesEvenWhenThereIsStageStatusChange() throws Exception {
        PipelineConfig twistConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("twist", "dev", "ft");
        goConfigDao.addPipeline(twistConfig, "pipelinesqlmapdaotest");
        Pipeline twistPipeline = dbHelper.newPipelineWithAllStagesPassed(twistConfig);
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev", "ft");
        goConfigDao.addPipeline(mingleConfig, "pipelinesqlmapdaotest");
        final Pipeline firstPipeline = dbHelper.newPipelineWithAllStagesPassed(mingleConfig);
        final Pipeline secondPipeline = dbHelper.newPipelineWithFirstStagePassed(mingleConfig);
        dbHelper.scheduleStage(secondPipeline, mingleConfig.get(1));
        Pipeline thirdPipeline = dbHelper.newPipelineWithFirstStageScheduled(mingleConfig);
        Thread stageStatusChanger = new Thread() {
            @Override
            public void run() {
                for (; ; ) {
                    pipelineDao.stageStatusChanged(secondPipeline.findStage("dev"));
                    if (super.isInterrupted()) {
                        break;
                    }
                }
            }
        };
        stageStatusChanger.setDaemon(true);
        stageStatusChanger.start();
        PipelineInstanceModels pipelineHistories = pipelineDao.loadActivePipelines();
        assertThat(pipelineHistories.size(), is(3));
        assertThat(pipelineHistories.get(0).getId(), is(thirdPipeline.getId()));
        assertThat(pipelineHistories.get(1).getId(), is(secondPipeline.getId()));
        assertThat(pipelineHistories.get(2).getId(), is(twistPipeline.getId()));
        assertThat(pipelineHistories.get(0).getBuildCause().getMaterialRevisions().isEmpty(), is(false));
        stageStatusChanger.interrupt();
    }

    @Test
    public void shouldLoadPipelineHistoriesWithMultipleSameStage() throws Exception {
        String stageName = "dev";
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", stageName);
        Pipeline mingle = schedulePipelineWithStages(mingleConfig);

        Stage newInstance = rescheduleStage(stageName, mingleConfig, mingle);

        PipelineInstanceModels pipelineHistories = pipelineDao.loadHistory(mingle.getName(), 10, 0);
        assertThat(pipelineHistories.size(), is(1));
        StageInstanceModels stageHistories = pipelineHistories.first().getStageHistory();
        assertThat(stageHistories.size(), is(1));
        StageInstanceModel history = stageHistories.first();
        assertThat(history.getName(), is(stageName));
        assertThat(history.getId(), is(newInstance.getId()));
        assertThat(history.getBuildHistory().size(), is(2));
    }

    @Test
    public void shouldSaveUserNameAsCausedBy() throws Exception {
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        BuildCause cause = BuildCause.createManualForced(modifyOneFile(pipelineConfig), Username.ANONYMOUS);
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, cause, new DefaultSchedulingContext(MOD_USER), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());
        BuildCause buildCause = pipelineFromDB.getBuildCause();
        //TODO: This is a known bug #3248 in the way that the pipeline user is stored. We should fix with the new UI.
        assertThat(buildCause.getBuildCauseMessage(), is("Forced by " + Username.ANONYMOUS.getDisplayName()));
    }

    @Test
    public void shouldReturnCorrectCount() throws Exception {
        PipelineConfig mingle = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        assertThat(pipelineDao.count(CaseInsensitiveString.str(mingle.name())), is(0));
        dbHelper.pass(schedulePipelineWithStages(mingle));
        assertThat(pipelineDao.count(CaseInsensitiveString.str(mingle.name())), is(1));
    }

    @Test
    public void shouldStoreAndRetrieveSvnMaterials() throws SQLException {
        SvnMaterial svnMaterial = svnMaterial("svnUrl", "folder");
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(svnMaterial.config()));

        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(svnMaterial, ModificationsMother.multipleModificationList());

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions,
                Username.ANONYMOUS), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());
        final Materials materials = pipelineFromDB.getMaterials();

        assertThat(materials.get(0), is(svnMaterial));
    }

    @Test
    public void shouldRetrieveModificationsSortedBySavedOrder() throws SQLException {
        GitMaterial gitMaterial = new GitMaterial("url");
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(gitMaterial.config()));

        Modification firstModification = new Modification(new Date(), "1", "MOCK_LABEL-12", null);
        Modification secondModification = new Modification(new Date(), "2", "MOCK_LABEL-12", null);
        ArrayList<ModifiedFile> modifiedFiles = new ArrayList<ModifiedFile>() {
            {
                add(new ModifiedFile("filename", "foldername", ModifiedAction.modified));
            }
        };
        secondModification.setModifiedFiles(modifiedFiles);

        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(gitMaterial, firstModification, secondModification);

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions,
                Username.ANONYMOUS), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());

        assertNotInserted(pipeline.getId());
        save(pipeline);

        Pipeline pipelineFromDB = pipelineDao.mostRecentPipeline(pipeline.getName());

        List<MaterialRevision> revisionsFromDB = pipelineFromDB.getMaterialRevisions().getRevisions();
        List<Modification> modificationsFromDB = revisionsFromDB.get(0).getModifications();
        assertThat(modificationsFromDB.size(), is(2));
        assertThat(modificationsFromDB.get(0).getRevision(), is("1"));
        assertThat(modificationsFromDB.get(1).getRevision(), is("2"));
    }

    @Test
    public void shouldStoreAndRetrieveDependencyMaterials() throws SQLException {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(dependencyMaterial.config()));

        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(DependencyMaterialRevision.create("pipeline-name", -12, "1234", "stage-name", 1).convert(dependencyMaterial, new Date()));


        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions,
                Username.ANONYMOUS), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());

        final Materials materials = pipelineFromDB.getMaterials();

        assertThat(materials.get(0), is(dependencyMaterial));
    }

    @Test
    public void shouldStoreAndRetrieveDependencyMaterialsWithMaxAllowedRevision() throws SQLException {
        char[] name = new char[255];
        for (int i = 0; i < 255; i++) {
            name[i] = 'a';
        }
        final String s = new String(name);
        final String s1 = new String(name);
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString(s), new CaseInsensitiveString(s1));
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(dependencyMaterial.config()));

        MaterialRevisions materialRevisions = new MaterialRevisions();
        DependencyMaterialRevision revision = DependencyMaterialRevision.create(new String(name), -10, new String(name), new String(name), Integer.MAX_VALUE);
        materialRevisions.addRevision(revision.convert(dependencyMaterial, new Date()));

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions,
                Username.ANONYMOUS), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());

        final Materials materials = pipelineFromDB.getMaterials();

        assertThat(materials.get(0), is(dependencyMaterial));
    }

    @Test
    public void shouldStoreAndRetrieveMultipleSvnMaterials() throws SQLException {
        SvnMaterial svnMaterial1 = svnMaterial("svnUrl1", "folder1");
        SvnMaterial svnMaterial2 = svnMaterial("svnUrl2", "folder2");
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(svnMaterial1.config(), svnMaterial2.config()));

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(modifyOneFile(pipelineConfig),
                Username.ANONYMOUS),
                new DefaultSchedulingContext(
                        DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);

        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());

        Materials materials = pipelineFromDB.getMaterials();
        assertThat(materials, hasItem((Material) svnMaterial1));
        assertThat(materials, hasItem((Material) svnMaterial2));
    }

    @Test
    public void shouldStoreAndRetrieveMaterialRevisions() throws SQLException {
        SvnMaterial svnMaterial1 = svnMaterial("svnUrl1", "folder1");
        SvnMaterial svnMaterial2 = svnMaterial("svnUrl2", "folder2");
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(svnMaterial1.config(), svnMaterial2.config()));

        MaterialRevisions revisions = new MaterialRevisions();
        revisions.addRevision(svnMaterial1, new Modification("user1", "comment1", null, new Date(), "1"));
        revisions.addRevision(svnMaterial2, new Modification("user2", "comment2", null, new Date(), "2"));

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(revisions, Username.ANONYMOUS),
                new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());

        save(pipeline);

        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());

        BuildCause buildCause = pipelineFromDB.getBuildCause();
        assertEquals(revisions, buildCause.getMaterialRevisions());
    }

    @Test
    public void shouldStoreAndRetrieveHgMaterialsFromDatabase() throws SQLException {
        Materials materials = MaterialsMother.hgMaterials("hgUrl", "hgdir");
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(materials.convertToConfigs());

        final MaterialRevisions originalMaterialRevision = multipleModificationsInHg(pipelineConfig);
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(originalMaterialRevision, Username.ANONYMOUS), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());
        save(pipeline);

        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());
        final MaterialRevisions materialRevisions = pipelineFromDB.getBuildCause().getMaterialRevisions();

        assertEquals(originalMaterialRevision, materialRevisions);
        assertThat(materialRevisions.getMaterialRevision(0).getRevision().getRevision(),
                is("9fdcf27f16eadc362733328dd481d8a2c29915e1"));
        assertThat(pipelineFromDB.getMaterials(), is(materials));
    }

    @Test
    public void shouldHaveUrlInGitMaterials() throws SQLException {
        Materials gitMaterials = MaterialsMother.gitMaterials("gitUrl");
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(gitMaterials.convertToConfigs());

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig,
                BuildCause.createManualForced(modifyOneFile(pipelineConfig), Username.ANONYMOUS),
                new DefaultSchedulingContext(DEFAULT_APPROVED_BY),
                md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());
        Materials materials = pipelineFromDB.getMaterials();
        GitMaterial gitMaterial = (GitMaterial) materials.get(0);
        assertThat(gitMaterial.getUrl(), is("gitUrl"));
    }

    @Test
    public void shouldSupportBranchInGitMaterials() throws Exception {
        Materials branchedMaterials = MaterialsMother.gitMaterials("gitUrl", "foo");
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(branchedMaterials.convertToConfigs());

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(modifyOneFile(pipelineConfig),
                Username.ANONYMOUS),
                new DefaultSchedulingContext(
                        DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());
        Materials materials = pipelineFromDB.getMaterials();
        GitMaterial gitMaterial = (GitMaterial) materials.get(0);
        assertThat(gitMaterial.getBranch(), is("foo"));
    }

    @Test
    public void shouldSupportSubmoduleFolderInGitMaterials() throws Exception {
        Materials materials = MaterialsMother.gitMaterials("gitUrl", "submoduleFolder", null);
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(materials.convertToConfigs());

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(modifyOneFile(pipelineConfig),
                Username.ANONYMOUS), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        save(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());

        Materials materialsFromDB = pipelineFromDB.getMaterials();
        GitMaterial gitMaterial = (GitMaterial) materialsFromDB.get(0);
        assertThat(gitMaterial.getSubmoduleFolder(), is("submoduleFolder"));
    }

    @Test
    public void shouldHaveServerAndPortAndViewAndUseTicketsInP4Materials() throws SQLException {
        String p4view = "//depot/... //localhost/...";
        Materials p4Materials = MaterialsMother.p4Materials(p4view);
        P4Material p4Material = (P4Material) p4Materials.first();
        p4Material.setUseTickets(true);
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(p4Materials.convertToConfigs());

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(modifyOneFile(pipelineConfig),
                Username.ANONYMOUS),
                new DefaultSchedulingContext(
                        DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);

        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());
        Materials materials = pipelineFromDB.getMaterials();
        assertThat(materials.get(0), is(p4Material));
    }

    @Test
    public void shouldSupportMultipleP4Materials() throws SQLException {
        String p4view1 = "//depot1/... //localhost1/...";
        String p4view2 = "//depot2/... //localhost2/...";
        Material p4Material1 = MaterialsMother.p4Materials(p4view1).get(0);
        Material p4Material2 = MaterialsMother.p4Materials(p4view2).get(0);
        Materials materials = new Materials(p4Material1, p4Material2);

        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "dev");
        pipelineConfig.setMaterialConfigs(materials.convertToConfigs());

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(modifyOneFile(pipelineConfig),
                Username.ANONYMOUS),
                new DefaultSchedulingContext(
                        DEFAULT_APPROVED_BY), md5, new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        Pipeline pipelineFromDB = pipelineDao.loadPipeline(pipeline.getId());
        final Materials loaded = pipelineFromDB.getMaterials();
        assertThat(loaded.get(0), is(p4Material1));
        assertThat(loaded.get(1), is(p4Material2));
    }

    @Test
    public void shouldFindPipelineByNameAndCounterCaseInsensitively() {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithEmptyModifications());
        savePipeline(pipeline);
        Pipeline loadedId = pipelineDao.findPipelineByNameAndCounter("Test", pipeline.getCounter());
        assertThat(loadedId.getId(), is(pipeline.getId()));
        loadedId = pipelineDao.findPipelineByNameAndCounter("tEsT", pipeline.getCounter());
        assertThat(loadedId.getId(), is(pipeline.getId()));
    }

    @Test
    public void shouldFindTheRightPipelineWithUseOfTilde() {
        Pipeline correctPipeline = new Pipeline("Test", BuildCause.createWithEmptyModifications());
        savePipeline(correctPipeline);
        Pipeline incorrectPipeline = new Pipeline("Tests", BuildCause.createWithEmptyModifications());
        savePipeline(incorrectPipeline);
        Pipeline loadedId = pipelineDao.findPipelineByNameAndCounter(correctPipeline.getName(), correctPipeline.getCounter());
        assertThat(loadedId.getId(), is(correctPipeline.getId()));
        loadedId = pipelineDao.findPipelineByNameAndCounter(correctPipeline.getName().toLowerCase(), correctPipeline.getCounter());
        assertThat(loadedId.getId(), is(correctPipeline.getId()));
    }

    @Test
    public void shouldInvalidateSessionAndFetchNewPipelineByNameAndCounter_WhenPipelineIsPersisted() {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithEmptyModifications());
        assertThat(pipelineDao.findPipelineByNameAndCounter("Test", 1), is(nullValue()));

        savePipeline(pipeline);
        Pipeline loadedPipeline = pipelineDao.findPipelineByNameAndCounter("Test", pipeline.getCounter());
        assertThat(pipelineDao.findPipelineByNameAndCounter("Test", 1), is(not(nullValue())));
        assertThat(loadedPipeline.getId(), is(pipeline.getId()));
    }

    @Test
    public void shouldInvalidateSessionAndFetchNewPipelineByNameAndLabel_WhenPipelineIsPersisted() {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithEmptyModifications());
        assertThat(pipelineDao.findPipelineByNameAndLabel("Test", "1"), is(nullValue()));

        savePipeline(pipeline);
        Pipeline loadedPipeline = pipelineDao.findPipelineByNameAndLabel("Test", pipeline.getLabel());
        assertThat(loadedPipeline, is(not(nullValue())));
        assertThat(loadedPipeline.getId(), is(pipeline.getId()));
    }

    @Test
    public void shouldFindPipelineByNameAndLabel() {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithEmptyModifications());
        savePipeline(pipeline);
        Pipeline loadedId = pipelineDao.findPipelineByNameAndLabel("Test", pipeline.getLabel());
        assertThat(loadedId.getId(), is(pipeline.getId()));
    }

    @Test
    public void findPipelineByNameAndLabelShouldReturnLatestWhenLabelRepeated() {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithEmptyModifications());
        savePipeline(pipeline);
        Pipeline newPipeline = dbHelper.save(pipeline);
        Pipeline loadedId = pipelineDao.findPipelineByNameAndLabel("Test", newPipeline.getLabel());
        assertThat(loadedId.getId(), is(newPipeline.getId()));
    }

    @Test
    public void shouldSaveModificationWithChangedAsTrue() throws Exception {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithModifications(revisions(true), ""));
        save(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        assertThat(loaded.getMaterialRevisions().getMaterialRevision(0).isChanged(), is(true));
    }

    @Test
    public void shouldSaveModificationWithChangedAsFalse() throws Exception {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithModifications(revisions(false), ""));
        save(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        assertThat(loaded.getMaterialRevisions().getMaterialRevision(0).isChanged(), is(false));
    }

    @Test
    public void shouldSaveAndLoadPipeline() {
        Pipeline pipeline = new Pipeline("Test", BuildCause.createWithModifications(revisions(false), ""));
        pipeline.updateCounter(0);
        save(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        assertThat(loaded.getCounter(), is(1));
    }

    @Test
    public void shouldSaveMixedLabel() {
        Pipeline pipeline = new Pipeline("Test", "mingle-${COUNT}-${mingle}", BuildCause.createWithModifications(revisions(false), ""), new EnvironmentVariables());
        pipeline.updateCounter(0);
        save(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        assertThat(loaded.getLabel(), is("mingle-1-" + ModificationsMother.currentRevision()));
    }

    @Test
    public void shouldSaveAndLoadMaterialsWithName() {
        BuildCause buildCause = BuildCause.createWithModifications(revisions(false), "");
        Pipeline pipeline = new Pipeline("Test", buildCause);
        save(pipeline);
        Pipeline loaded = pipelineDao.loadPipeline(pipeline.getId());
        assertEquals(buildCause, loaded.getBuildCause());
    }


    @Test
    public void shouldFindPipelineThatPassedForStage() throws Exception {
        PipelineConfig config = PipelineMother.createPipelineConfig("pipeline", new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig()), "firstStage", "secondStage");
        Pipeline pipeline0 = dbHelper.newPipelineWithAllStagesPassed(config);
        dbHelper.updateNaturalOrder(pipeline0.getId(), 4.0);

        Pipeline pipeline1 = dbHelper.newPipelineWithFirstStagePassed(config);
        Stage stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.failStage(stage);
        stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.passStage(stage);
        dbHelper.updateNaturalOrder(pipeline1.getId(), 5.0);

        Pipeline pipeline2 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline2, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline2.getId(), 6.0);

        Pipeline pipeline3 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.updateNaturalOrder(pipeline3.getId(), 7.0);

        Pipeline pipeline4 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline4, config.get(1));
        dbHelper.cancelStage(stage);
        dbHelper.updateNaturalOrder(pipeline4.getId(), 8.0);

        Pipeline pipeline5 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.scheduleStage(pipeline5, config.get(1));
        dbHelper.updateNaturalOrder(pipeline5.getId(), 9.0);

        Pipeline pipeline6 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline6, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline6.getId(), 10.0);

        Pipeline pipeline = pipelineDao.findEarlierPipelineThatPassedForStage("pipeline", "secondStage", 10.0);
        assertThat(pipeline.getId(), is(pipeline1.getId()));
        assertThat(pipeline.getNaturalOrder(), is(5.0));
    }

    @Test
    public void shouldFindPipelineThatPassedForStageAcrossStageRerunsHavingPassedStagesOtherThanLatest() throws Exception {
        PipelineConfig config = PipelineMother.createPipelineConfig("pipeline", new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig()), "firstStage", "secondStage");
        Pipeline pipeline0 = dbHelper.newPipelineWithAllStagesPassed(config);
        dbHelper.updateNaturalOrder(pipeline0.getId(), 4.0);

        Pipeline pipeline1 = dbHelper.newPipelineWithFirstStagePassed(config);
        Stage stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.failStage(stage);
        stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.passStage(stage);
        dbHelper.updateNaturalOrder(pipeline1.getId(), 5.0);


        Pipeline pipeline5 = dbHelper.newPipelineWithAllStagesPassed(config);
        dbHelper.updateNaturalOrder(pipeline5.getId(), 9.0);
        Stage failedRerun = StageMother.scheduledStage("pipeline", pipeline5.getCounter(), "secondStage", 2, "job");
        failedRerun = stageDao.saveWithJobs(pipeline5, failedRerun);
        dbHelper.failStage(failedRerun);

        Pipeline pipeline6 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline6, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline6.getId(), 10.0);

        Pipeline pipeline = pipelineDao.findEarlierPipelineThatPassedForStage("pipeline", "secondStage", 10.0);
        assertThat(pipeline.getNaturalOrder(), is(5.0));
        assertThat(pipeline.getId(), is(pipeline1.getId()));
    }

    @Test
    public void shouldFindPipelineThatPassedForStageAcrossStageReruns() throws Exception {
        PipelineConfig config = PipelineMother.createPipelineConfig("pipeline", new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig()), "firstStage", "secondStage");
        Pipeline pipeline0 = dbHelper.newPipelineWithAllStagesPassed(config);
        dbHelper.updateNaturalOrder(pipeline0.getId(), 4.0);

        Pipeline pipeline1 = dbHelper.newPipelineWithFirstStagePassed(config);
        Stage stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.failStage(stage);
        stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.passStage(stage);
        dbHelper.updateNaturalOrder(pipeline1.getId(), 5.0);

        Stage passedStageRerun = StageMother.scheduledStage("pipeline", pipeline1.getCounter(), "secondStage", 2, "job");
        passedStageRerun = stageDao.saveWithJobs(pipeline1, passedStageRerun);
        dbHelper.passStage(passedStageRerun);

        Pipeline pipeline5 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline5, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline5.getId(), 9.0);

        Pipeline pipeline6 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline6, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline6.getId(), 10.0);

        Pipeline pipeline = pipelineDao.findEarlierPipelineThatPassedForStage("pipeline", "secondStage", 10.0);
        assertThat(pipeline.getId(), is(pipeline1.getId()));
        assertThat(pipeline.getNaturalOrder(), is(5.0));
    }

    @Test
    public void shouldReturnTheEarliestFailedPipelineIfThereAreNoPassedStageEver() throws Exception {
        PipelineConfig config = PipelineMother.createPipelineConfig("pipeline", new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig()), "firstStage", "secondStage");

        Pipeline pipeline2 = dbHelper.newPipelineWithFirstStagePassed(config);
        Stage stage = dbHelper.scheduleStage(pipeline2, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline2.getId(), 6.0);

        Pipeline pipeline3 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.updateNaturalOrder(pipeline3.getId(), 7.0);

        Pipeline pipeline4 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline4, config.get(1));
        dbHelper.cancelStage(stage);
        dbHelper.updateNaturalOrder(pipeline4.getId(), 8.0);

        Pipeline pipeline5 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.scheduleStage(pipeline5, config.get(1));
        dbHelper.updateNaturalOrder(pipeline5.getId(), 9.0);

        Pipeline pipeline6 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline6, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline6.getId(), 10.0);

        assertThat(pipelineDao.findEarlierPipelineThatPassedForStage("pipeline", "secondStage", 10.0), is(nullValue()));
    }

    @Test
    public void shouldReturnPageNumberOfThePageInWhichThePIMWouldBePresent() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("some-pipeline", "dev");
        Pipeline pipeline1 = schedulePipelineWithStages(mingleConfig);
        Pipeline pipeline2 = schedulePipelineWithStages(mingleConfig);
        Pipeline pipeline3 = schedulePipelineWithStages(mingleConfig);
        Pipeline pipeline4 = schedulePipelineWithStages(mingleConfig);
        Pipeline pipeline5 = schedulePipelineWithStages(mingleConfig);

        assertThat(pipelineDao.getPageNumberForCounter("some-pipeline", pipeline4.getCounter(), 1), is(2));
        assertThat(pipelineDao.getPageNumberForCounter("some-pipeline", pipeline5.getCounter(), 1), is(1));
        assertThat(pipelineDao.getPageNumberForCounter("some-pipeline", pipeline1.getCounter(), 2), is(3));
        assertThat(pipelineDao.getPageNumberForCounter("some-pipeline", pipeline2.getCounter(), 3), is(2));
        assertThat(pipelineDao.getPageNumberForCounter("some-pipeline", pipeline3.getCounter(), 10), is(1));
    }

    @Test
    public void shouldPauseExistingPipeline() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("some-pipeline", "dev");
        schedulePipelineWithStages(mingleConfig);

        pipelineDao.pause(mingleConfig.name().toString(), "cause", "by");

        PipelinePauseInfo actual = pipelineDao.pauseState(mingleConfig.name().toString());
        PipelinePauseInfo expected = new PipelinePauseInfo(true, "cause", "by");

        assertThat(actual.isPaused(), is(expected.isPaused()));
        assertThat(actual.getPauseCause(), is(expected.getPauseCause()));
        assertThat(actual.getPauseBy(), is(expected.getPauseBy()));
        assertNotNull(actual.getPausedAt());
    }

    @Test
    public void shouldPauseExistingPipelineCaseInsensitive() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("some-pipeline", "dev");
        schedulePipelineWithStages(mingleConfig);

        pipelineDao.pause(mingleConfig.name().toString(), "cause", "by");

        PipelinePauseInfo actual = pipelineDao.pauseState(mingleConfig.name().toString().toUpperCase());
        PipelinePauseInfo expected = new PipelinePauseInfo(true, "cause", "by");

        assertThat(actual.isPaused(), is(expected.isPaused()));
        assertThat(actual.getPauseCause(), is(expected.getPauseCause()));
        assertThat(actual.getPauseBy(), is(expected.getPauseBy()));
        assertNotNull(actual.getPausedAt());
    }

    @Test
    public void shouldUnPauseAPausedPipeline() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("some-pipeline", "dev");
        schedulePipelineWithStages(mingleConfig);
        pipelineDao.pause(mingleConfig.name().toString(), "cause", "by");

        pipelineDao.unpause(mingleConfig.name().toString());

        PipelinePauseInfo actual = pipelineDao.pauseState(mingleConfig.name().toString());
        PipelinePauseInfo expected = new PipelinePauseInfo(false, null, null);
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldUnPauseAPausedPipelineCaseInsensitive() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("some-pipeline", "dev");
        schedulePipelineWithStages(mingleConfig);
        pipelineDao.pause(mingleConfig.name().toString(), "cause", "by");

        pipelineDao.unpause(mingleConfig.name().toString().toUpperCase());

        PipelinePauseInfo actual = pipelineDao.pauseState(mingleConfig.name().toString());
        PipelinePauseInfo expected = new PipelinePauseInfo(false, null, null);
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldPauseNewPipeline() {
        PipelineConfig newlyAddedPipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("newly-added-pipeline-config", "dev");

        pipelineDao.pause(newlyAddedPipelineConfig.name().toString(), "cause", "by");

        PipelinePauseInfo actual = pipelineDao.pauseState(newlyAddedPipelineConfig.name().toString());
        PipelinePauseInfo expected = new PipelinePauseInfo(true, "cause", "by");

        assertThat(actual.isPaused(), is(expected.isPaused()));
        assertThat(actual.getPauseCause(), is(expected.getPauseCause()));
        assertThat(actual.getPauseBy(), is(expected.getPauseBy()));
        assertNotNull(actual.getPausedAt());
    }

    @Test
    public void shouldReturnCurrentPauseStateOfPipeline() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("some-pipeline", "dev");
        schedulePipelineWithStages(mingleConfig);

        PipelinePauseInfo expected = new PipelinePauseInfo(false, null, null);
        PipelinePauseInfo actual = pipelineDao.pauseState(mingleConfig.name().toString());
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldReturnCurrentPauseStateOfPipelineCaseInsensitive() throws Exception {
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials("some-pipeline", "dev");
        schedulePipelineWithStages(mingleConfig);

        pipelineDao.pause(mingleConfig.name().toString(), "really good reason", "me");

        PipelinePauseInfo actual = pipelineDao.pauseState(mingleConfig.name().toString().toUpperCase());
        assertThat(actual.isPaused(), is(true));
        assertThat(actual.getPauseCause(), is("really good reason"));
        assertThat(actual.getPauseBy(), is("me"));
        assertNotNull(actual.getPausedAt());
    }

    @Test
    public void shouldUpdateCounter_WhenPipelineRowIsPresentWhichWasInsertedByPauseAction() {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);
        Username userNameAdmin = new Username(new CaseInsensitiveString("admin"));
        pipelinePauseService.pause(pipelineName, "some-cause", userNameAdmin); // Pause and unpause so that an entry exists for that pipeline
        pipelinePauseService.unpause(pipelineName);

        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, new TimeProvider());

        assertThat(pipelineDao.getCounterForPipeline(pipeline.getName()), is(1));
    }

    @Test
    public void shouldIncrementCounter_WhenPipelineRowIsPresentWhichWasInsertedByPauseAction() throws SQLException {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);

        Pipeline pipeline = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig); // Counter is 1
        dbHelper.newPipelineWithAllStagesPassed(pipelineConfig); // Counter should be incremented

        assertThat(pipelineDao.getCounterForPipeline(pipeline.getName().toUpperCase()), is(pipeline.getCounter() + 1));
    }

    @Test
    public void shouldInsertCounter_WhenPipelineRowIsNotPresent() {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);

        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, new TimeProvider());

        assertThat(pipelineDao.getCounterForPipeline(pipeline.getName()), is(1));
    }

    @Test
    public void shouldReturnStageIdIfAStageOfPipelineIdPassed() throws SQLException {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);

        Pipeline pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.pass(pipeline);
        String stage = pipelineConfig.get(0).name().toString();
        assertThat(pipelineDao.latestPassedStageIdentifier(pipeline.getId(), stage), is(new StageIdentifier(pipelineName, pipeline.getCounter(), pipeline.getLabel(), stage, "1")));
    }

    @Test
    public void shouldReturnNullStageIdIfStageOfPipelineIdNeverPassed() throws SQLException {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);
        Pipeline pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.failStage(pipeline.getFirstStage());
        String stage = pipelineConfig.get(0).name().toString();
        assertThat(pipelineDao.latestPassedStageIdentifier(pipeline.getId(), stage), is(StageIdentifier.NULL));
    }

    @Test
    public void shouldReturnStageIdOfPassedRunIfThereWereMultipleRerunsOfAStageAndOneOfThemPassed() throws SQLException {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);

        Pipeline pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.pass(pipeline);
        Stage stage = dbHelper.scheduleStage(pipeline, pipelineConfig.getFirstStageConfig());
        dbHelper.cancelStage(stage);
        String stageName = pipelineConfig.get(0).name().toString();
        assertThat(pipelineDao.latestPassedStageIdentifier(pipeline.getId(), stageName), is(new StageIdentifier(pipelineName, pipeline.getCounter(), pipeline.getLabel(), stageName, "1")));
    }

    @Test
    public void shouldReturnLatestPassedStageIdentifierIfMultipleRunsOfTheStageHadPassed() throws SQLException {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);
        Pipeline pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.pass(pipeline);
        Stage stage = dbHelper.scheduleStage(pipeline, pipelineConfig.getFirstStageConfig());
        dbHelper.passStage(stage);
        stage = dbHelper.scheduleStage(pipeline, pipelineConfig.getFirstStageConfig());
        dbHelper.cancelStage(stage);

        String stageName = stage.getName();
        assertThat(pipelineDao.latestPassedStageIdentifier(pipeline.getId(), stageName), is(new StageIdentifier(pipelineName, pipeline.getCounter(), pipeline.getLabel(), stageName, "2")));
    }

    @Test
    public void shouldReturnPipelineWithBuildCauseForJobId() {
        String pipelineName = "P1";
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages(pipelineName, "S1");
        String username = "username";
        BuildCause manualForced = BuildCause.createManualForced(modifyOneFile(pipelineConfig), new Username(new CaseInsensitiveString(username)));
        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, manualForced, username, new TimeProvider());
        dbHelper.pass(pipeline);
        long jobId = pipeline.getStages().get(0).getJobInstances().get(0).getId();
        Pipeline pipelineFromDB = pipelineDao.pipelineWithMaterialsAndModsByBuildId(jobId);
        assertThat(pipelineFromDB.getBuildCause().getApprover(), is(username));
        assertThat(pipelineFromDB.getBuildCause().getBuildCauseMessage(), is("Forced by username"));
        assertThat(pipelineFromDB.getName(), is(pipelineName));
    }

    @Test
    public void shouldThrowExceptionWhenBuildCauseIsAskedForANonExistentPipeline() {
        try {
            pipelineDao.findBuildCauseOfPipelineByNameAndCounter("foo", 1);
            fail("should have thrown RecordNotFoundException");
        } catch (Exception e) {
            assertThat(e instanceof RecordNotFoundException, is(true));
            assertThat(e.getMessage(), is("Pipeline foo with counter 1 was not found"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenBuildCauseIsAskedForAPipelineWithInvalidCounter() {
        String pipelineName = "P1";
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStages(pipelineName, "S1");
        String username = "username";
        BuildCause manualForced = BuildCause.createManualForced(modifyOneFile(pipelineConfig), new Username(new CaseInsensitiveString(username)));
        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, manualForced, username, new TimeProvider());
        dbHelper.pass(pipeline);
        BuildCause buildCause = pipelineDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, 1);
        assertThat(buildCause, is(notNullValue()));
        try {
            pipelineDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, 10);
            fail("should have thrown RecordNotFoundException");
        } catch (Exception e) {
            assertThat(e instanceof RecordNotFoundException, is(true));
            assertThat(e.getMessage(), is("Pipeline P1 with counter 10 was not found"));
        }
    }

    @Test
    public void shouldReturnListOfPipelineIdentifiersForDownstreamPipelinesBasedOnARunOfUpstreamPipeline() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(g1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));

        String p1_1 = u.runAndPass(p1, "g_1");
        String p2_1 = u.runAndPass(p2, p1_1);
        String p2_2 = u.runAndPass(p2, p1_1);

        List<PipelineIdentifier> pipelineIdentifiers = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial(p2.config.name().toString(),
                new PipelineIdentifier(p1.config.name().toString(), 1));
        assertThat(pipelineIdentifiers, hasSize(2));
        assertThat(pipelineIdentifiers, contains(new PipelineIdentifier(p2.config.name().toString(), 2, "2"), new PipelineIdentifier(p2.config.name().toString(), 1, "1")));
    }

    @Test
    public void shouldReturnEmptyListOfPipelineIdentifiersForUnRunDownstreamPipelinesBasedOnARunOfUpstreamPipeline() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(g1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));

        String p1_1 = u.runAndPass(p1, "g_1");

        List<PipelineIdentifier> pipelineIdentifiers = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial(p2.config.name().toString(),
                new PipelineIdentifier(p1.config.name().toString(), 1));
        assertThat(pipelineIdentifiers, is(Matchers.empty()));
    }

    @Test
    public void shouldReturnListOfPipelineIdentifiersBasedOnAMaterialRevisionCorrectly() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1", "g_2", "g_3");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(g1));

        String p1_1 = u.runAndPass(p1, "g_2");
        String p1_2 = u.runAndPass(p1, "g_3");
        String p1_3 = u.runAndPass(p1, "g_2");

        MaterialInstance g1Instance = materialRepository.findMaterialInstance(g1);
        List<PipelineIdentifier> pipelineIdentifiers = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial(p1.config.name().toString(), g1Instance, "g_2");

        assertThat(pipelineIdentifiers.size(), is(2));
        assertThat(pipelineIdentifiers, contains(new PipelineIdentifier(p1.config.name().toString(), 3, "3"), new PipelineIdentifier(p1.config.name().toString(), 1, "1")));

        pipelineIdentifiers = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial(p1.config.name().toString(), g1Instance, "g_1");

        assertThat(pipelineIdentifiers, is(Matchers.empty()));
    }

    @Test
    /* THIS IS A BUG IN VSM. STAGE RERUNS ARE NOT SUPPORTED AND DOWNSTREAMS SHOW THE RUNS MADE OUT OF PREVIOUS STAGE RUN. CHANGE TEST EXPECTATION WHEN BUG IS FIXED POST 13.2 [Mingle #7385] (DUCK & SACHIN) */
    public void shouldReturnListOfDownstreamPipelineIdentifiersForARunOfUpstreamPipeline_AlthoughUpstreamHasHadAStageReRun() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(g1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));

        Pipeline p1_1_s_1 = u.runAndPassAndReturnPipelineInstance(p1, u.d(0), "g_1");
        Pipeline p2_1 = u.runAndPassAndReturnPipelineInstance(p2, u.d(1), p1_1_s_1.getStages().first().stageLocator());
        String p1_1_s_2 = u.rerunStageAndCancel(p1_1_s_1, p1.config.get(0));

        List<PipelineIdentifier> pipelineIdentifiers = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial(p2.config.name().toString(),
                new PipelineIdentifier(p1.config.name().toString(), 1));
        assertThat(pipelineIdentifiers, hasSize(1));
        assertThat(pipelineIdentifiers, hasItem(new PipelineIdentifier(p2.config.name().toString(), 1, "1")));
    }

    @Test
    public void shouldInvalidateCachedPipelineHistoryViaNameAndCounterUponStageChangeCaseInsensitively() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", "s1", u.m(g1));
        Pipeline p1_1 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.pass(p1_1);
        PipelineInstanceModel pim1 = pipelineDao.findPipelineHistoryByNameAndCounter(p1.config.name().toUpper().toString(), 1); //prime cache
        scheduleService.rerunStage(p1_1.getName(), p1_1.getCounter(), p1_1.getStages().get(0).getName());

        PipelineInstanceModel pim2 = pipelineDao.findPipelineHistoryByNameAndCounter(p1.config.name().toUpper().toString(), 1);

        assertThat(pim2, is(not(pim1)));
        assertThat(pim2.getStageHistory().get(0).getIdentifier().getStageCounter(), is("2"));
    }

    @Test
    public void shouldLoadHistoryForDashboard() throws Exception {
        /*
         *   P1 [S1 (J1), S2(J1), S3(J1, J2)]
         *   P2 [S1 (J1)]
         *
         * */
        String pipeline1 = "p1";
        String pipeline2 = "p2";
        PipelineConfig pipelineConfig1 = PipelineConfigMother.pipelineConfig(pipeline1);
        pipelineConfig1.getStages().clear();
        pipelineConfig1.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials("s1"));
        pipelineConfig1.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials("s2"));
        StageConfig s2Config = StageConfigMother.twoBuildPlansWithResourcesAndMaterials("s3");
        pipelineConfig1.add(s2Config);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith(pipelineConfig1);
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith(PipelineConfigMother.createPipelineConfig(pipeline2, "p2s1", "j1"));

        // Pipeline 1 Counter 1: All stages green
        Pipeline p1_1 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.pass(p1_1);

        // Pipeline 1 Counter 2: S1, S2 green, S3 running. J1 failed, J2 scheduled
        Pipeline p1_2 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.passStage(p1_2.getFirstStage());
        Stage p1_2_s2_1 = dbHelper.scheduleStage(p1_2, pipelineConfig1.getStage("s2"));
        dbHelper.passStage(p1_2_s2_1);
        Stage p1_2_s3_1 = dbHelper.scheduleStage(p1_2, pipelineConfig1.getStage("s3"));
        dbHelper.failJob(p1_2_s3_1, p1_2_s3_1.findJob("WinBuild"));

        // Pipeline 1 Counter 3: S1 green, S2 running
        Pipeline p1_3 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.passStage(p1_3.getFirstStage());
        Stage p1_3_s2_1 = dbHelper.scheduleStage(p1_3, pipelineConfig1.getStage("s2"));

        // Pipeline 1 Counter 4: S1 scheduled
        Pipeline p1_4 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));

        // Pipeline 2 Counter 1: All stages green
        Pipeline p2_1 = dbHelper.schedulePipeline(p2.config, new TestingClock(new Date()));
        dbHelper.pass(p2_1);

        PipelineInstanceModels pipelineInstanceModels = pipelineDao.loadHistoryForDashboard(Arrays.asList(pipeline1, pipeline2)); // Load for all pipelines
        assertThat(pipelineInstanceModels.size(), is(4));
        PipelineInstanceModels allRunningInstancesOfPipeline1 = pipelineInstanceModels.findAll(pipeline1);
        assertThat(allRunningInstancesOfPipeline1.size(), is(3));
        assertThat(allRunningInstancesOfPipeline1.get(0).getCounter(), is(4));
        assertThat(allRunningInstancesOfPipeline1.get(1).getCounter(), is(3));
        assertThat(allRunningInstancesOfPipeline1.get(2).getCounter(), is(2));

        PipelineInstanceModels allRunningInstancesOfPipeline2 = pipelineInstanceModels.findAll(pipeline2);
        assertThat(allRunningInstancesOfPipeline2.size(), is(1));
        assertThat(allRunningInstancesOfPipeline2.get(0).getCounter(), is(1));

        PipelineInstanceModels pipelineInstanceModelsForPipeline1 = pipelineDao.loadHistoryForDashboard(Arrays.asList(pipeline1)); // Load for single pipeline
        assertThat(pipelineInstanceModelsForPipeline1.size(), is(3));
        assertThat(pipelineInstanceModelsForPipeline1.get(0).getCounter(), is(4));
        assertThat(pipelineInstanceModelsForPipeline1.get(1).getCounter(), is(3));
        assertThat(pipelineInstanceModelsForPipeline1.get(2).getCounter(), is(2));
    }

    @Test
    public void ensureActivePipelineCacheUsedByOldDashboardIsCaseInsensitiveWRTPipelineNames() {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");

        ScheduleTestUtil.AddedPipeline pipeline1 = u.saveConfigWith("pipeline1", u.m(g1));
        Pipeline pipeline1_1 = dbHelper.schedulePipeline(pipeline1.config, new TestingClock(new Date()));
        pipelineDao.loadActivePipelines(); //to initialize cache

        dbHelper.pass(pipeline1_1);
        pipelineDao.stageStatusChanged(pipeline1_1.getFirstStage());
        assertThat(getActivePipelinesForPipelineName(pipeline1).count(), is(1L));
        assertThat(getActivePipelinesForPipelineName(pipeline1).findFirst().get().getName(), is(pipeline1.config.name().toString()));

        configHelper.removePipeline(pipeline1.config.name().toString());
        ScheduleTestUtil.AddedPipeline p1ReincarnatedWithDifferentCase = u.saveConfigWith("PIPELINE1", u.m(g1));
        Pipeline pipelineReincarnatedWithDifferentCase_1 = dbHelper.schedulePipeline(p1ReincarnatedWithDifferentCase.config, new TestingClock(new Date()));
        pipelineDao.loadActivePipelines(); //to initialize cache
        pipelineDao.stageStatusChanged(pipelineReincarnatedWithDifferentCase_1.getFirstStage());

        assertThat(getActivePipelinesForPipelineName(p1ReincarnatedWithDifferentCase).count(), is(1L));
        assertThat(getActivePipelinesForPipelineName(p1ReincarnatedWithDifferentCase).findFirst().get().getName(), is(p1ReincarnatedWithDifferentCase.config.name().toString()));
    }

    @Test
    public void shouldRemoveDuplicateEntriesForPipelineCounterFromDbForAGivenPipelineName() throws SQLException {
        String pipelineName = "Pipeline-Name";
        configHelper.addPipeline(pipelineName, "stage-name");
        pipelineDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toLowerCase()).and("count", 10).asMap());
        pipelineDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toUpperCase()).and("count", 20).asMap());
        pipelineDao.getSqlMapClientTemplate().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName).and("count", 30).asMap());
        assertThat(pipelineDao.getPipelineNamesWithMultipleEntriesForLabelCount().size(), is(1));
        assertThat(pipelineDao.getPipelineNamesWithMultipleEntriesForLabelCount().get(0).equalsIgnoreCase(pipelineName), is(true));

        pipelineDao.deleteOldPipelineLabelCountForPipelineInConfig(pipelineName);
        assertThat(pipelineDao.getPipelineNamesWithMultipleEntriesForLabelCount().isEmpty(), is(true));
        assertThat(pipelineDao.getCounterForPipeline(pipelineName), is(30));
        assertThat(pipelineDao.getCounterForPipeline(pipelineName.toLowerCase()), is(30));
        assertThat(pipelineDao.getCounterForPipeline(pipelineName.toUpperCase()), is(30));
    }

    private Stream<PipelineInstanceModel> getActivePipelinesForPipelineName(ScheduleTestUtil.AddedPipeline pipeline1) {
        return pipelineDao.loadActivePipelines().stream().filter(new Predicate<PipelineInstanceModel>() {
            @Override
            public boolean test(PipelineInstanceModel pipelineInstanceModel) {
                return pipelineInstanceModel.getName().equalsIgnoreCase(pipeline1.config.name().toString());
            }
        });
    }

    @Test
    public void shouldLoadMostRecentPipelineIdentifier() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");

        String pipelineName = "p1";
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith(pipelineName, "s1", u.m(g1));
        Pipeline p1_1 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.pass(p1_1);
        Pipeline p2_1 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.pass(p2_1);

        PipelineIdentifier pipelineIdentifier = pipelineDao.mostRecentPipelineIdentifier(pipelineName);
        assertThat(pipelineIdentifier.getLabel(), is(p2_1.getLabel()));
        assertThat(pipelineIdentifier.getName(), is(p2_1.getName()));
        assertThat(pipelineIdentifier.getCounter(), is(p2_1.getCounter()));
    }

    @Test
    public void shouldReturnLatestAndOldestPipelineRunID() {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");

        String pipelineName = "p1";
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith(pipelineName, "s1", u.m(g1));
        Pipeline p1_1 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.pass(p1_1);
        Pipeline p2_1 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.pass(p2_1);
        Pipeline p3_1 = dbHelper.schedulePipeline(p1.config, new TestingClock(new Date()));
        dbHelper.pass(p3_1);
        List<Long> oldestAndLatestPipelineId = pipelineDao.getOldestAndLatestPipelineId(pipelineName);

        assertThat(oldestAndLatestPipelineId.get(0), is(p3_1.getId()));
        assertThat(oldestAndLatestPipelineId.get(1), is(p1_1.getId()));
    }

    @Test
    public void shouldReturnLatestPipelineHistory() throws SQLException {
        String pipelineName = "some-pipeline";
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "dev", "ft");

        Pipeline pipeline1 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline2 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline3 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline4 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline5 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        List<CaseInsensitiveString> allPipelineNames = goConfigDao.load().getAllPipelineNames();
        if (!allPipelineNames.contains(new CaseInsensitiveString("twist"))) {
            goConfigDao.addPipeline(pipelineConfig, "pipelinesqlmapdaotest");
        }

        PipelineInstanceModels pipelineInstanceModels = pipelineDao.loadHistory(pipelineName, FeedModifier.Latest, 0, 3);

        assertThat(pipelineInstanceModels.size(), is(3));
        assertThat(pipelineInstanceModels.get(0).getId(), is(pipeline5.getId()));
        assertThat(pipelineInstanceModels.get(1).getId(), is(pipeline4.getId()));
        assertThat(pipelineInstanceModels.get(2).getId(), is(pipeline3.getId()));
    }

    @Test
    public void shouldReturnPipelineHistoryAfterTheSuppliedCursor() throws SQLException {
        String pipelineName = "pipeline-name" + UUID.randomUUID().toString();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "dev", "ft");

        Pipeline pipeline1 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline2 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline3 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline4 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline5 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        List<CaseInsensitiveString> allPipelineNames = goConfigDao.load().getAllPipelineNames();
        if (!allPipelineNames.contains(new CaseInsensitiveString("twist"))) {
            goConfigDao.addPipeline(pipelineConfig, "pipelinesqlmapdaotest");
        }

        PipelineInstanceModels pipelineInstanceModels = pipelineDao.loadHistory(pipelineName, FeedModifier.After, pipeline3.getId(), 3);

        assertThat(pipelineInstanceModels.size(), is(2));
        assertThat(pipelineInstanceModels.get(0).getId(), is(pipeline2.getId()));
        assertThat(pipelineInstanceModels.get(1).getId(), is(pipeline1.getId()));
    }

    @Test
    public void shouldReturnPipelineHistoryBeforeTheSuppliedCursor() throws SQLException {
        String pipelineName = "pipeline-name" + UUID.randomUUID().toString();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "dev", "ft");

        Pipeline pipeline1 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline2 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline3 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline4 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline pipeline5 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        List<CaseInsensitiveString> allPipelineNames = goConfigDao.load().getAllPipelineNames();
        if (!allPipelineNames.contains(new CaseInsensitiveString("twist"))) {
            goConfigDao.addPipeline(pipelineConfig, "pipelinesqlmapdaotest");
        }

        PipelineInstanceModels pipelineInstanceModels = pipelineDao.loadHistory(pipelineName, FeedModifier.Before, pipeline3.getId(), 3);

        assertThat(pipelineInstanceModels.size(), is(2));
        assertThat(pipelineInstanceModels.get(0).getId(), is(pipeline5.getId()));
        assertThat(pipelineInstanceModels.get(1).getId(), is(pipeline4.getId()));
    }

    public static MaterialRevisions revisions(boolean changed) {
        MaterialRevisions revisions = new MaterialRevisions();
        List<Modification> modifications = new ArrayList<>();
        modifications.add(ModificationsMother.oneModifiedFile(ModificationsMother.currentRevision()));
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial("http://mingle.com");
        svnMaterial.setName(new CaseInsensitiveString("mingle"));
        MaterialRevision materialRevision = new MaterialRevision(svnMaterial, changed, modifications.toArray(new Modification[modifications.size()]));
        revisions.addRevision(materialRevision);
        return revisions;
    }

    private void assertNotInserted(long instanceId) {
        assertThat("Already thinks it's inserted", instanceId, is(NOT_PERSISTED));
    }

    private void assertIsInserted(long instanceId) {
        assertThat("Not inserted", instanceId, is(not(0L)));
    }

    private void assertModifications(Pipeline pipeline) {
        assertThat(pipeline.getBuildCause().getMaterialRevisions().totalNumberOfModifications(), is(1));
    }

    private class ModificationsCollector extends ModificationVisitorAdapter {
        private List<Modification> mods = new ArrayList<>();

        @Override
        public void visit(Modification modification) {
            mods.add(modification);
        }

        public Modification first() {
            return mods.get(0);
        }

        public int numberOfModifications() {
            return mods.size();
        }
    }
}
