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

package com.thoughtworks.go.server.dao.sparql;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.testinfo.FailingTestsInPipeline;
import com.thoughtworks.go.domain.testinfo.FailureDetails;
import com.thoughtworks.go.domain.testinfo.StageTestRuns;
import com.thoughtworks.go.domain.testinfo.TestInformation;
import com.thoughtworks.go.domain.testinfo.TestStatus;
import com.thoughtworks.go.domain.testinfo.TestSuite;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.service.XmlApiService;
import com.thoughtworks.go.server.service.result.SubsectionLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.studios.shine.cruise.builder.JunitXML;
import com.thoughtworks.studios.shine.cruise.stage.StagesQuery;
import com.thoughtworks.studios.shine.cruise.stage.StagesQueryCache;
import com.thoughtworks.studios.shine.cruise.stage.details.LazyStageGraphLoader;
import com.thoughtworks.studios.shine.cruise.stage.details.StageResourceImporter;
import com.thoughtworks.studios.shine.cruise.stage.details.StageStorage;
import com.thoughtworks.studios.shine.net.StubGoURLRepository;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.studios.shine.cruise.builder.JunitXML.junitXML;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ShineDaoIntegrationTest {
    private ShineDao shineDao;
    @Autowired private Localizer localizer;
    private SubsectionLocalizedOperationResult result;
    @Autowired private StageStorage stageStorage;
    @Autowired private StagesQueryCache stagesQueryCache;
    @Autowired private StageService stageService;
    @Autowired private PipelineHistoryService pipelineHistoryService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private XmlApiService xmlApiService;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private DatabaseAccessHelper dbHelper;

    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private SystemEnvironment systemEnvironment;

    private TempFiles tempFiles;
    private StubGoURLRepository goURLRepository;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private Pipeline pipeline;
    private StageIdentifier stageId;

    private TestFailureSetup failureSetup;

    @Before public void setUp() throws Exception {
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        result = new SubsectionLocalizedOperationResult();
        tempFiles = new TempFiles();
        File tempFolder = tempFiles.createUniqueFolder("artifacts");

        String artifactsRoot = tempFolder.getAbsolutePath();
        stageStorage.clear();
        StageResourceImporter importer = new StageResourceImporter(artifactsRoot, xmlApiService, stageService, pipelineHistoryService,systemEnvironment);

        LazyStageGraphLoader graphLoader = new LazyStageGraphLoader(importer, stageStorage, systemEnvironment);
        StagesQuery stagesQuery = new StagesQuery(graphLoader, stagesQueryCache);
        shineDao = new ShineDao(stagesQuery, stageService, pipelineHistoryService);
        goURLRepository = new StubGoURLRepository("http://localhost:8153", artifactsRoot);

        failureSetup = new TestFailureSetup(materialRepository, dbHelper, pipelineTimeline, configHelper, transactionTemplate);

        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstance(true, null, goURLRepository);
        pipeline = savedStage.pipeline;
        stageId = savedStage.stageId;
    }

    @After public void tearDown() throws Exception {
        tempFiles.cleanUp();
        stagesQueryCache.clear();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldBeAbleToRetriveFailedTestsForAStageIdentifier() throws Exception {
        List<TestSuite> suites = shineDao.failedTestsFor(stageId);

        assertThat(suites.size(), is(1));
        assertThat(suites.get(0).fullName(), is("testSuite1"));
        List<TestInformation> tests = suites.get(0).tests();
        assertThat(tests.size(), is(2));
        assertThat(tests.get(0).getName(), is("test1"));
        assertThat(tests.get(0).getStatus(), Is.is(TestStatus.Failure));
        assertThat(tests.get(0).getJobNames(), hasItems("NixBuild", "WinBuild"));
        assertThat(tests.get(1).getName(), is("test2"));
        assertThat(tests.get(1).getStatus(), is(TestStatus.Error));
        assertThat(tests.get(1).getJobNames(), hasItems("WinBuild"));
    }

    @Test
    public void shouldBeAbleToRetriveStageTestRunsWithLazyGraphLoader() throws Exception {
        StageTestRuns stageTestRuns = shineDao.failedBuildHistoryForStage(stageId, result);
        List<FailingTestsInPipeline> failingTestsInPipelines = stageTestRuns.failingTestsInPipelines();

        assertThat(failingTestsInPipelines.toString(), failingTestsInPipelines.size(), is(1));
        List<TestSuite> suites = failingTestsInPipelines.get(0).failingSuites();
        assertThat(suites.size(), is(1));
        assertThat(suites.get(0).fullName(), is("testSuite1"));
        List<TestInformation> tests = suites.get(0).tests();
        assertThat(tests.size(), is(2));
        assertThat(tests.get(0).getName(), is("test1"));
        assertThat(tests.get(0).getStatus(), is(TestStatus.Failure));
        assertThat(tests.get(0).getJobNames(), hasItems("NixBuild", "WinBuild"));
        assertThat(tests.get(1).getName(), is("test2"));
        assertThat(tests.get(1).getStatus(), is(TestStatus.Error));
        assertThat(tests.get(1).getJobNames(), hasItems("WinBuild"));
    }

    @Test
    public void shouldBeAbleToPullUpStackTrace() {
        FailureDetails failureDetails = shineDao.failureDetailsForTest(new JobIdentifier(stageId, "WinBuild"), "testSuite1", "test1", result);
        assertThat(failureDetails.getMessage(), is("Something assert failed..."));
        assertThat(failureDetails.getStackTrace(), is("junit.framework.AssertionFailedError: Something assert failed..."));
        failureDetails = shineDao.failureDetailsForTest(new JobIdentifier(stageId, "WinBuild"), "testSuite1", "test2", result);
        assertThat(failureDetails.getMessage(), is("Something went wrong"));
        assertThat(failureDetails.getStackTrace(), is("com.foo.MyException: Something went wrong..."));
        failureDetails = shineDao.failureDetailsForTest(new JobIdentifier(stageId, "NixBuild"), "testSuite1", "test1", result);
        assertThat(failureDetails.getMessage(), is("Something assert failed..."));
        assertThat(failureDetails.getStackTrace(), is("junit.framework.AssertionFailedError: Something assert failed..."));
    }

    @Test
    public void shouldHandleExceptionsWhenTryingToPullUpTrace() throws Exception {
        stageId = failureSetup.setupPipelineInstance(false, null, goURLRepository).stageId;

        FailureDetails details = shineDao.failureDetailsForTest(new JobIdentifier(stageId, "job1"), "testSuite1", "test1", result);

        assertThat(details.getMessage(), is("NOT_YET_AVAILABLE"));
        assertThat(details.getStackTrace(), is("NOT_YET_AVAILABLE"));
        assertThat("not successful", result.isSuccessful(), is(false));
        assertThat(result.replacementContent(localizer), is("Unable to retrieve failure results."));
    }
    
    @Test
    public void shouldRetriveHistoricalFailureWithNoTests() throws Exception {
        failureSetup.setupPipelineInstance(true, null, goURLRepository);

        StageIdentifier stageId3 = failureSetup.setupPipelineInstance(true, null, goURLRepository).stageId;


        StageTestRuns stageTestRuns = shineDao.failedBuildHistoryForStage(stageId3, result);
        List<FailingTestsInPipeline> failingTestsInPipelines = stageTestRuns.failingTestsInPipelines();

        assertThat(failingTestsInPipelines.size(), is(3));
    }

    @Test
    public void shouldRetriveHistoricalFailureInformation() throws Exception {
        Pipeline pipeline1 = pipeline;

        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstance(true, null, goURLRepository);
        StageIdentifier stageId2 = savedStage.stageId;
        Pipeline pipeline2 = savedStage.pipeline;


        StageTestRuns stageTestRuns = shineDao.failedBuildHistoryForStage(stageId2, result);
        List<FailingTestsInPipeline> failingTestsInPipelines = stageTestRuns.failingTestsInPipelines();
        assertThat(failingTestsInPipelines.size(), is(2));
        FailingTestsInPipeline failingPipeline2 = failingTestsInPipelines.get(0);
        assertThat(failingPipeline2.getLabel(), is(pipeline2.getLabel()));
        assertThat(failingPipeline2.failingSuites().size(), is(0));
        FailingTestsInPipeline failingPipeline1 = failingTestsInPipelines.get(1);
        assertThat(failingPipeline1.getLabel(), is(pipeline1.getLabel()));
        assertThat(failingPipeline1.failingSuites().size(), is(1));
    }

    @Test
    public void shouldRetriveTheRightHistoricalFailureInformationForPipelinesWithSameLabel() throws Exception {
        Pipeline pipeline1 = pipeline;

        failureSetup.setupPipelineInstance(true, pipeline1.getLabel(), goURLRepository);

        StageIdentifier stageId3 = failureSetup.setupPipelineInstance(true, pipeline1.getLabel(), goURLRepository).stageId;

        StageTestRuns stageTestRuns = shineDao.failedBuildHistoryForStage(stageId3, result);
        List<FailingTestsInPipeline> failingTestsInPipelines = stageTestRuns.failingTestsInPipelines();
        assertThat(failingTestsInPipelines.size(), is(3));
        FailingTestsInPipeline failingPipeline3 = failingTestsInPipelines.get(0);
        assertThat(failingPipeline3.getLabel(), is(pipeline1.getLabel()));
        assertThat(failingPipeline3.failingSuites().size(), is(0));
        FailingTestsInPipeline failingPipeline2 = failingTestsInPipelines.get(1);
        assertThat(failingPipeline2.getLabel(), is(pipeline1.getLabel()));
        assertThat(failingPipeline2.failingSuites().size(), is(0));
        FailingTestsInPipeline failingPipeline1 = failingTestsInPipelines.get(2);
        assertThat(failingPipeline1.getLabel(), is(pipeline1.getLabel()));
        List<TestSuite> suites = failingPipeline1.failingSuites();
        assertThat(suites.size(), is(1));
        assertThat(suites.get(0).countOfStatus(TestStatus.Error), is(1));
        assertThat(suites.get(0).countOfStatus(TestStatus.Failure), is(1));
    }

    @Test public void shouldHandleExceptionsFromSelectWhileRenderingFailingTestsForStage() throws Exception {
        stageId = failureSetup.setupPipelineInstance(false, null, goURLRepository).stageId;

        shineDao.failedBuildHistoryForStage(stageId, result);
        assertThat("not successful", result.isSuccessful(), is(false));
        assertThat(result.replacementContent(localizer), is("Unable to retrieve failure results."));
    }

    @Test
    public void shouldContainLocatorForFailedJob() throws Exception {
        StageTestRuns stageTestRuns = failingStageHistory();
        List<FailingTestsInPipeline> failingTestsInPipelines = stageTestRuns.failingTestsInPipelines();

        FailingTestsInPipeline failingPipeline7 = failingTestsInPipelines.get(0);
        FailingTestsInPipeline failingPipeline6 = failingTestsInPipelines.get(1);
        List<TestInformation> failingTests7 = failingPipeline7.failingSuites().get(0).tests();
        assertThat(failingTests7.size(), is(1));
        assertThat(failingTests7.get(0).getJobs().size(), is(2));
        assertThat(failingTests7.get(0).getJobs().get(0).getBuildName(), is("NixBuild"));
        assertThat(failingTests7.get(0).getJobs().get(0).buildLocator(), is("foo-pipeline/3/bar-stage/1/NixBuild"));
        assertThat(failingTests7.get(0).getJobs().get(1).buildLocator(), is("foo-pipeline/3/bar-stage/1/WinBuild"));
        List<TestInformation> failingTests6 = failingPipeline6.failingSuites().get(0).tests();
        assertThat(failingTests6.size(), is(2));
        assertThat(failingTests6.get(0).getJobs().size(), is(1));
        assertThat(failingTests6.get(0).getJobs().get(0).getBuildName(), is("WinBuild"));
        assertThat(failingTests6.get(0).getJobs().get(0).buildLocator(), is("foo-pipeline/2/bar-stage/1/WinBuild"));

        assertThat(failingTests6.get(1).getJobs().size(), is(1));
        assertThat(failingTests6.get(1).getJobs().get(0).getBuildName(), is("WinBuild"));
        assertThat(failingTests6.get(1).getJobs().get(0).buildLocator(), is("foo-pipeline/2/bar-stage/1/WinBuild"));
    }

    @Test
    public void shouldPopulateUsersThatTriggeredTheBuild() {
        StageTestRuns stageTestRuns = failingStageHistory();
        List<FailingTestsInPipeline> failingTestsInPipelines = stageTestRuns.failingTestsInPipelines();

        List<String> users = failingTestsInPipelines.get(1).users();
        assertThat(users.size(), is(2));
        assertThat(users, hasItem("fooUser"));
        assertThat(users, hasItem("RRR & DR"));
        assertThat(users.size(), is(2));
        users = failingTestsInPipelines.get(0).users();
        assertThat(users.size(), is(1));
        assertThat(users, hasItem("blahUser"));
    }

    private StageTestRuns failingStageHistory() {
        failureSetup.setupPipelineInstance(true, null, Arrays.asList(
                new Modification("fooUser", "revision 15", "loser@mail.com", new Date(), "15"),
                new Modification("RRR & DR", "revision 16", "boozer@mail.com", new Date(), "16")),
                new TestFailureSetup.TestResultsStubbing() {
                    public void stub(Stage stage) {
                        JunitXML junit1 = junitXML("testSuite1", 2).failed(2).errored(1);
                        junit1.registerStubContent(goURLRepository, "pipelines/" + stage.getJobInstances().get(0).getIdentifier().artifactLocator("junit") + "/junit/");
                    }
                }, new Date());

        stageId = failureSetup.setupPipelineInstance(true, null, Arrays.asList(new Modification("blahUser", "revision 17", "hello@world", new Date(), "17")),
                new TestFailureSetup.TestResultsStubbing() {
                    public void stub(Stage stage) {
                        JunitXML junit1 = junitXML("testSuite1", 2).failed(2).errored(1);
                        junit1.registerStubContent(goURLRepository, "pipelines/" + stage.getJobInstances().get(0).getIdentifier().artifactLocator("junit") + "/junit/");

                        JunitXML junit2 = junitXML("testSuite1", 2).errored(1);
                        junit2.registerStubContent(goURLRepository, "pipelines/" + stage.getJobInstances().get(1).getIdentifier().artifactLocator("junit") + "/junit/");
                    }
                }, new Date()).stageId;

        return shineDao.failedBuildHistoryForStage(stageId, result);
    }

    @Test
    public void shouldContainTotalTestCounts() throws Exception {
        StageTestRuns stageTestRuns = failingStageHistory();
        assertThat(stageTestRuns.numberOfTests(), is(4));
    }

    @Test
    public void shouldReturnTheTotalNumberOfFailuresAndErrorsForAPassedStage() {
        StageIdentifier stageId = failureSetup.setupPipelineInstance(false, null, Arrays.asList(new Modification("blahUser", "revision 17", "hello@world", new Date(), "17")),
                       new TestFailureSetup.TestResultsStubbing() {
                           public void stub(Stage stage) {
                               dbHelper.passStage(stage);
                               JunitXML junit1 = junitXML("testSuite1", 2).failed(2).errored(1);
                               junit1.registerStubContent(goURLRepository, "pipelines/" + stage.getJobInstances().get(0).getIdentifier().artifactLocator("junit") + "/junit/");

                               JunitXML junit2 = junitXML("testSuite1", 2).errored(1);
                               junit2.registerStubContent(goURLRepository, "pipelines/" + stage.getJobInstances().get(1).getIdentifier().artifactLocator("junit") + "/junit/");
                           }
                       }, new Date()).stageId;

        StageTestRuns testRuns = shineDao.failedBuildHistoryForStage(stageId, result);

        assertThat(testRuns.numberOfTests(), is(4));
        assertThat(testRuns.totalErrorCount(), is(2));
        assertThat(testRuns.totalFailureCount(), is(1));
    }
}
