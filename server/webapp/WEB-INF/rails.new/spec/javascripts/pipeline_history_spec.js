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
 *************************GO-LICENSE-END**********************************/

describe("pipeline_history", function () {
    var page;

    beforeEach(function () {
        page = new PipelineHistoryPage();
        pipelineActions = new PipelineActions();
        paginator = new Paginator();
        contextPath = '';
        dashboard_periodical_executor = new DashboardPeriodicalExecutor('pipelineHistory.json?pipelineName=up42');
    });

    afterEach(function () {
        pipelineActions = undefined;
        paginator = undefined;
        contextPath = undefined;
        dashboard_periodical_executor = undefined;
    });

    it("testCompleteAutomatically", function () {
        assertEquals("gate-completed-auto", page._gateClass('cruise'));
    });


    it("testShouldShowGate", function () {
        var allConfigs = [getConfig("foo"), getConfig("bar")];
        page.findLastStageNameFromConfiguration(allConfigs);

        var allstages = [getStage('dev'), getStage('ft')];
        assertTrue("should show the dev gate", page.shouldShowGate(getStage('dev'), allstages, allConfigs));
        assertFalse("should not show the ft gate", page.shouldShowGate(getStage('ft'), allstages, allConfigs));

        allstages = [getStage('foo')]
        assertTrue("should show the gate when more stages to build", page.shouldShowGate(getStage('foo'), allstages, allConfigs));

        allstages = [getStage("bar"), getStage('foo')]
        assertTrue("should show the gate when already completed", page.shouldShowGate(getStage('bar'), allstages, allConfigs));
        assertTrue("should show the gate when more stages to build (even when it has already built it)",
            page.shouldShowGate(getStage('foo'), allstages, allConfigs));
    });


    it("testCompletedManually", function () {
        assertEquals("gate-completed-manual", page._gateClass("Chris & Gao Li"));
    });

// pipelineHistoryPage.noNextStagesRunning(data.history, stage.stageName)


    it("testNoNextStagesRunning", function () {
        assertEquals(false, page.noNextStagesRunning(getPipelines(), "ft"));
    });


    it("testNoNextStagesRunningIsTrueWhenNothingBuilding", function () {
        var pipelinesWithoutAnyBuildingStage = getPipelines()
        pipelinesWithoutAnyBuildingStage[1].stages.pop()
        assertEquals(true, page.noNextStagesRunning(pipelinesWithoutAnyBuildingStage, "ft"));
    });


    it("test_isPipelineScheduleButtonEnabled_returns_false_when_forcedBuild", function () {

        var pipelinesWithoutAnyBuildingStage = getPipelines()
        var pipeline1Json = pipelinesWithoutAnyBuildingStage[0]
        var pipelineName = pipeline1Json.name;
        pipeline1Json.paused = 'false';
        pipeline1Json.canForce = 'true';

        assertTrue(page.isPipelineScheduleButtonEnabled(pipeline1Json));

        pipeline1Json.forcedBuild = 'true';
        assertFalse(page.isPipelineScheduleButtonEnabled(pipeline1Json));
    });

    it("testShouldSwitchToPage", function () {
      var pipelinesWithoutAnyBuildingStage = getPipelines()
      var pipeline1Json = pipelinesWithoutAnyBuildingStage[0]
      assertEquals(dashboard_periodical_executor.url, "/pipelineHistory.json?pipelineName=up42");
      page.switchToPage(pipeline1Json.pipelineId, "1");
      assertEquals(dashboard_periodical_executor.url, "//pipelineHistory.json?pipelineName=11&start=0");
    });

    function getPipelines() {
        return [
            { "pipelineId": "11",
                "stages": [
                    { "stageStatus": "Passed", "stageId": "300", "stageName": "dev" },
                    { "stageStatus": "Passed", "stageId": "301", "stageName": "ft" }

                ]
            },
            { "pipelineId": "10",
                "stages": [
                    { "stageStatus": "Passed", "stageId": "200", "stageName": "dev" },
                    { "stageStatus": "Passed", "stageId": "201", "stageName": "ft" },
                    { "stageStatus": "Building", "stageId": "202", "stageName": "qa" }
                ]
            }
        ];

    }

    function getStage(stageName) {
        return {stageStatus: "Passed", stageId: "12", approvedBy: 'user', stageName: stageName};
    }

    function getConfig(stageName) {
        return { needsApproval: "true", stageName: stageName };
    }
});
