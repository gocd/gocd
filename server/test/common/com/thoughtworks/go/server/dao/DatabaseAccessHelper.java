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

package com.thoughtworks.go.server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.BuildPlanMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.ManualBuild;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.log4j.Logger;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.AmbiguousTableNameException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;

@Component
public class DatabaseAccessHelper extends HibernateDaoSupport {
    private static final Logger LOG = Logger.getLogger(DatabaseAccessHelper.class);

    private IDatabaseTester databaseTester;
    private StageDao stageDao;
    private PipelineSqlMapDao pipelineDao;
    private JobInstanceDao jobInstanceDao;
    private PropertyDao propertyDao;
    private PipelineTimeline pipelineTimeline;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;

    public static final String AGENT_UUID = "123456789-123";
    private DataSource dataSource;
    private UserDao userDao;
    private SqlMapClient sqlMapClient;
    private MaterialRepository materialRepository;
    private GoCache goCache;
    private PipelineService pipelineService;
    private String md5 = "md5-test";
    private InstanceFactory instanceFactory;

    @Deprecated // Should not be creating a new spring context for every test
    public DatabaseAccessHelper() throws AmbiguousTableNameException {
        ClassPathXmlApplicationContext context = createDataContext();
        dataSource = (DataSource) context.getBean("dataSource");
        initialize(dataSource);
    }

