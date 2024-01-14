/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.ManualBuild;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.AmbiguousTableNameException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.SessionFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;
import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@SuppressWarnings("TestOnlyProblems") // Workaround for IntelliJ thinking this place is production rather than test code
@Component
public class DatabaseAccessHelper extends HibernateDaoSupport {
    public static final String AGENT_UUID = "123456789-123";
    private static final String MD5 = "md5-test";
    private final DataSource dataSource;
    private final SqlSessionFactory sqlMapClient;
    private final PipelineTimeline pipelineTimeline;
    private final TransactionTemplate transactionTemplate;
    private final PipelineService pipelineService;
    private IDatabaseTester databaseTester;
    private final StageDao stageDao;
    private final PipelineSqlMapDao pipelineDao;
    private final JobInstanceDao jobInstanceDao;
    private final AgentDao agentDao;

    private final MaterialRepository materialRepository;
    private final GoCache goCache;
    private final InstanceFactory instanceFactory;
    private final JobAgentMetadataDao jobAgentMetadataDao;

    @Autowired
    public DatabaseAccessHelper(DataSource dataSource,
                                SqlSessionFactory sqlMapClient,
                                StageDao stageDao,
                                JobInstanceDao jobInstanceDao,
                                PipelineDao pipelineDao,
                                MaterialRepository materialRepository,
                                SessionFactory sessionFactory,
                                PipelineTimeline pipelineTimeline,
                                TransactionTemplate transactionTemplate,
                                GoCache goCache,
                                PipelineService pipelineService, InstanceFactory instanceFactory,
                                JobAgentMetadataDao jobAgentMetadataDao,
                                AgentDao agentDao) throws AmbiguousTableNameException {
        this.dataSource = dataSource;
        this.sqlMapClient = sqlMapClient;
        this.stageDao = stageDao;
        this.jobInstanceDao = jobInstanceDao;
        this.pipelineTimeline = pipelineTimeline;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
        this.pipelineService = pipelineService;
        this.instanceFactory = instanceFactory;
        this.jobAgentMetadataDao = jobAgentMetadataDao;
        this.pipelineDao = (PipelineSqlMapDao) pipelineDao;
        this.materialRepository = materialRepository;
        this.agentDao = agentDao;
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
        dataSet.addTable(new DefaultTable("pipelinestates"));
        dataSet.addTable(new DefaultTable("materials"));
        dataSet.addTable(new DefaultTable("modifications"));
        dataSet.addTable(new DefaultTable("pipelineMaterialRevisions"));
        dataSet.addTable(new DefaultTable("modifiedFiles"));

        dataSet.addTable(new DefaultTable("notificationfilters"));
        dataSet.addTable(new DefaultTable("users"));
        dataSet.addTable(new DefaultTable("stages"));
        dataSet.addTable(new DefaultTable("pipelineLabelCounts"));
        dataSet.addTable(new DefaultTable("environmentVariables"));
        dataSet.addTable(new DefaultTable("artifactPlans"));
        dataSet.addTable(new DefaultTable("buildStateTransitions"));
        dataSet.addTable(new DefaultTable("resources"));
        dataSet.addTable(new DefaultTable("builds"));

        dataSet.addTable(new DefaultTable("stageArtifactCleanupProhibited"));
        dataSet.addTable(new DefaultTable("serverBackups"));
        dataSet.addTable(new DefaultTable("jobAgentMetadata"));
        dataSet.addTable(new DefaultTable("AccessToken"));

        databaseTester.setDataSet(dataSet);
    }

    public void onSetUp() throws Exception {
        databaseTester.onSetup();
        pipelineTimeline.clearWhichIsEvilAndShouldNotBeUsedInRealWorld();
        if (sqlMapClient != null) {
            for (Cache cache : sqlMapClient.getConfiguration().getCaches()) {
                cache.clear();
            }
        }
    }

    public void onTearDown() throws Exception {
        databaseTester.onTearDown();
        goCache.clear();
    }

