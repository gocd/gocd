/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.BuildPlanMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.SortOrder;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.SQLException;
import java.util.*;

import static com.thoughtworks.go.helper.JobInstanceMother.*;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.server.dao.PersistentObjectMatchers.hasSameId;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class JobInstanceSqlMapDaoTest {
    @Autowired
    private JobInstanceSqlMapDao jobInstanceDao;
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private EnvironmentVariableDao environmentVariableDao;
    @Autowired
    private PipelineDao pipelineDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private StageDao stageDao;
    @Autowired
    private GoCache goCache;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private InstanceFactory instanceFactory;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    private long stageId;
    private static final String JOB_NAME = "functional";
    private static final String JOB_NAME_IN_DIFFERENT_CASE = "FUnctiONAl";
    private final String projectOne = "project1";
    private static final String STAGE_NAME = "mingle";
    private static final String PIPELINE_NAME = "pipeline";
    private PipelineConfig pipelineConfig;
    private Pipeline savedPipeline;
    private Stage savedStage;
    private static final Date MOST_RECENT_DATE = new DateTime().plusMinutes(20).toDate();
    private int counter;
    private static final String OTHER_JOB_NAME = "unit";
    private DefaultSchedulingContext schedulingContext;
    private SqlMapClientTemplate actualSqlClientTemplate;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        goCache.clear();
        pipelineConfig = PipelineMother.withSingleStageWithMaterials(PIPELINE_NAME, STAGE_NAME, BuildPlanMother.withBuildPlans(JOB_NAME, OTHER_JOB_NAME));
        schedulingContext = new DefaultSchedulingContext(DEFAULT_APPROVED_BY);
        savedPipeline = instanceFactory.createPipelineInstance(pipelineConfig, modifySomeFiles(pipelineConfig), schedulingContext, "md5-test", new TimeProvider());

        dbHelper.savePipelineWithStagesAndMaterials(savedPipeline);

        actualSqlClientTemplate = jobInstanceDao.getSqlMapClientTemplate();

        savedStage = savedPipeline.getFirstStage();
        stageId = savedStage.getId();
        counter = savedPipeline.getFirstStage().getCounter();
        JobInstance job = savedPipeline.getStages().first().getJobInstances().first();
        job.setIgnored(true);
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        jobInstanceDao.setSqlMapClientTemplate(actualSqlClientTemplate);
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldSaveAndRetrieveIncompleteBuild() throws Exception {
        JobInstance expected = scheduled(JOB_NAME, new Date());
        expected = jobInstanceDao.save(stageId, expected);
        assertThat(expected.getId(), is(not(0L)));

        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(expected.getId());
        assertThat(actual, hasSameId(expected));
    }

    @Test
    public void shouldGetBuildIngBuildAsMostRecentBuildByPipelineLabelAndStageCounter() throws Exception {
        JobInstance expected = JobInstanceMother.building(JOB_NAME);
        expected.setScheduledDate(MOST_RECENT_DATE);
        expected = jobInstanceDao.save(stageId, expected);

        JobInstance actual = jobInstanceDao.mostRecentJobWithTransitions(
                new JobIdentifier(PIPELINE_NAME, savedPipeline.getCounter(), savedPipeline.getLabel(), STAGE_NAME,
                        String.valueOf(counter), JOB_NAME));

        assertThat(actual.getId(), is(expected.getId()));
        assertThat("JobInstance should match", actual.getId(), is(expected.getId()));
        assertThat(actual.getTransitions(), is(expected.getTransitions()));
    }

    @Test
    public void shouldGetCompletedBuildAsMostRecentBuildByPipelineLabelAndStageCounter() throws Exception {
        JobInstance expected = JobInstanceMother.completed(JOB_NAME, JobResult.Unknown);
        expected.setScheduledDate(MOST_RECENT_DATE);
        expected = jobInstanceDao.save(stageId, expected);

        JobInstance actual = jobInstanceDao.mostRecentJobWithTransitions(
                new JobIdentifier(PIPELINE_NAME, savedPipeline.getCounter(), savedPipeline.getLabel(), STAGE_NAME,
                        String.valueOf(counter), JOB_NAME));
        assertThat("JobInstance should match", actual.getId(), is(expected.getId()));
    }

    @Test
    public void shouldFindJobByPipelineCounterWhenTwoPipelinesWithSameLabel() {
        pipelineConfig.setLabelTemplate("fixed-label");
        Pipeline oldPipeline = createNewPipeline(pipelineConfig);
        Pipeline newPipeline = createNewPipeline(pipelineConfig);

        JobInstance expected = oldPipeline.getFirstStage().getJobInstances().first();
        JobInstance actual = jobInstanceDao.mostRecentJobWithTransitions(
                new JobIdentifier(oldPipeline, oldPipeline.getFirstStage(), expected));
        assertThat(actual.getId(), is(expected.getId()));
    }

    private Pipeline createNewPipeline(PipelineConfig pipelineConfig) {
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, modifySomeFiles(pipelineConfig), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), "md5-test", new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

    @Test
    public void shouldFindJobIdByPipelineCounter() throws Exception {
        long actual = jobInstanceDao.findOriginalJobIdentifier(new StageIdentifier(savedPipeline, savedStage), JOB_NAME).getBuildId();

        assertThat(actual, is(savedStage.getJobInstances().getByName(JOB_NAME).getId()));
    }

    @Test
    public void shouldInvalidateSessionAndFetchJobIdentifier_WhenNewJobIsInserted() throws Exception {
        configHelper.addPipeline(pipelineConfig);
        configHelper.turnOffSecurity();

        StageIdentifier stageIdentifier = new StageIdentifier(savedPipeline.getName(), savedPipeline.getCounter(), savedPipeline.getLabel(), savedStage.getName(), Integer.toString(savedStage.getCounter() + 1));
        assertThat(jobInstanceDao.findOriginalJobIdentifier(stageIdentifier, JOB_NAME), is(nullValue()));
        dbHelper.passStage(savedStage);
        Stage stage = scheduleService.rerunStage(savedPipeline.getName(), savedPipeline.getCounter(), savedStage.getName());

        JobIdentifier actual = jobInstanceDao.findOriginalJobIdentifier(stageIdentifier, JOB_NAME);

        assertThat(actual, is(notNullValue()));
        assertThat(actual, is(new JobIdentifier(stageIdentifier, JOB_NAME, stage.getFirstJob().getId())));
    }

    @Test
    public void shouldFindJobIdByPipelineLabel() throws Exception {
        long actual = jobInstanceDao.findOriginalJobIdentifier(new StageIdentifier(PIPELINE_NAME, savedPipeline.getCounter(), null, STAGE_NAME, String.valueOf(counter)), JOB_NAME).getBuildId();

        assertThat(actual, is(savedStage.getJobInstances().getByName(JOB_NAME).getId()));
    }

    @Test
    public void findByJobIdShouldBeJobNameCaseAgnostic() throws Exception {
        long actual = jobInstanceDao.findOriginalJobIdentifier(new StageIdentifier(PIPELINE_NAME, savedPipeline.getCounter(),
                null, STAGE_NAME, String.valueOf(counter)), JOB_NAME_IN_DIFFERENT_CASE).getBuildId();

        assertThat(actual, is(savedStage.getJobInstances().getByName(JOB_NAME).getId()));
    }

    @Test
    public void findByJobIdShouldLoadOriginalJobWhenCopiedForJobRerun() throws Exception {
        Stage firstOldStage = savedPipeline.getStages().get(0);
        Stage newStage = instanceFactory.createStageForRerunOfJobs(firstOldStage, a(JOB_NAME), new DefaultSchedulingContext("loser", new Agents()), pipelineConfig.get(0), new TimeProvider(), "md5");

        stageDao.saveWithJobs(savedPipeline, newStage);
        dbHelper.passStage(newStage);

        JobIdentifier oldJobIdentifierThroughOldJob = jobInstanceDao.findOriginalJobIdentifier(new StageIdentifier(PIPELINE_NAME, savedPipeline.getCounter(), null, STAGE_NAME, String.valueOf(counter)), OTHER_JOB_NAME);

        JobIdentifier oldJobIdentifierThroughCopiedNewJob = jobInstanceDao.findOriginalJobIdentifier(new StageIdentifier(PIPELINE_NAME, savedPipeline.getCounter(), null, STAGE_NAME, String.valueOf(newStage.getCounter())), OTHER_JOB_NAME);

        JobIdentifier newJobIdentifierThroughRerunJob = jobInstanceDao.findOriginalJobIdentifier(new StageIdentifier(PIPELINE_NAME, savedPipeline.getCounter(), null, STAGE_NAME, String.valueOf(newStage.getCounter())), JOB_NAME);

        assertThat(oldJobIdentifierThroughOldJob, is(firstOldStage.getJobInstances().getByName(OTHER_JOB_NAME).getIdentifier()));
        assertThat(oldJobIdentifierThroughCopiedNewJob, is(firstOldStage.getJobInstances().getByName(OTHER_JOB_NAME).getIdentifier()));
        assertThat(newJobIdentifierThroughRerunJob, is(newStage.getJobInstances().getByName(JOB_NAME).getIdentifier()));
    }

    @Test
    public void findJobIdShouldExcludeIgnoredJob() throws Exception {
        JobInstance oldJob = savedStage.getJobInstances().getByName(JOB_NAME);
        jobInstanceDao.ignore(oldJob);

        JobInstance expected = JobInstanceMother.scheduled(JOB_NAME);
        expected = jobInstanceDao.save(stageId, expected);

        long actual = jobInstanceDao.findOriginalJobIdentifier(new StageIdentifier(savedPipeline, savedStage), JOB_NAME).getBuildId();

        assertThat(actual, is(expected.getId()));
    }

    @Test
    public void shouldFindAllInstancesOfJobsThatAreRunOnAllAgents() throws Exception {
        List<JobInstance> before = stageDao.mostRecentJobsForStage(PIPELINE_NAME, STAGE_NAME);

        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();

        JobInstance instance1 = savedJobForAgent(JOB_NAME + "-" + uuid1, uuid1, true, false);
        JobInstance instance2 = savedJobForAgent(JOB_NAME + "-" + uuid2, uuid2, true, false);

        List<JobInstance> after = stageDao.mostRecentJobsForStage(PIPELINE_NAME, STAGE_NAME);
        after.removeAll(before);
        assertThat(after.toArray(), hasItemInArray(hasProperty("name", is(instance1.getName()))));
        assertThat(after.toArray(), hasItemInArray(hasProperty("name", is(instance2.getName()))));
        assertThat("Expected 2 but got " + after, after.size(), is(2));
    }

    @Test
    public void shouldFindAllInstancesOfJobsThatAreRunMultipleInstance() throws Exception {
        List<JobInstance> before = stageDao.mostRecentJobsForStage(PIPELINE_NAME, STAGE_NAME);

        JobInstance instance1 = savedJobForAgent(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("job", 1), null, false, true);
        JobInstance instance2 = savedJobForAgent(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("job", 2), null, false, true);

        List<JobInstance> after = stageDao.mostRecentJobsForStage(PIPELINE_NAME, STAGE_NAME);
        after.removeAll(before);

        assertThat("Expected 2 but got " + after, after.size(), is(2));
        assertThat(after.toArray(), hasItemInArray(hasProperty("name", is(instance1.getName()))));
        assertThat(after.toArray(), hasItemInArray(hasProperty("name", is(instance2.getName()))));
    }

    @Test
    public void shouldLoadOldestBuild(){
        JobStateTransition jobStateTransition = jobInstanceDao.oldestBuild();
        assertThat(jobStateTransition.getId(), is(stageDao.stageById(stageId).getJobInstances().first().getTransitions().first().getId()));
    }

    private JobInstance savedJobForAgent(final String jobName, final String uuid, final boolean runOnAllAgents, final boolean runMultipleInstance) {
        return (JobInstance) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                JobInstance jobInstance = scheduled(jobName, new DateTime().plusMinutes(1).toDate());
                jobInstance.setRunOnAllAgents(runOnAllAgents);
                jobInstance.setRunMultipleInstance(runMultipleInstance);
                jobInstanceService.save(savedStage.getIdentifier(), stageId, jobInstance);
                jobInstance.changeState(JobState.Building);
                jobInstance.setAgentUuid(uuid);
                jobInstanceDao.updateStateAndResult(jobInstance);
                return jobInstance;
            }
        });
    }

    @Test(expected = DataRetrievalFailureException.class)
    public void shouldReturnNullObjectWhenNoBuildInstanceFound() throws Exception {
        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(999);
        assertThat(actual.isNull(), is(true));
    }

    @Test
    public void shouldReturnBuildInstanceIfItExists() throws Exception {
        JobInstance jobInstance = JobInstanceMother.completed("Baboon", JobResult.Passed);
        JobInstance instance = jobInstanceDao.save(stageId, jobInstance);
        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(instance.getId());
        assertThat(actual.isNull(), is(false));
    }

    private JobInstance[] createJobs(final String name, int count) {
        JobInstance[] before = new JobInstance[count];
        for (int i = 0; i < count; i++) {
            before[i] = saveJob(name + "-" + i);
        }
        return before;
    }

    @Test
    public void shouldLogStatusUpdatesOfCompletedJobs() throws Exception {
        try (LogFixture logFixture = logFixtureFor(JobInstanceSqlMapDao.class, Level.DEBUG)) {
            JobInstance instance = runningJob("1");
            completeJobs(instance);
            instance.schedule();
            jobInstanceDao.updateStateAndResult(instance);
            assertThat(logFixture.getLog(), logFixture.contains(Level.WARN, "State change for a completed Job is not allowed."), is(true));
        }
    }

    private JobInstance[] completeJobs(JobInstance... instances) {
        for (JobInstance instance : instances) {
            complete(instance);
        }
        return instances;
    }

    private JobInstance saveJob(String jobName) {
        JobInstance jobInstance = JobInstanceMother.completed(jobName, JobResult.Passed);
        jobInstanceDao.save(stageId, jobInstance);
        return jobInstance;
    }

    private JobInstance runningJob(final String name) {
        JobInstance jobInstance = JobInstanceMother.buildingInstance("pipeline", "stage", name, "1");
        jobInstanceDao.save(stageId, jobInstance);
        return jobInstance;
    }

    private void complete(JobInstance jobInstance) {
        jobInstance.completing(JobResult.Passed);
        jobInstance.completed(new Date());
        jobInstanceDao.updateStateAndResult(jobInstance);
    }

    @Test
    public void shouldUpdateBuildResult() throws Exception {
        JobInstance jobInstance = JobInstanceMother.scheduled("Baboon");
        jobInstanceDao.save(stageId, jobInstance);
        jobInstance.cancel();
        jobInstanceDao.updateStateAndResult(jobInstance);
        JobInstance instance = jobInstanceDao.buildByIdWithTransitions(jobInstance.getId());
        assertThat(instance.getResult(), is(JobResult.Cancelled));

        jobInstance.fail();
        jobInstanceDao.updateStateAndResult(jobInstance);
        instance = jobInstanceDao.buildByIdWithTransitions(jobInstance.getId());
        assertThat(instance.getResult(), is(JobResult.Failed));

    }

    @Test
    public void shouldDeleteJobPlanAssociatedEntities() throws Exception {
        JobInstance jobInstance = JobInstanceMother.building("Baboon");

        JobPlan jobPlan = JobInstanceMother.jobPlanWithAssociatedEntities(jobInstance.getName(), jobInstance.getId(), artifactPlans(), artifactPropertiesGenerators());
        jobInstance.setPlan(jobPlan);

        jobInstanceDao.save(stageId, jobInstance);
        JobPlan jobPlanFromDb = jobInstanceDao.loadPlan(jobInstance.getId());

        assertThat(jobPlanFromDb.getArtifactPlans(), is(jobPlan.getArtifactPlans()));
        assertThat(jobPlanFromDb.getPropertyGenerators(), is(jobPlan.getPropertyGenerators()));
        assertThat(jobPlanFromDb.getResources(), is(jobPlan.getResources()));
        assertThat(jobPlanFromDb.getVariables(), is(jobPlan.getVariables()));
        assertThat(jobPlanFromDb.getElasticProfile(), is(jobPlan.getElasticProfile()));

        Date completionDate = new Date();
        jobInstance.completing(JobResult.Passed, completionDate);
        jobInstance.completed(completionDate);
        jobInstanceDao.updateStateAndResult(jobInstance);
        jobPlanFromDb = jobInstanceDao.loadPlan(jobInstance.getId());

        assertThat(jobPlanFromDb.getArtifactPlans().size(), is(2));
        assertThat(jobPlanFromDb.getArtifactPlans(), Matchers.containsInAnyOrder(
                new ArtifactPlan(ArtifactPlanType.unit, "unit", "unit"),
                new ArtifactPlan(ArtifactPlanType.unit, "integration", "integration")
        ));
        assertThat(jobPlanFromDb.getPropertyGenerators().size(), is(0));
        assertThat(jobPlanFromDb.getResources().size(), is(0));
        assertThat(jobPlanFromDb.getVariables().size(), is(0));
        assertThat(jobPlanFromDb.getElasticProfile(), is(nullValue()));
    }

    @Test
    public void shouldDeleteVariablesAttachedToJobAfterTheJobReschedules() throws Exception {
        JobInstance jobInstance = JobInstanceMother.building("Baboon");

        JobPlan jobPlan = JobInstanceMother.jobPlanWithAssociatedEntities(jobInstance.getName(), jobInstance.getId(), artifactPlans(), artifactPropertiesGenerators());
        jobInstance.setPlan(jobPlan);

        jobInstanceDao.save(stageId, jobInstance);
        JobPlan jobPlanFromDb = jobInstanceDao.loadPlan(jobInstance.getId());

        assertThat(jobPlanFromDb.getArtifactPlans().size(), is(4));
        assertThat(jobPlanFromDb.getPropertyGenerators(), is(jobPlan.getPropertyGenerators()));
        assertThat(jobPlanFromDb.getResources(), is(jobPlan.getResources()));
        assertThat(jobPlanFromDb.getVariables(), is(jobPlan.getVariables()));
        assertThat(jobPlanFromDb.getElasticProfile(), is(jobPlan.getElasticProfile()));

        jobInstance.setState(JobState.Rescheduled);
        jobInstanceDao.ignore(jobInstance);
        jobPlanFromDb = jobInstanceDao.loadPlan(jobInstance.getId());

        assertThat(jobPlanFromDb.getArtifactPlans().size(), is(2));
        assertThat(jobPlanFromDb.getArtifactPlans(), Matchers.containsInAnyOrder(
                new ArtifactPlan(ArtifactPlanType.unit, "unit", "unit"),
                new ArtifactPlan(ArtifactPlanType.unit, "integration", "integration")
        ));
        assertThat(jobPlanFromDb.getPropertyGenerators().size(), is(0));
        assertThat(jobPlanFromDb.getResources().size(), is(0));
        assertThat(jobPlanFromDb.getVariables().size(), is(0));
        assertThat(jobPlanFromDb.getElasticProfile(), is(nullValue()));

    }

    @Test(expected = DataRetrievalFailureException.class)
    public void shouldReturnNullObjectIfItNotExists() throws Exception {
        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(1);
        assertThat(actual.isNull(), is(true));
    }

    @Test
    public void shouldNotGetJobsFromBeforeTheJobNameIsChanged() throws Exception {
        String oldName = "oldName";
        createSomeJobs(oldName, 15);

        String newName = "newName";
        createSomeJobs(newName, 10);

        JobInstances myinstances = jobInstanceDao.latestCompletedJobs(PIPELINE_NAME, STAGE_NAME, newName, 25);
        assertThat(myinstances.size(), is(10));
        assertThat(myinstances.get(0).getName(), is(not(oldName)));
        assertThat(myinstances.get(0).getName(), is(newName));
    }

    private long createSomeJobs(String jobName, int count) throws SQLException {
        long stageId = 0;
        for (int i = 0; i < count; i++) {
            Pipeline newPipeline = createNewPipeline(pipelineConfig);
            stageId = newPipeline.getFirstStage().getId();
            JobInstance job = JobInstanceMother.completed(jobName, JobResult.Passed);
            jobInstanceDao.save(stageId, job);
        }
        return stageId;
    }

    private void createCopiedJobs(long stageId, String jobName, int count) throws SQLException {
        for (int i = 0; i < count; i++) {
            JobInstance job = JobInstanceMother.completed(jobName, JobResult.Failed);
            job.setOriginalJobId(1L);
            jobInstanceDao.save(stageId, job);
        }
    }

    @Test
    public void shouldGetMostRecentCompletedBuildsWhenTotalBuildsIsLessThan25() throws Exception {
        JobInstance jobInstance = JobInstanceMother.completed("shouldnotload", JobResult.Passed);
        jobInstanceDao.save(stageId, jobInstance);

        createSomeJobs(JOB_NAME, 3);

        JobInstances instances = jobInstanceDao.latestCompletedJobs(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 25);
        assertThat(instances.size(), is(3));
    }

    @Test
    public void shouldLoadStageCounter() throws Exception {
        JobInstance jobInstance = JobInstanceMother.completed("shouldnotload", JobResult.Passed);
        jobInstanceDao.save(stageId, jobInstance);
        createSomeJobs(JOB_NAME, 3);

        JobInstances instances = jobInstanceDao.latestCompletedJobs(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 25);

        for (JobInstance instance : instances) {
            Pipeline pipeline = pipelineDao.pipelineWithMaterialsAndModsByBuildId(instance.getId());
            String locator = pipeline.getName() +
                    "/" + pipeline.getLabel() + "/" + savedStage.getName() + "/1/" + JOB_NAME;
            assertThat(instance.getIdentifier().buildLocator(), Is.is(locator));
        }
    }

    @Test
    public void shouldGet25Builds() throws Exception {
        createSomeJobs(JOB_NAME, 30);

        JobInstances instances = jobInstanceDao.latestCompletedJobs(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 25);
        assertThat(instances.size(), is(25));

        for (JobInstance instance : instances) {
            assertThat(instance.getIdentifier().getBuildName(), is(JOB_NAME));
        }
    }

    @Test
    public void shouldGet25Builds_AlthoughFirst5AreCopied() throws Exception {
        long stageId = createSomeJobs(JOB_NAME, 30);
        createCopiedJobs(stageId, JOB_NAME, 5);

        JobInstances instances = jobInstanceDao.latestCompletedJobs(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 25);
        assertThat(instances.size(), is(25));

        for (JobInstance instance : instances) {
            assertThat(instance.getIdentifier().getBuildName(), is(JOB_NAME));
            assertThat("Should not have retrieved copied-over jobs", instance.isCopy(), is(false));
        }
    }

    @Test
    public void shouldGetMostRecentCompletedBuildsWhenTwoStagesWithIdenticalStageNamesAndBuildPlanNames()
            throws Exception {

        Pipeline otherPipeline = PipelineMother.passedPipelineInstance(PIPELINE_NAME + "2", STAGE_NAME, JOB_NAME);
        dbHelper.savePipelineWithStagesAndMaterials(otherPipeline);

        for (int i = 0; i < 2; i++) {
            Pipeline completedPipeline = PipelineMother.passedPipelineInstance(PIPELINE_NAME, STAGE_NAME, JOB_NAME);
            dbHelper.savePipelineWithStagesAndMaterials(completedPipeline);
        }

        assertThat(jobInstanceDao.latestCompletedJobs(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 25).size(), is(2));

        assertThat(jobInstanceDao.latestCompletedJobs(otherPipeline.getName(), STAGE_NAME, JOB_NAME, 25).size(), is(1));
    }

    @Test
    public void shouldIgnoreBuildingBuilds() throws Exception {
        JobInstance instance = JobInstanceMother.completed("shouldnotload", JobResult.Passed);
        jobInstanceDao.save(stageId, instance);
        JobInstance building = JobInstanceMother.building(JOB_NAME);
        JobInstance saved = jobInstanceDao.save(stageId, building);

        JobInstances instances =
                jobInstanceDao.latestCompletedJobs(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 25);
        assertThat(instances.size(), is(0));
    }

    @Test
    public void shouldCorrectly_getJobHistoryCount_findJobHistoryPage() throws Exception {
        // has a scheduled job
        long stageId = createSomeJobs(JOB_NAME, 2); // create 4 instances completed, scheduled, completed, scheduled
        createCopiedJobs(stageId, JOB_NAME, 2);

        JobInstance shouldNotLoadInstance = JobInstanceMother.completed("shouldnotload", JobResult.Passed); // create job with a different name
        jobInstanceDao.save(stageId, shouldNotLoadInstance);

        JobInstance building = JobInstanceMother.building(JOB_NAME); // create a building job
        JobInstance saved = jobInstanceDao.save(stageId, building);

        int jobHistoryCount = jobInstanceDao.getJobHistoryCount(PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        assertThat(jobHistoryCount, is(6));

        JobInstances instances = jobInstanceDao.findJobHistoryPage(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 4, 0);
        assertThat(instances.size(), is(4));

        assertThat(instances.get(0).getState(), is(JobState.Building));
        assertThat(instances.get(1).getState(), is(JobState.Completed));
        assertThat(instances.get(2).getState(), is(JobState.Scheduled));
        assertThat(instances.get(3).getState(), is(JobState.Completed));
        assertJobHistoryCorrectness(instances, JOB_NAME);

        instances = jobInstanceDao.findJobHistoryPage(PIPELINE_NAME, STAGE_NAME, JOB_NAME, 4, 4);
        assertThat(instances.size(), is(2));

        assertThat(instances.get(0).getState(), is(JobState.Scheduled));
        assertThat(instances.get(1).getState(), is(JobState.Scheduled));
        assertJobHistoryCorrectness(instances, JOB_NAME);
    }

    private void assertJobHistoryCorrectness(JobInstances instances, String jobName) {
        for (JobInstance instance : instances) {
            assertThat(instance.getIdentifier().getBuildName(), is(jobName));
            assertThat("Should not have retrieved copied-over jobs", instance.isCopy(), is(false));
        }
    }

    @Test
    public void shouldLoadRerunOfCounterValueForScheduledBuilds() {
        List<JobPlan> jobPlans = jobInstanceDao.orderedScheduledBuilds();
        assertThat(jobPlans.size(), is(2));
        assertThat(jobPlans.get(0).getIdentifier().getRerunOfCounter(), is(nullValue()));
        assertThat(jobPlans.get(1).getIdentifier().getRerunOfCounter(), is(nullValue()));

        dbHelper.passStage(savedStage);
        Stage stage = instanceFactory.createStageForRerunOfJobs(savedStage, a(JOB_NAME), schedulingContext, pipelineConfig.getStage(new CaseInsensitiveString(STAGE_NAME)), new TimeProvider(), "md5");
        dbHelper.saveStage(savedPipeline, stage, stage.getOrderId() + 1);

        jobPlans = jobInstanceDao.orderedScheduledBuilds();
        assertThat(jobPlans.size(), is(1));
        assertThat(jobPlans.get(0).getIdentifier().getRerunOfCounter(), is(savedStage.getCounter()));
    }

    @Test
    public void shouldGetAllScheduledBuildsInOrder() throws Exception {
        // in setup, we created 2 scheduled builds
        assertThat(jobInstanceDao.orderedScheduledBuilds().size(), is(2));

        JobIdentifier jobIdentifier = new JobIdentifier(PIPELINE_NAME, "LABEL-1", STAGE_NAME, "1", JOB_NAME);

        long newestId = schedule(JOB_NAME, stageId, new Date(10001), jobIdentifier);
        long olderId = schedule(JOB_NAME, stageId, new Date(10000), jobIdentifier);
        long oldestId = schedule(JOB_NAME, stageId, new Date(999), jobIdentifier);


        List<JobPlan> jobPlans = jobInstanceDao.orderedScheduledBuilds();
        assertThat(jobPlans.size(), is(5));
        assertJobInstance(jobPlans.get(0), oldestId, PIPELINE_NAME, STAGE_NAME);
        assertJobInstance(jobPlans.get(1), olderId, PIPELINE_NAME, STAGE_NAME);
        assertJobInstance(jobPlans.get(2), newestId, PIPELINE_NAME, STAGE_NAME);
    }

    private long schedule(String jobName, long stageId, Date date, JobIdentifier jobIdentifier) {
        JobInstance newest = new JobInstance(jobName);
        newest.setScheduledDate(date);
        jobInstanceDao.save(stageId, newest);

        jobInstanceDao.save(newest.getId(), new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArrayList<>(), -1, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null));

        return newest.getId();
    }

    private void assertJobInstance(JobPlan actual, long expect, String pipelineName, String stageName) {
        assertThat(actual.getPipelineName(), is(pipelineName));
        assertThat(actual.getStageName(), is(stageName));
        assertThat("JobInstance should match", actual.getJobId(), is(expect));
    }

    @Test
    public void shouldUpdateStateTransitions() throws Exception {
        JobInstance expected = scheduled(JOB_NAME, new Date(1000));
        jobInstanceDao.save(stageId, expected);
        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(expected.getId());
        assertThat(actual.getTransitions(), Matchers.iterableWithSize(1));
        expected.changeState(JobState.Assigned);
        jobInstanceDao.updateStateAndResult(expected);
        actual = jobInstanceDao.buildByIdWithTransitions(expected.getId());
        assertThat(actual.getTransitions(), Matchers.iterableWithSize(2));
        for (JobStateTransition transition : actual.getTransitions()) {
            assertThat(transition.getStageId(), is(stageId));
        }
    }

    @Test
    public void shouldUpdateBuildStatus() throws Exception {
        JobInstance expected = scheduled(JOB_NAME);
        jobInstanceDao.save(stageId, expected);
        expected.changeState(JobState.Building);
        jobInstanceDao.updateStateAndResult(expected);
        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(expected.getId());
        assertThat(actual.getState(), is(JobState.Building));
        assertThat(actual.getTransitions().size(), is(2));
    }

    @Test
    public void shouldUpdateAssignedInfo() throws Exception {
        JobInstance expected = scheduled(JOB_NAME);
        jobInstanceDao.save(stageId, expected);
        expected.changeState(JobState.Building);
        expected.setAgentUuid("uuid");
        jobInstanceDao.updateAssignedInfo(expected);
        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(expected.getId());
        assertThat(actual.getState(), is(JobState.Building));
        assertThat(actual.getTransitions().size(), is(2));
        assertThat(actual.getAgentUuid(), is("uuid"));
    }

    @Test
    public void shouldUpdateCompletingInfo() throws Exception {
        JobInstance expected = scheduled(JOB_NAME);
        jobInstanceDao.save(stageId, expected);
        expected.changeState(JobState.Completing);
        expected.setResult(JobResult.Failed);
        jobInstanceDao.updateStateAndResult(expected);
        JobInstance actual = jobInstanceDao.buildByIdWithTransitions(expected.getId());
        assertThat(actual.getState(), is(JobState.Completing));
        assertThat(actual.getTransitions().size(), is(2));
        assertThat(actual.getResult(), is(JobResult.Failed));
    }

    @Test
    public void shouldSaveTransitionsCorrectly() {
        JobInstance jobInstance = scheduled(projectOne, new Date(1));
        jobInstance.completing(JobResult.Failed, new Date(3));

        jobInstanceDao.save(stageId, jobInstance);

        JobInstance loaded = jobInstanceDao.buildByIdWithTransitions(jobInstance.getId());

        JobStateTransitions actualTransitions = loaded.getTransitions();
        assertThat(actualTransitions, Matchers.iterableWithSize(2));
        assertThat(actualTransitions.first().getCurrentState(), is(JobState.Scheduled));
    }

    @Test
    public void shouldSaveResources() {
        JobInstance instance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        instance.setIdentifier(new JobIdentifier(savedPipeline, savedStage, instance));
        JobPlan plan = new DefaultJobPlan(new Resources("something"), new ArrayList<>(),
                new ArrayList<>(), instance.getId(), instance.getIdentifier(), null, new EnvironmentVariables(), new EnvironmentVariables(), null);
        jobInstanceDao.save(instance.getId(), plan);
        JobPlan retrieved = jobInstanceDao.loadPlan(plan.getJobId());
        assertThat(retrieved, is(plan));
    }

    @Test
    public void shouldSaveJobAgentMetadata() {
        JobInstance instance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        instance.setIdentifier(new JobIdentifier(savedPipeline, savedStage, instance));
        ElasticProfile elasticProfile = new ElasticProfile("foo", "cd.go.elastic-agent:docker", Arrays.asList(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"))));
        JobPlan plan = new DefaultJobPlan(new Resources("something"), new ArrayList<>(),
                new ArrayList<>(), instance.getId(), instance.getIdentifier(), null, new EnvironmentVariables(), new EnvironmentVariables(), elasticProfile);
        jobInstanceDao.save(instance.getId(), plan);

        JobPlan retrieved = jobInstanceDao.loadPlan(plan.getJobId());
        assertThat(retrieved.getElasticProfile(), is(elasticProfile));
    }

    @Test
    public void shouldNotThrowUpWhenJobAgentMetadataIsNull() {
        JobInstance instance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        instance.setIdentifier(new JobIdentifier(savedPipeline, savedStage, instance));
        ElasticProfile elasticProfile = null;
        JobPlan plan = new DefaultJobPlan(new Resources("something"), new ArrayList<>(),
                new ArrayList<>(), instance.getId(), instance.getIdentifier(), null, new EnvironmentVariables(), new EnvironmentVariables(), elasticProfile);
        jobInstanceDao.save(instance.getId(), plan);

        JobPlan retrieved = jobInstanceDao.loadPlan(plan.getJobId());
        assertThat(retrieved.getElasticProfile(), is(elasticProfile));
    }

    @Test
    public void shouldSaveEnvironmentVariables() {
        JobInstance instance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        instance.setIdentifier(new JobIdentifier(savedPipeline, savedStage, instance));

        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("VARIABLE_NAME", "variable value");
        variables.add("TRIGGER_VAR", "junk val");
        JobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(),
                new ArrayList<>(), instance.getId(),
                instance.getIdentifier(), null, variables, new EnvironmentVariables(), null);
        jobInstanceDao.save(instance.getId(), plan);
        environmentVariableDao.save(savedPipeline.getId(), EnvironmentVariableType.Trigger, environmentVariables("TRIGGER_VAR", "trigger val"));
        JobPlan retrieved = jobInstanceDao.loadPlan(plan.getJobId());
        assertThat(retrieved.getVariables(), is(plan.getVariables()));
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        retrieved.applyTo(context);
        assertThat(context.getProperty("VARIABLE_NAME"), is("variable value"));
        assertThat(context.getProperty("TRIGGER_VAR"), is("trigger val"));
    }

    private EnvironmentVariables environmentVariables(String name, String value) {
        return new EnvironmentVariables(Arrays.asList(new EnvironmentVariable(name, value, false)));
    }

    @Test
    public void shouldLoadEnvironmentVariablesForScheduledJobs() {
        JobInstance newInstance = new JobInstance(JOB_NAME);
        newInstance.schedule();
        JobInstance instance = jobInstanceDao.save(stageId, newInstance);
        instance.setIdentifier(new JobIdentifier(savedPipeline, savedStage, instance));

        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("VARIABLE_NAME", "variable value");
        variables.add("TRIGGER_VAR", "junk val");
        JobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(),
                new ArrayList<>(), instance.getId(),
                instance.getIdentifier(), null, variables, new EnvironmentVariables(), null);
        jobInstanceDao.save(instance.getId(), plan);

        environmentVariableDao.save(savedPipeline.getId(), EnvironmentVariableType.Trigger, environmentVariables("TRIGGER_VAR", "trigger val"));

        List<JobPlan> retrieved = jobInstanceDao.orderedScheduledBuilds();

        JobPlan reloadedPlan = planForJob(retrieved, plan.getJobId());
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        reloadedPlan.applyTo(context);
        assertThat(reloadedPlan.getVariables(), is(plan.getVariables()));
        assertThat(context.getProperty("VARIABLE_NAME"), is("variable value"));
        assertThat(context.getProperty("TRIGGER_VAR"), is("trigger val"));
    }

    private JobPlan planForJob(List<JobPlan> retrieved, long expectedJobId) {
        for (JobPlan loadedJobPlan : retrieved) {
            if (loadedJobPlan.getJobId() == expectedJobId) {
                return loadedJobPlan;
            }
        }
        return null;
    }

    @Test
    public void shouldLoadArtifactPropertiesGeneratorsInOrderForAssignment() {
        ArtifactPropertiesGenerator prop1 = new ArtifactPropertiesGenerator("test1", "src", "//xpath");
        ArtifactPropertiesGenerator prop2 = new ArtifactPropertiesGenerator("test2", "src", "//xpath");
        ArtifactPropertiesGenerator prop3 = new ArtifactPropertiesGenerator("test3", "src", "//xpath");
        ArtifactPropertiesGenerator prop4 = new ArtifactPropertiesGenerator("test4", "src", "//xpath");
        ArtifactPropertiesGenerator prop5 = new ArtifactPropertiesGenerator("test5", "src", "//xpath");

        JobInstance instance = jobInstanceDao.save(stageId, new JobInstance(projectOne));
        instance.setIdentifier(new JobIdentifier(savedPipeline, savedStage, instance));
        JobPlan savedPlan = new DefaultJobPlan(new Resources(), artifactPlans(), Arrays.asList(prop1, prop2, prop3, prop4, prop5), instance.getId(), instance.getIdentifier(), null, new EnvironmentVariables(), new EnvironmentVariables(), null);

        jobInstanceDao.save(instance.getId(), savedPlan);

        JobPlan plan = findPlan(jobInstanceDao.orderedScheduledBuilds(), projectOne);
        List<ArtifactPropertiesGenerator> generators = plan.getPropertyGenerators();
        assertThat(generators.size(), is(5));
        assertThat(generators.get(0), is(prop1));
        assertThat(generators.get(1), is(prop2));
        assertThat(generators.get(2), is(prop3));
        assertThat(generators.get(3), is(prop4));
        assertThat(generators.get(4), is(prop5));
    }

    @Test
    public void shouldLoadArtifactsAndResourcesForAssignment() {
        JobInstance instance = jobInstanceDao.save(stageId, new JobInstance(projectOne));
        instance.setIdentifier(new JobIdentifier(savedPipeline, savedStage, instance));
        Resources resources = new Resources("one, two, three");
        JobPlan savedPlan = new DefaultJobPlan(resources, artifactPlans(), new ArrayList<>(), instance.getId(), instance.getIdentifier(), null, new EnvironmentVariables(), new EnvironmentVariables(), null);

        jobInstanceDao.save(instance.getId(), savedPlan);

        final List<JobPlan> planList = jobInstanceDao.orderedScheduledBuilds();

        final List<JobPlan> plans = findPlans(planList, projectOne);

        assertThat(plans.size(), is(1));
        assertThat(plans.get(0).getResources(), is(resources));
    }

    @Test
    public void shouldLoadJobIdentifierForAssignment() {
        JobInstance jobInstance = scheduled(projectOne);
        jobInstanceDao.save(stageId, jobInstance);

        JobPlan job = findPlan(jobInstanceDao.orderedScheduledBuilds(), projectOne);
        assertThat(job.getIdentifier(), is(jobIdentifier(jobInstance)));
    }

    @Test
    public void shouldLoadAgentUuidForAssignment() {
        JobInstance jobInstance = scheduled(projectOne);
        jobInstance.setAgentUuid("uuid1");
        jobInstanceDao.save(stageId, jobInstance);

        JobPlan job = findPlan(jobInstanceDao.orderedScheduledBuilds(), projectOne);
        assertThat(job.getAgentUuid(), is("uuid1"));
    }

    @Test
    public void shouldLoadRunOnAllAgentsForAssignment() {
        JobInstance jobInstance = scheduled(projectOne);
        jobInstance.setRunOnAllAgents(true);
        jobInstanceDao.save(stageId, jobInstance);

        JobInstance reloaded = jobInstanceDao.buildByIdWithTransitions(jobInstance.getId());
        assertThat(reloaded.isRunOnAllAgents(), is(true));
    }

    @Test
    public void shouldLoadRunMultipleInstanceForAssignment() {
        JobInstance jobInstance = scheduled(projectOne);
        jobInstance.setRunMultipleInstance(true);
        jobInstanceDao.save(stageId, jobInstance);

        JobInstance reloaded = jobInstanceDao.buildByIdWithTransitions(jobInstance.getId());
        assertThat(reloaded.isRunMultipleInstance(), is(true));
    }

    private JobIdentifier jobIdentifier(JobInstance jobInstance) {
        return new JobIdentifier(savedPipeline, savedStage, jobInstance);
    }

    private JobPlan findPlan(List<JobPlan> list, String jobName) {
        final List<JobPlan> planList = findPlans(list, jobName);

        if (planList.size() > 0) {
            return planList.get(0);
        }

        return null;
    }

    private List<JobPlan> findPlans(List<JobPlan> list, String jobName) {
        List<JobPlan> result = new ArrayList<>();
        for (JobPlan buildNameBean : list) {
            if (jobName.equals(buildNameBean.getName())) {
                result.add(buildNameBean);
            }
        }

        return result;
    }

    @Test
    public void shouldGetLatestInProgressBuildByAgentUuid() {
        JobInstance buildingJob = building(projectOne, new Date(1));
        final String uuid = "uuid";
        buildingJob.setAgentUuid(uuid);
        jobInstanceDao.save(stageId, buildingJob);

        JobInstance completedJob = JobInstanceMother.completed("anotherBuild", JobResult.Passed);
        completedJob.setAgentUuid(uuid);
        jobInstanceDao.save(stageId, completedJob);

        JobInstance jobInstance = jobInstanceDao.getLatestInProgressBuildByAgentUuid(uuid);
        assertThat(jobInstance, hasSameId(buildingJob));
        assertThat(jobInstance.getIdentifier(), is(jobIdentifier(jobInstance)));
    }

    @Test
    public void shouldGetInProgressJobs() throws Exception {
        JobInstance buildingJob1 = building(projectOne, new Date(1));
        buildingJob1.setAgentUuid("uuid1");
        jobInstanceDao.save(stageId, buildingJob1);

        JobInstance buildingJob2 = building("project2", new Date(2));
        buildingJob2.setAgentUuid("uuid2");
        jobInstanceDao.save(stageId, buildingJob2);

        JobInstance buildingJob3 = building("project3", new Date(3));
        buildingJob3.setAgentUuid("uuid3");
        jobInstanceDao.save(stageId, buildingJob3);

        List<String> liveAgentIds = new ArrayList<String>() {
            {
                add("uuid1");
                add("uuid2");
            }
        };
        JobInstances list = jobInstanceDao.findHungJobs(liveAgentIds);
        assertThat(list.size(), is(1));
        JobInstance reloaded = list.get(0);
        assertThat(reloaded, hasSameId(buildingJob3));
        assertThat(reloaded.getIdentifier(), is(jobIdentifier(buildingJob3)));
    }


    @Test
    public void shouldIgnore() {
        JobInstance instance = scheduled(projectOne);
        jobInstanceDao.save(stageId, instance);
        jobInstanceDao.ignore(instance);

        JobInstance reloaded = jobInstanceDao.buildByIdWithTransitions(instance.getId());
        assertThat(reloaded.isIgnored(), is(true));
    }

    @Test
    public void shouldGetCompletedJobsOnAgentForARange() {
        String agentUuid = "special_uuid";
        JobInstance buildingJob = building("job1", new Date(1));
        buildingJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageId, buildingJob);

        JobInstance completedJob = completed("job2", JobResult.Passed, new Date(1));
        completedJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageId, completedJob);

        JobInstance cancelledJob = cancelled("job3");
        cancelledJob.setAgentUuid("something_different");//Different UUID. Should not be considered
        jobInstanceDao.save(stageId, cancelledJob);

        JobInstance rescheduledJob = rescheduled("rescheduled", agentUuid);
        jobInstanceDao.save(stageId, rescheduledJob);
        jobInstanceDao.ignore(rescheduledJob);

        List<JobInstance> jobInstances = jobInstanceDao.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.stage, SortOrder.ASC, 0, 10);
        assertThat(jobInstances.size(), is(2));
        JobInstance actual = jobInstances.get(0);
        assertThat(actual.getName(), is(completedJob.getName()));
        completedJob.setIdentifier(actual.getIdentifier());
        assertThat(actual, is(completedJob));

        actual = jobInstances.get(1);
        assertThat(actual.getName(), is(rescheduledJob.getName()));
        rescheduledJob.setIdentifier(actual.getIdentifier());
        assertThat(actual, is(rescheduledJob));
    }

    @Test
    public void shouldGetTotalNumberOfCompletedJobsForAnAgent() {
        String agentUuid = "special_uuid";
        JobInstance buildingJob = building("job1", new Date(1));
        buildingJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageId, buildingJob);

        JobInstance completedJob = completed("job2", JobResult.Passed, new Date(1));
        completedJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageId, completedJob);

        JobInstance rescheduledJob = rescheduled("rescheduled", agentUuid);
        jobInstanceDao.save(stageId, rescheduledJob);
        jobInstanceDao.ignore(rescheduledJob);

        JobInstance cancelledJob = cancelled("job3");
        cancelledJob.setAgentUuid("something_different");//Different UUID. Should not be counted
        jobInstanceDao.save(stageId, cancelledJob);

        JobInstance simpleJob = failed("simpleJob");
        simpleJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageId, simpleJob);

        assertThat(jobInstanceDao.totalCompletedJobsOnAgent(agentUuid), is(3));
    }

    private List<ArtifactPlan> artifactPlans() {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "src", "dest"));
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, "src1", "dest2"));
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.unit, "unit", "unit"));
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.unit, "integration", "integration"));
        return artifactPlans;
    }

    private List<ArtifactPropertiesGenerator> artifactPropertiesGenerators() {
        List<ArtifactPropertiesGenerator> artifactPropertiesGenerators = new ArrayList<>();
        artifactPropertiesGenerators.add(new ArtifactPropertiesGenerator("log", "src", "path"));
        artifactPropertiesGenerators.add(new ArtifactPropertiesGenerator("text", "src", "path"));
        return artifactPropertiesGenerators;
    }
}