    private ClassPathXmlApplicationContext createDataContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath*:WEB-INF/applicationContext-dataLocalAccess.xml");
        this.stageDao = (StageSqlMapDao) context.getBean("stageDao");
        this.jobInstanceDao = (JobInstanceDao) context.getBean("buildInstanceDao");
        this.propertyDao = (PropertyDao) context.getBean("propertyDao");
        this.pipelineDao = (PipelineSqlMapDao) context.getBean("pipelineDao");
        this.materialRepository = (MaterialRepository) context.getBean("materialRepository");
        this.goCache = (GoCache) context.getBean("goCache");
        this.instanceFactory = (InstanceFactory) context.getBean("instanceFactory");
        setSessionFactory((SessionFactory) context.getBean("sessionFactory"));
        return context;
    }

    @Deprecated //use Autowired version
    public DatabaseAccessHelper(DataSource dataSource) throws AmbiguousTableNameException {
        this.dataSource = dataSource;
        initialize(dataSource);
    }

    @Autowired
    public DatabaseAccessHelper(DataSource dataSource,
                                SqlMapClient sqlMapClient,
                                StageDao stageDao,
                                JobInstanceDao jobInstanceDao,
                                PropertyDao propertyDao,
                                PipelineDao pipelineDao,
                                MaterialRepository materialRepository,
                                SessionFactory sessionFactory,
                                PipelineTimeline pipelineTimeline,
                                TransactionTemplate transactionTemplate,
                                TransactionSynchronizationManager transactionSynchronizationManager,
                                GoCache goCache,
                                PipelineService pipelineService, InstanceFactory instanceFactory) throws AmbiguousTableNameException {
        this.dataSource = dataSource;
        this.sqlMapClient = sqlMapClient;
        this.stageDao = stageDao;
        this.jobInstanceDao = jobInstanceDao;
        this.propertyDao = propertyDao;
        this.pipelineTimeline = pipelineTimeline;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.goCache = goCache;
        this.pipelineService = pipelineService;
        this.instanceFactory = instanceFactory;
        this.pipelineDao = (PipelineSqlMapDao) pipelineDao;
        this.materialRepository = materialRepository;
        setSessionFactory(sessionFactory);
        initialize(dataSource);
    }

    private void initialize(DataSource dataSource) throws AmbiguousTableNameException {
        databaseTester = new DataSourceDatabaseTester(dataSource);
        databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        databaseTester.setTearDownOperation(DatabaseOperation.DELETE_ALL);
        DefaultDataSet dataSet = new DefaultDataSet();
        dataSet.addTable(new DefaultTable("agents"));

        dataSet.addTable(new DefaultTable("pipelines"));
        dataSet.addTable(new DefaultTable("materials"));
        dataSet.addTable(new DefaultTable("modifications"));
        dataSet.addTable(new DefaultTable("pipelineMaterialRevisions"));
        dataSet.addTable(new DefaultTable("modifiedFiles"));

        dataSet.addTable(new DefaultTable("notificationfilters"));
        dataSet.addTable(new DefaultTable("users"));
        dataSet.addTable(new DefaultTable("artifactPropertiesGenerator"));
        dataSet.addTable(new DefaultTable("stages"));
        dataSet.addTable(new DefaultTable("buildCauseBuffer"));
        dataSet.addTable(new DefaultTable("pipelineLabelCounts"));
        dataSet.addTable(new DefaultTable("environmentVariables"));
        dataSet.addTable(new DefaultTable("properties"));
        dataSet.addTable(new DefaultTable("artifactPlans"));
        dataSet.addTable(new DefaultTable("buildStateTransitions"));
        dataSet.addTable(new DefaultTable("resources"));
        dataSet.addTable(new DefaultTable("builds"));

        dataSet.addTable(new DefaultTable("oauthclients"));
        dataSet.addTable(new DefaultTable("oauthauthorizations"));
        dataSet.addTable(new DefaultTable("oauthtokens"));

        dataSet.addTable(new DefaultTable("gadgetOauthClients"));
        dataSet.addTable(new DefaultTable("gadgetOauthAuthorizationCodes"));
        dataSet.addTable(new DefaultTable("gadgetOauthAccessTokens"));
        dataSet.addTable(new DefaultTable("stageArtifactCleanupProhibited"));
        dataSet.addTable(new DefaultTable("serverBackups"));

        databaseTester.setDataSet(dataSet);
    }

    public void onSetUp() throws Exception {
        databaseTester.onSetup();
        pipelineTimeline.clearWhichIsEvilAndShouldNotBeUsedInRealWorld();
        if (sqlMapClient != null) {
            sqlMapClient.flushDataCache();
        }
    }

    public void onTearDown() throws Exception {
        databaseTester.onTearDown();
        goCache.clear();
    }

    public TransactionTemplate txTemplate() {
        return transactionTemplate;
    }

    public TransactionSynchronizationManager txSynchronizationManager() {
        return transactionSynchronizationManager;
    }

    public JobInstanceDao getBuildInstanceDao() {
        return jobInstanceDao;
    }

    public StageDao getStageDao() {
        return stageDao;
    }

    public PropertyDao getPropertyDao() {
        return propertyDao;
    }

    public PipelineDao getPipelineDao() {
        return pipelineDao;
    }

    public void pass(Pipeline pipeline) {
        for (Stage stage : pipeline.getStages()) {
            passStage(stage);
        }
    }

    public long getScheduledStageId(String pipelineName, String stageName) throws SQLException {
        Pipeline pipeline = saveTestPipeline(pipelineName, stageName);
        return pipeline.getStages().get(0).getId();
    }

    public Pipeline saveTestPipeline(String pipelineName, String stageName, String... jobConfigNames)
            throws SQLException {
        PipelineConfig pipelineConfig = configurePipeline(pipelineName, stageName, jobConfigNames);
        Pipeline pipeline = scheduleWithFileChanges(pipelineConfig);
        pipeline = savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

    public Pipeline savePipelineWithStagesAndMaterials(Pipeline pipeline) {
        saveRevs(pipeline.getBuildCause().getMaterialRevisions());
        return save(pipeline);
    }

    public Pipeline savePipelineWithMaterials(Pipeline pipeline) {
        saveRevs(pipeline.getBuildCause().getMaterialRevisions());
        return save(pipeline);
    }

    public Pipeline save(Pipeline pipeline) {
        return pipelineDao.saveWithStages(pipeline);
    }

    private PipelineConfig configurePipeline(String pipelineName, String stageName, String... jobConfigNames) {
        final String[] defaultBuildPlanNames = {"functional", "unit"};
        jobConfigNames = jobConfigNames.length == 0 ? defaultBuildPlanNames : jobConfigNames;
        StageConfig stageConfig = StageConfigMother.stageConfig(stageName,
                BuildPlanMother.withBuildPlans(jobConfigNames));
        MaterialConfigs materialConfigs = MaterialConfigsMother.multipleMaterialConfigs();
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs, stageConfig);
        return pipelineConfig;
    }

    private Pipeline scheduleWithFileChanges(PipelineConfig pipelineConfig) {
        BuildCause buildCause = BuildCause.createWithModifications(modifyOneFile(pipelineConfig), "");
        saveRevs(buildCause.getMaterialRevisions());
        return instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(
                GoConstants.DEFAULT_APPROVED_BY), md5, new TimeProvider());
    }

    public Pipeline saveTestPipelineWithoutSchedulingBuilds(String pipelineName, String stageName,
                                                            String... jobConfigNames)
            throws SQLException {
        PipelineConfig pipelineConfig = configurePipeline(pipelineName, stageName, jobConfigNames);
        Pipeline pipeline = scheduleWithFileChanges(pipelineConfig);
        clearAllBuildInstances(pipeline);
        return savePipelineWithStagesAndMaterials(pipeline);
    }

    private void clearAllBuildInstances(Pipeline pipeline) {
        pipeline.getFirstStage().setJobInstances(new JobInstances());
    }

    public Pipeline rescheduleTestPipeline(String pipelineName, String stageName, String userName) throws SQLException {
        String[] jobConfigNames = new String[]{};
        PipelineConfig pipelineConfig = configurePipeline(pipelineName, stageName, jobConfigNames);

        BuildCause buildCause = BuildCause.createManualForced(modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(pipelineConfig.materialConfigs()),
                ModificationsMother.currentRevision()), Username.ANONYMOUS);

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(
                GoConstants.DEFAULT_APPROVED_BY), md5, new TimeProvider());
        return savePipelineWithStagesAndMaterials(pipeline);
    }

    @Deprecated // Only actually passes the first stage. Use newPipelineWithAllStagesPassed instead
    public Pipeline passPipeline(Pipeline pipeline) {
        for (Stage stage : pipeline.getStages()) {
            passStage(stage);
        }
        Stages loadedStages = new Stages();
        for (Stage stage : pipeline.getStages()) {
            loadedStages.add(stageDao.stageById(stage.getId()));
        }
        Pipeline loadedPipeline = this.pipelineDao.loadPipeline(pipeline.getId());
        loadedPipeline.setStages(loadedStages);
        return loadedPipeline;
    }

    public Pipeline newPipelineWithFirstStagePassed(PipelineConfig config) throws SQLException {
        Pipeline pipeline = instanceFactory.createPipelineInstance(config,
                BuildCause.createManualForced(modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(config.materialConfigs()), ModificationsMother.nextRevision()), Username.ANONYMOUS),
                new DefaultSchedulingContext(
                        GoConstants.DEFAULT_APPROVED_BY), md5, new TimeProvider());
        saveMaterialsWIthPassedStages(pipeline);
        return pipeline;
    }

    public void saveMaterialsWIthPassedStages(Pipeline pipeline) {
        savePipelineWithStagesAndMaterials(pipeline);
        passStage(pipeline.getFirstStage());
    }

    public Pipeline newPipelineWithFirstStageFailed(PipelineConfig config) throws SQLException {
        Pipeline pipeline = instanceFactory.createPipelineInstance(config, BuildCause.createManualForced(modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(config.materialConfigs()),
                ModificationsMother.currentRevision()), Username.ANONYMOUS),
                new DefaultSchedulingContext(
                        GoConstants.DEFAULT_APPROVED_BY), md5, new TimeProvider());
        savePipelineWithStagesAndMaterials(pipeline);
        failStage(pipeline.getFirstStage());
        return pipeline;
    }

    public Pipeline newPipelineWithFirstStageScheduled(PipelineConfig config) throws SQLException {
        Pipeline pipeline = instanceFactory.createPipelineInstance(config,
                BuildCause.createManualForced(modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(config.materialConfigs()), ModificationsMother.nextRevision()), Username.ANONYMOUS),
                new DefaultSchedulingContext(
                        GoConstants.DEFAULT_APPROVED_BY), md5, new TimeProvider());
        savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

    public Pipeline newPipelineWithAllStagesPassed(PipelineConfig config) throws SQLException {
        Pipeline pipeline = newPipelineWithFirstStagePassed(config);
        for (StageConfig stageConfig : config) {
            if (config.first().equals(stageConfig)) {
                continue;
            }
            Stage instance = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext(
                    GoConstants.DEFAULT_APPROVED_BY), md5, new TimeProvider());
            stageDao.saveWithJobs(pipeline, instance);
            passStage(instance);
        }
        return pipelineDao.loadPipeline(pipeline.getId());
    }

    public Stage saveBuildingStage(String pipelineName, String stageName) throws SQLException {
        Pipeline pipeline = saveTestPipeline(pipelineName, stageName);
        Stage stage = saveBuildingStage(pipeline.getStages().byName(stageName));
        for (JobInstance job : stage.getJobInstances()) {
            job.setIdentifier(new JobIdentifier(pipeline, stage, job));
        }
        return stage;
    }

    public Stage saveBuildingStage(Stage stage) {
        for (JobInstance jobInstance : stage.getJobInstances()) {
            JobInstanceMother.setBuildingState(jobInstance);
            jobInstance.setAgentUuid(AGENT_UUID);
            jobInstanceDao.updateAssignedInfo(jobInstance);
        }
        return stage;
    }

    public Stage scheduleStage(Pipeline pipeline, StageConfig stageConfig) {
        return scheduleStage(pipeline, stageConfig, 0);
    }

    public Stage scheduleStage(Pipeline pipeline, StageConfig stageConfig, int order) {
        Stage instance = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext("cruise"), md5, new TimeProvider());
        return saveStage(pipeline, instance, order);
    }

    public Stage saveStage(Pipeline pipeline, Stage instance, int order) {
        instance.setOrderId(order);
        return stageDao.saveWithJobs(pipeline, instance);
    }

    public void passStage(Stage stage) {
        completeStage(stage, JobResult.Passed);
    }

    public void completeStage(final Stage stage, final JobResult jobResult) {
        final StageResult stageResult = completeAllJobs(stage, jobResult);
        updateResultInTransaction(stage, stageResult);
    }

    private void updateResultInTransaction(final Stage stage, final StageResult stageResult) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                stageDao.updateResult(stage, stageResult);
            }
        });
    }

    public StageResult completeAllJobs(Stage stage, JobResult jobResult) {
        for (JobInstance job : stage.getJobInstances()) {
            JobInstanceMother.setBuildingState(job);
            job.setAgentUuid(AGENT_UUID);
            job.completing(jobResult);
            job.completed(new DateTime().plusMinutes(5).toDate());
            jobInstanceDao.updateAssignedInfo(job);
        }
        StageResult stageResult;
        switch (jobResult) {
            case Failed:
                stageResult = StageResult.Failed;
                break;
            case Cancelled:
                stageResult = StageResult.Cancelled;
                break;
            default:
                stageResult = StageResult.Passed;
        }
        stage.calculateResult();
        return stageResult;
    }

    public void failStage(Stage stage) {
        failStage(stage, new Date());
    }

    public void failStage(Stage stage, Date completionDate) {
        for (JobInstance job : stage.getJobInstances()) {
            job.completing(Failed, completionDate);
            job.completed(completionDate);
            jobInstanceDao.updateStateAndResult(job);
        }
        stage.calculateResult();
        updateResultInTransaction(stage, StageResult.Failed);
    }

    public void buildingBuildInstance(Stage stage) {
        if (!stage.getJobInstances().isEmpty()) {
            JobInstance jobInstance = stage.getJobInstances().get(0);
            jobInstance.setAgentUuid(AGENT_UUID);
            jobInstance.changeState(JobState.Building);
            jobInstanceDao.updateAssignedInfo(jobInstance);
        }
    }

    public void assignToAgent(JobInstance jobInstance, String agentId) {
        jobInstance.setAgentUuid(agentId);
        jobInstance.changeState(JobState.Assigned);
        jobInstanceDao.updateAssignedInfo(jobInstance);
    }

    public void onefailAndOnePassedBuildInstances(Stage instance) {
        final JobInstance first = instance.getJobInstances().get(0);
        final JobInstance second = instance.getJobInstances().get(1);
        first.completing(Failed);
        second.completing(Failed);
        first.completed(new Date());
        second.completed(new Date());
        jobInstanceDao.updateStateAndResult(first);
        jobInstanceDao.updateStateAndResult(second);
    }

    public void cancelStage(Stage stage) {
        for (JobInstance job : stage.getJobInstances()) {
            job.cancel();
            jobInstanceDao.updateStateAndResult(job);
        }
        stage.calculateResult();
        updateResultInTransaction(stage, StageResult.Cancelled);
    }

    public void buildInstanceWithDiscontinuedState(Stage instance) {
        final JobInstance first = instance.getJobInstances().get(0);
        final JobInstance second = instance.getJobInstances().get(1);
        first.completing(JobResult.Passed);
        second.changeState(JobState.Discontinued);
        second.setResult(JobResult.Passed);
        first.completed(new Date());
        jobInstanceDao.updateStateAndResult(first);
        jobInstanceDao.updateStateAndResult(second);
        updateResultInTransaction(instance, StageResult.Passed);
    }

    public void saveMaterials(final MaterialRevisions materialRevisions) {
        saveRevs(materialRevisions);
    }

    public void saveRevs(final MaterialRevisions materialRevisions) {
        final MaterialRevisions unsavedRevisions = new MaterialRevisions();
        for (MaterialRevision materialRevision : materialRevisions) {
            unsavedRevisions.addRevision(filterUnsaved(materialRevision));
        }
        if (unsavedRevisions.isEmpty()) {
            return;
        }
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(unsavedRevisions);
            }
        });

