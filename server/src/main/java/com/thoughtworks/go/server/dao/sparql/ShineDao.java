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

import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.testinfo.FailureDetails;
import com.thoughtworks.go.domain.testinfo.StageTestRuns;
import com.thoughtworks.go.domain.testinfo.TestStatus;
import com.thoughtworks.go.domain.testinfo.TestSuite;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.service.PipelineInstanceLoader;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.cruise.stage.StagesQuery;
import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.xunit.XUnitOntology;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;


/**
 * @understands how to get data out of shine
 */
public class ShineDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShineDao.class);
    private StagesQuery stagesQuery;
    private final StageService stageService;

    public ShineDao(StagesQuery stagesQuery, StageService stageService, PipelineInstanceLoader pipelineInstanceLoader) {
        this.stagesQuery = stagesQuery;
        this.stageService = stageService;
    }

    public List<TestSuite> failedTestsFor(final StageIdentifier stageId) {
        StageTestRuns stageTestRuns = getTestCount(stageId);
        final List<StageIdentifier> stageIdentifierList = Arrays.asList(stageId);
        populateFailingTests(stageTestRuns, getFailedTests(stageIdentifierList));
        populateUsers(stageTestRuns, getCommitters(stageIdentifierList));
        return stageTestRuns.failingTestSuitesForNthPipelineRun(0);
    }

    public StageTestRuns failedBuildHistoryForStage(StageIdentifier stageId, LocalizedOperationResult result) {
        try {
            StageTestRuns stageTestRuns = getTestCount(stageId);
            List<StageIdentifier> failedStageIds = stageService.findRunForStage(stageId);
            populateFailingTests(stageTestRuns, getFailedTests(failedStageIds));
            populateUsers(stageTestRuns, getCommitters(failedStageIds));
            stageTestRuns.removeDuplicateTestEntries();
            return stageTestRuns;
        } catch (RuntimeException e) {
            LOGGER.error("can not retrieve shine test history!", e);
            result.connectionError(LocalizedMessage.unableToRetrieveFailureResults());
            return new StageTestRuns(0, 0, 0);
        }
    }

    public FailureDetails failureDetailsForTest(JobIdentifier jobId, String suiteName, String testCaseName, LocalizedOperationResult result) {
        String selectTestCase = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX xunit: <" + XUnitOntology.URI + "> "
                + "PREFIX cruise: <" + GoOntology.URI + "> "
                + "\n"
                + "SELECT ?failureMessage ?stackTrace {\n"
                + "  ?pipeline cruise:pipelineName " + s(jobId.getPipelineName()) + " .\n"
                + "  ?pipeline cruise:pipelineCounter " + i(jobId.getPipelineCounter()) + " .\n"
                + "  ?pipeline cruise:hasStage ?stage .\n"
                + "  ?stage cruise:stageName " + s(jobId.getStageName()) + " .\n"
                + "  ?stage cruise:stageCounter " + i(jobId.getStageCounter()) + " .\n"
                + "  ?stage cruise:hasJob ?job .\n"
                + "  ?job cruise:jobName " + s(jobId.getBuildName()) + " .\n"
                + "  ?job xunit:hasTestCase ?testCase .\n"
                + "  ?testCase a xunit:TestCase .\n"
                + "  ?testCase xunit:testCaseName " + s(testCaseName) + " .\n"
                + "  ?testCase xunit:testSuiteName  " + s(suiteName) + " .\n"
                + "  ?testCase xunit:hasFailure ?failure .\n"
                + "  ?failure a xunit:Failure .\n"
                + "  ?failure xunit:failureMessage ?failureMessage .\n"
                + "  ?failure xunit:failureStackTrace ?stackTrace "
                + "}";

        try {
            return stagesQuery.select(selectTestCase, Arrays.asList(jobId.getStageIdentifier()), new RdfResultMapper<FailureDetails>() {
                public FailureDetails map(BoundVariables aRow) {
                    return new FailureDetails(aRow.getAsString("failureMessage"), aRow.getAsString("stackTrace"));
                }
            }).get(0);
        } catch (RuntimeException e) {
            LOGGER.error("can not retrieve shine test history!", e);
            result.connectionError(LocalizedMessage.unableToRetrieveFailureResults());
            return FailureDetails.nullFailureDetails();
        }
    }

    private StageTestRuns getTestCount(StageIdentifier stageId) {
        String selectTestCase =
                "PREFIX xunit: <" + XUnitOntology.URI + "> "
                        + "\n"
                        + "SELECT ?testCase ?failure ?error {\n"
                        + "  ?testCase a xunit:TestCase .\n"
                        + "  OPTIONAL {\n"
                        + "     ?testCase xunit:hasFailure ?failure .\n"
                        + "     ?failure xunit:isError ?error .\n"
                        + "  }"
                        + "}";

        List<TestCaseResultModel> testCounts = stagesQuery.select(selectTestCase, Arrays.asList(stageId), new RdfResultMapper<TestCaseResultModel>() {
            public TestCaseResultModel map(BoundVariables aRow) {
                boolean error = Boolean.TRUE.equals(aRow.getBoolean("error"));
                return new TestCaseResultModel(aRow.getAsString("failure") != null, error);
            }
        });
        int totalCount = testCounts.size();
        int failuresCount = computeFailureCounts(testCounts);
        int errorsCount = computeErrorCounts(testCounts);
        return new StageTestRuns(totalCount, failuresCount, errorsCount);
    }

    private int computeErrorCounts(List<TestCaseResultModel> testCounts) {
        int count = 0;
        for (TestCaseResultModel testCaseResultModel : testCounts) {
            if (testCaseResultModel.hasFailure() && testCaseResultModel.hasError()) {
                count++;
            }
        }
        return count;
    }

    private int computeFailureCounts(List<TestCaseResultModel> testCaseResultModels) {
        int count = 0;
        for (TestCaseResultModel testCaseResult : testCaseResultModels) {
            if (testCaseResult.hasFailure() && !testCaseResult.hasError()) {
                count++;
            }
        }
        return count;
    }

    private List<PipelineCommiter> getCommitters(List<StageIdentifier> failedStageIds) {
        String committersForStage =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "PREFIX xunit: <" + XUnitOntology.URI + "> "
                        + "PREFIX cruise: <" + GoOntology.URI + "> "
                        + "\n"
                        + "SELECT DISTINCT ?failedPipelineCounter ?failedPipelineLabel ?user {\n"
                        + "       ?pipeline cruise:pipelineCounter ?failedPipelineCounter .\n"
                        + "       ?pipeline cruise:pipelineLabel ?failedPipelineLabel .\n"
                        + "       ?pipelineTrigger a cruise:ChangeSet .\n"
                        + "       OPTIONAL {"
                        + "         ?pipelineTrigger cruise:user ?user .\n"
                        + "       }"
                        + "} ORDER BY ?failedPipelineCounter ?user";
        return stagesQuery.select(committersForStage, failedStageIds, new RdfResultMapper<PipelineCommiter>() {

            public PipelineCommiter map(BoundVariables aRow) {
                String userName = aRow.getString("user");
                Integer failedPipelineCounter = aRow.getInt("failedPipelineCounter");
                String failedPipelineLabel = aRow.getString("failedPipelineLabel");
                return new PipelineCommiter(userName, failedPipelineCounter, failedPipelineLabel);
            }
        });

    }

    private void populateUsers(StageTestRuns stageTestRuns, List<PipelineCommiter> commiters) {
        for (PipelineCommiter commiter : commiters) {
            if (!StringUtils.isEmpty(commiter.getUserName())) {
                stageTestRuns.addUser(commiter.getFailedPipelineCounter(), commiter.getFailedPipelineLabel(), commiter.getUserName());
            }
        }
    }

    private List<TestCaseModel> getFailedTests(List<StageIdentifier> failedStageIds) {
        String selectFailingTests =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "PREFIX xunit: <" + XUnitOntology.URI + "> "
                        + "PREFIX cruise: <" + GoOntology.URI + "> "
                        + "\n"
                        + "SELECT DISTINCT ?testSuiteName ?testCaseName ?isError ?jobName ?failedPipelineName ?failedPipelineCounter ?failedPipelineLabel ?failedStageName ?failedStageCounter {\n"
                        + "       ?pipeline cruise:pipelineName ?failedPipelineName . "
                        + "       ?pipeline cruise:pipelineCounter ?failedPipelineCounter . "
                        + "       ?pipeline cruise:pipelineLabel ?failedPipelineLabel . "
                        + "       ?pipeline cruise:hasStage ?stage . "
                        + "       ?stage cruise:stageName ?failedStageName . "
                        + "       ?stage cruise:stageCounter ?failedStageCounter . "
                        + "       ?stage cruise:hasJob ?job . "
                        + "       ?job cruise:jobName ?jobName .\n"
                        + "       OPTIONAL {\n"
                        + "              ?job xunit:hasTestCase ?testCase .\n"
                        + "              ?testCase xunit:testCaseName ?testCaseName .\n"
                        + "              ?testCase xunit:testSuiteName ?testSuiteName .\n"
                        + "              ?testCase xunit:hasFailure ?failure .\n"
                        + "              ?failure xunit:isError ?isError.\n"
                        + "       }\n"
                        + "} ORDER BY ?jobName ?testSuiteName ?testCaseName";

        return stagesQuery.select(selectFailingTests, failedStageIds, new RdfResultMapper<TestCaseModel>() {

            public TestCaseModel map(BoundVariables aRow) {
                String failedPipelineName = aRow.getString("failedPipelineName");
                int failedPipelineCounter = aRow.getInt("failedPipelineCounter");
                String failedPipelineLabel = aRow.getString("failedPipelineLabel");
                String failedStageName = aRow.getString("failedStageName");
                String failedStageCounter = aRow.getString("failedStageCounter");
                String jobName = aRow.getString("jobName");
                return new TestCaseModel(new JobIdentifier(failedPipelineName, failedPipelineCounter, failedPipelineLabel, failedStageName, failedStageCounter, jobName),
                        aRow.getString("testSuiteName"), aRow.getString("testCaseName"), aRow.getBoolean("isError"));
            }
        });
    }

    private void populateFailingTests(StageTestRuns stageTestRuns, List<TestCaseModel> testCaseModels) {
        for (TestCaseModel testCaseModel : testCaseModels) {
            JobIdentifier jobIdentifier = testCaseModel.getJobIdentifier();
            if (!StringUtils.isEmpty(testCaseModel.getTestName())) {
                stageTestRuns.add(jobIdentifier.getPipelineCounter(), jobIdentifier.getPipelineLabel(), testCaseModel.getTestSuiteName(), testCaseModel.getTestName(), TestStatus.fromURLType(
                        testCaseModel.isError()), jobIdentifier);
            } else {
                stageTestRuns.add(jobIdentifier.getPipelineCounter(), jobIdentifier.getPipelineLabel());
            }
        }
    }

    private static String s(String str) {
        return String.format("\"%s\"^^xsd:string", str);
    }

    private static String i(int integer) {
        return i(String.valueOf(integer));
    }

    private static String i(String integer) {
        return String.format("\"%s\"^^xsd:integer", integer);
    }
}