    public TransactionTemplate txTemplate() {
        return transactionTemplate;
    }

    public JobInstanceDao getBuildInstanceDao() {
        return jobInstanceDao;
    }

    public StageDao getStageDao() {
        return stageDao;
    }

    public PipelineDao getPipelineDao() {
        return pipelineDao;
    }

    public void pass(Pipeline pipeline) {
        for (Stage stage : pipeline.getStages()) {
            passStage(stage);
        }
    }

    public Pipeline saveTestPipeline(String pipelineName, String stageName, String... jobConfigNames) {
        PipelineConfig pipelineConfig = configurePipeline(pipelineName, stageName, jobConfigNames);
        Pipeline pipeline = scheduleWithFileChanges(pipelineConfig);
        pipeline = savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

    public Pipeline savePipelineWithStagesAndMaterials(Pipeline pipeline) {
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
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs, stageConfig);
    }

    private Pipeline scheduleWithFileChanges(PipelineConfig pipelineConfig) {
        BuildCause buildCause = BuildCause.createWithModifications(modifyOneFile(pipelineConfig), "");
        saveRevs(buildCause.getMaterialRevisions());
        return instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(
            GoConstants.DEFAULT_APPROVED_BY), MD5, new TimeProvider());
    }

    public Pipeline passPipelineFirstStageOnly(Pipeline pipeline) {
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

    public Pipeline newPipelineWithFirstStagePassed(PipelineConfig config) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(config,
            BuildCause.createManualForced(modifyOneFile(new MaterialConfigConverter().toMaterials(config.materialConfigs()), ModificationsMother.nextRevision()), Username.ANONYMOUS),
            new DefaultSchedulingContext(
                GoConstants.DEFAULT_APPROVED_BY), MD5, new TimeProvider());
        saveMaterialsWIthPassedStages(pipeline);
        return pipeline;
    }

    public void saveMaterialsWIthPassedStages(Pipeline pipeline) {
        savePipelineWithStagesAndMaterials(pipeline);
        passStage(pipeline.getFirstStage());
    }

    public Pipeline newPipelineWithFirstStageFailed(PipelineConfig config) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(config, BuildCause.createManualForced(modifyOneFile(new MaterialConfigConverter().toMaterials(config.materialConfigs()),
                ModificationsMother.currentRevision()), Username.ANONYMOUS),
            new DefaultSchedulingContext(
                GoConstants.DEFAULT_APPROVED_BY), MD5, new TimeProvider());
        savePipelineWithStagesAndMaterials(pipeline);
        failStage(pipeline.getFirstStage());
        return pipeline;
    }

    public Pipeline newPipelineWithFirstStageScheduled(PipelineConfig config) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(config,
            BuildCause.createManualForced(modifyOneFile(new MaterialConfigConverter().toMaterials(config.materialConfigs()), ModificationsMother.nextRevision()), Username.ANONYMOUS),
            new DefaultSchedulingContext(
                GoConstants.DEFAULT_APPROVED_BY), MD5, new TimeProvider());
        savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

    public Pipeline newPipelineWithAllStagesPassed(PipelineConfig config) {
        Pipeline pipeline = newPipelineWithFirstStagePassed(config);
        for (StageConfig stageConfig : config) {
            if (config.first().equals(stageConfig)) {
                continue;
            }
            Stage instance = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext(
                GoConstants.DEFAULT_APPROVED_BY), MD5, new TimeProvider());
            stageDao.saveWithJobs(pipeline, instance);
            passStage(instance);
        }
        return pipelineDao.loadPipeline(pipeline.getId());
    }

    public Stage saveBuildingStage(String pipelineName, String stageName) {
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
        Stage instance = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext("cruise"), MD5, new TimeProvider());
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
                stageDao.updateResult(stage, stageResult, null);
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
        StageResult stageResult = switch (jobResult) {
            case Failed -> StageResult.Failed;
            case Cancelled -> StageResult.Cancelled;
            default -> StageResult.Passed;
        };
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

    public void failJob(Stage stage, JobInstance instance) {
        instance.completing(Failed);
        instance.completed(new Date());
        jobInstanceDao.updateStateAndResult(instance);
        stage.calculateResult();
        updateResultInTransaction(stage, stage.getResult());
    }

    public void cancelStage(Stage stage) {
        for (JobInstance job : stage.getJobInstances()) {
            job.cancel();
            jobInstanceDao.updateStateAndResult(job);
        }
        stage.calculateResult();
        updateResultInTransaction(stage, StageResult.Cancelled);
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
        try (Statement stmt = connection.createStatement()) {
            //noinspection SqlSourceToSinkFlow
            stmt.execute(sql);
        }
    }

    static void assertNotInserted(long instanceId) {
        assertThat("Already thinks it's inserted", instanceId, is(NOT_PERSISTED));
    }

    static void assertIsInserted(long instanceId) {
        assertThat("Not inserted", instanceId, is(not(NOT_PERSISTED)));
    }

    public Pipeline schedulePipeline(PipelineConfig pipelineConfig, Clock clock) {
        return schedulePipeline(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig), clock);
    }

    public Pipeline schedulePipeline(PipelineConfig pipelineConfig, BuildCause buildCause, Clock clock) {
        return schedulePipeline(pipelineConfig, buildCause, GoConstants.DEFAULT_APPROVED_BY, clock);
    }

    public Pipeline schedulePipeline(PipelineConfig pipelineConfig, BuildCause buildCause, String approvedBy, final Clock clock) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(approvedBy), MD5, clock);
        return scheduleJobInstancesAndSavePipeline(pipeline);
    }

    public Pipeline schedulePipeline(PipelineConfig pipelineConfig, BuildCause buildCause, String approvedBy, final Clock clock, Map<String, ElasticProfile> profiles, Map<String, ClusterProfile> clusterProfiles) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(approvedBy, new Agents(), profiles, clusterProfiles), MD5, clock);
        return scheduleJobInstancesAndSavePipeline(pipeline);
    }

    public Pipeline schedulePipelineWithAllStages(PipelineConfig pipelineConfig, BuildCause buildCause) {
        buildCause.assertMaterialsMatch(pipelineConfig.materialConfigs());
        DefaultSchedulingContext defaultSchedulingContext = new DefaultSchedulingContext(GoConstants.DEFAULT_APPROVED_BY);
        Stages stages = new Stages();
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, defaultSchedulingContext, MD5, new TimeProvider());
        for (StageConfig stageConfig : pipelineConfig) {
            stages.add(instanceFactory.createStageInstance(stageConfig, defaultSchedulingContext, MD5, new TimeProvider()));
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
        return getHibernateTemplate().execute(session -> PipelineRepository.updateNaturalOrderForPipeline(session, pipelineId, naturalOrder));
    }

    public MaterialRevision addRevisionsWithModifications(Material material, Modification... modifications) {
        final MaterialRevision revision = filterUnsaved(new MaterialRevision(material, modifications));
        if (revision.getModifications().isEmpty()) {
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

    public void addAgent(Agent agent) {
        if (agent != null) {
            agentDao.saveOrUpdate(agent);
        }
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
        List<Modification> modifications = new ArrayList<>();
        for (Pipeline upstream : upstreams) {
            modifications.add(new Modification(new Date(),
                DependencyMaterialRevision.create(CaseInsensitiveString.str(dependencyMaterial.getPipelineName()), upstream.getCounter(), label, stageName, upstream.findStage(stageName
                ).getCounter()).getRevision(),
                label, upstream.getId()));
        }
        MaterialRevision depRev = addRevisionsWithModifications(dependencyMaterial, modifications.toArray(new Modification[0]));
        materialRevisions.add(depRev);
        return List.of(depRev);
    }

    public void addJobAgentMetadata(JobAgentMetadata jobAgentMetadata) {
        jobAgentMetadataDao.save(jobAgentMetadata);
    }
}