//        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
//            @Override
//            protected void doInTransactionWithoutResult(TransactionStatus status) {
//                materialRepository.save(materialRevisions);
//            }
//        });
    }

    private MaterialRevision filterUnsaved(MaterialRevision materialRevision) {
        ArrayList<Modification> unsavedModifications = new ArrayList<>();
        for (Modification modification : materialRevision.getModifications()) {
            if (!modification.hasId()) {
                unsavedModifications.add(modification);
            }
        }
        return new MaterialRevision(materialRevision.getMaterial(), unsavedModifications);
    }

    public void execute(String sql) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            execute(sql, connection);
        } finally {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            connection.close();
        }
    }

    public void execute(String sql, Connection connection) throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.execute(sql);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    static void assertNotInserted(long instanceId) {
        org.junit.Assert.assertThat("Already thinks it's inserted", instanceId, org.hamcrest.core.Is.is(PersistentObject.NOT_PERSISTED));
    }

    static void assertIsInserted(long instanceId) {
        org.junit.Assert.assertThat("Not inserted", instanceId, org.hamcrest.core.Is.is(org.hamcrest.core.IsNot.not(PersistentObject.NOT_PERSISTED)));
    }

    public Pipeline schedulePipeline(PipelineConfig pipelineConfig, Clock clock) {
        return schedulePipeline(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig), clock);
    }

    public Pipeline schedulePipeline(PipelineConfig pipelineConfig, BuildCause buildCause, Clock clock) {
        return schedulePipeline(pipelineConfig, buildCause, GoConstants.DEFAULT_APPROVED_BY, clock);
    }

    public Pipeline schedulePipeline(PipelineConfig pipelineConfig, BuildCause buildCause, String approvedBy, final Clock clock) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(approvedBy), md5, clock);
        return scheduleJobInstancesAndSavePipeline(pipeline);
    }

    public Pipeline schedulePipelineWithAllStages(PipelineConfig pipelineConfig, BuildCause buildCause) {
        buildCause.assertMaterialsMatch(pipelineConfig.materialConfigs());
        DefaultSchedulingContext defaultSchedulingContext = new DefaultSchedulingContext(GoConstants.DEFAULT_APPROVED_BY);
        Stages stages = new Stages();
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, defaultSchedulingContext, md5, new TimeProvider());
        for (StageConfig stageConfig : pipelineConfig) {
            stages.add(instanceFactory.createStageInstance(stageConfig, defaultSchedulingContext, md5, new TimeProvider()));
        }
        pipeline.setStages(stages);
        return scheduleJobInstancesAndSavePipeline(pipeline);
    }

    private Pipeline scheduleJobInstancesAndSavePipeline(Pipeline pipeline) {
        assertNotInserted(pipeline.getId());
        for (Stage stage : pipeline.getStages()) {
            for (JobInstance jobInstance : stage.getJobInstances()) {
                jobInstance.schedule();
            }
        }
        this.savePipelineWithStagesAndMaterials(pipeline);

        long pipelineId = pipeline.getId();
        assertIsInserted(pipelineId);
        return pipeline;
    }

    public Integer updateNaturalOrder(final long pipelineId, final double naturalOrder) {
        return (Integer) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                return PipelineRepository.updateNaturalOrderForPipeline(session, pipelineId, naturalOrder);
            }
        });
    }

    public MaterialRevision addRevisionsWithModifications(Material material, Modification... modifications) {
        final MaterialRevision revision = filterUnsaved(new MaterialRevision(material, modifications));
        if (revision.getModifications().isEmpty()){
            return revision;
        }
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.saveMaterialRevision(revision);
            }
        });
        return revision;
    }

    public Pipeline checkinRevisionsToBuild(ManualBuild build, PipelineConfig pipelineConfig, MaterialRevision... materialRevisions) {
        return checkinRevisionsToBuild(build, pipelineConfig, Arrays.asList(materialRevisions));
    }

    public Pipeline checkinRevisionsToBuild(ManualBuild build, PipelineConfig pipelineConfig, List<MaterialRevision> revisions) {
        return pipelineService.save(
                instanceFactory.createPipelineInstance(pipelineConfig, build.onModifications(new MaterialRevisions(revisions), false, null), new DefaultSchedulingContext(), "md5-test",
                        new TimeProvider()));
    }

    public List<MaterialRevision> addDependencyRevisionModification(List<MaterialRevision> materialRevisions, DependencyMaterial dependencyMaterial, Pipeline... upstreams) {
        String stageName = CaseInsensitiveString.str(dependencyMaterial.getStageName());
        String label = upstreams[0].getLabel();
        List<Modification> modifications = new ArrayList<Modification>();
        for (Pipeline upstream : upstreams) {
            modifications.add(new Modification(new Date(),
                    DependencyMaterialRevision.create(CaseInsensitiveString.str(dependencyMaterial.getPipelineName()), upstream.getCounter(), label, stageName, upstream.findStage(stageName
                    ).getCounter()).getRevision(),
                    label, upstream.getId()));
        }
        MaterialRevision depRev = addRevisionsWithModifications(dependencyMaterial, modifications.toArray(new Modification[0]));
        materialRevisions.add(depRev);
        return Arrays.asList(depRev);
    }
}
