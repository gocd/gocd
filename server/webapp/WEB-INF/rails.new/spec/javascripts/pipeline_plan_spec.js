/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("pipeline_plan", function () {
    beforeEach(function () {
        contextPath = "/go";
        pipelinePage = new PipelinePage();
        pipelineActions = new PipelineActions();

        window.last_transfered_json = pipelinesJson;
        window.pipelineObserver = new PipelineObserver();
        pipelinePage.initializeCollapsedStageArrayWhenNeeded([pipeline1stageJson.uniqueStageId]);

        setFixtures("<textarea id=\"new-pipeline-list-template\" style=\"display: none;\" rows=\"0\" cols=\"0\">\n" +
            "{eval}\n" +
            "    pipelinePage.fixIEZIndexBugs(300);\n" +
            "{/eval}\n" +
            "{if data.pipelines && data.pipelines.length > 0}\n" +
            "{for pipeline in data.pipelines}\n" +
            "{if !pipeline.hide_in_ui}\n" +
            "<div class=\"pipeline-container rounded-corner-for-pipeline\" style=\"${%pipelinePage.fixIEZIndexBugs()%}\">\n" +
            "    <b class=\"c1\"></b><b class=\"c2\"></b><b class=\"c3\"></b><b class=\"c4\"></b>\n" +
            "    {if pipelinePage.isPipelinePaused(pipeline)}\n" +
            "    <h1 class=\"paused-build\">\n" +
            "    {else}\n" +
            "    <h1>\n" +
            "    {/if}\n" +
            "        <span class=\"pipeline-buttons\">\n" +
            "        {if pipelinePage.isPipelineScheduleButtonEnabled(pipeline)}\n" +
            "            <a href=\"javascript:void(0)\" id=\"reschedule-${%pipeline.name%}\" onclick=\"pipelineActions.schedulePipeline('${%pipeline.name%}', this);\" class=\"icon-link {if pipelineActions.shouldShowPipelineScheduleButtonAsSpinner(pipeline)} submiting-link{else} schedule-build-link-enabled{/if}\" title=\"Schedule pipeline\"></a>\n" +
            "        {else}\n" +
            "            <a href=\"javascript:void(0)\" id=\"reschedule-${%pipeline.name%}\" class=\"icon-link {if pipelineActions.shouldShowPipelineScheduleButtonAsSpinner(pipeline)} submiting-link{else} schedule-build-link-disabled{/if}\" title=\"Schedule pipeline\"></a>\n" +
            "        {/if}\n" +
            "        {if pipelinePage.isPipelinePaused(pipeline)}\n" +
            "            <a href=\"javascript:void(0)\" onclick=\"pipelineActions.unpausePipeline('${%pipeline.name%}', this);\" class=\"icon-link unpause-build-link\" title=\"Resume scheduling\"></a>\n" +
            "        {else}\n" +
            "            <a href=\"javascript:void(0)\" onclick=\"pipelineActions.pausePipeline('${%pipeline.name%}', this);\" class=\"icon-link pause-build-link\" title=\"Stop scheduling\"></a>\n" +
            "        {/if}\n" +
            "        </span>\n" +
            "        <a href=\"$req.getContextPath()/tab/pipeline/history/${% pipeline.name %}\" title=\"View pipeline history\" id=\"${% pipeline.name %}-history\">\n" +
            "            ${% pipeline.name %}<span class=\"pipeline-nav\">[pipeline activity]</span></a>\n" +
            "        <span class=\"paused-status\">${%pipelinePage.pauseStatusText(pipeline)%}</span>\n" +
            "        <div class=\"clear\"></div>\n" +
            "    </h1>\n" +
            "    <div class=\"pipeline-stages\">\n" +
            "    {if pipeline.stages && pipeline.stages.length > 0}\n" +
            "    {for stage in pipeline.stages}\n" +
            "        <div class=\"stage-container pipeline-${% stage.current_status %} {if pipelinePage.isStageCollapsed(stage.uniqueStageId)} closed{/if}\" id=\"pipeline-${% stage.uniqueStageId %}\" style=\"${%pipelinePage.fixIEZIndexBugs()%}\">\n" +
            "            <h2>\n" +
            "                <table>\n" +
            "                    <thead>\n" +
            "                        <tr>\n" +
            "                            <th  class=\"stage-name\" onclick=\"pipelinePage.toggleStagePanel('${% stage.uniqueStageId %}')\" title=\"collapse or expand this pipeline\">\n" +
            "                                <button id=\"${% stage.uniqueStageId %}-collapse-link\" class=\"collapse-or-expand-button {if pipelinePage.isStageCollapsed(stage.uniqueStageId)} collapsed{else} expanded{/if}\">Collapse/Expand</button>\n" +
            "                                ${% stage.stageName %}\n" +
            "                                <a href=\"$req.getContextPath()/tab/pipeline/${% pipeline.name %}/${% stage.stageName %}\" title=\"View stage activity\" class=\"pipeline-nav\">[stage activity]</a>\n" +
            "                            </th>\n" +
            "                            <th>\n" +
            "                                <span class=\"stage-buttons\">\n" +
            "                                  {if pipelinePage.isCancelButtonEnabled(stage.current_status)}\n" +
            "                                      <a href=\"javascript:void(0)\" onclick=\"pipelineActions.cancelPipeline(${%stage.id%}, this);\" class=\"icon-link cancel-build-link\" title=\"Cancel this stage\"></a>\n" +
            "                                  {else}\n" +
            "                                      <a href=\"javascript:void(0)\" class=\"icon-link cancel-build-link-disabled\" title=\"Cancel this stage\"></a>\n" +
            "                                  {/if}\n" +
            "                                </span>\n" +
            "\n" +
            "                                <span class=\"current-revision-status\">\n" +
            "                                {if stage.id > 0}\n" +
            "                                    <a href=\"$req.getContextPath()/pipelines/${% stage.id %}{if 'failed' == stage.current_status}#tab-failures{/if}{if 'passed' == stage.current_status}#tab-artifacts{/if}\" class=\"building-detail-link\" title=\"View stage details\">\n" +
            "                                {/if}\n" +
            "                                {if stage.id > 0}Label ${% stage.current_label %}{/if}\n" +
            "                                    ${% stage.current_status %}\n" +
            "                                {if stage.stage_completed_date}\n" +
            "                                    ${% stage.stage_completed_date %}\n" +
            "                                {/if}\n" +
            "                                {if stage.id > 0}\n" +
            "                                    <span class=\"pipeline-nav\">[stage details]</span>\n" +
            "                                    </a>\n" +
            "                                {/if}\n" +
            "                                </span>\n" +
            "\n" +
            "                                <span class=\"divider\">|</span>\n" +
            "\n" +
            "                                <span id=\"stage-${% stage.id %}-buildCause\" class=\"popup-${% pipelinePage.buildCauseActor.getBuildCauseClass(stage.id) %}\">\n" +
            "                                    {if stage.buildCause ==  'No modifications'}\n" +
            "                                        ${% stage.buildCause %}\n" +
            "                                    {else}\n" +
            "                                        <a href=\"javascript:void(0)\" onclick=\"pipelinePage.buildCauseActor.hideOrShowBuildCause('${%stage.id%}')\" title=\"See what caused this build\">${% stage.buildCause %}</a>\n" +
            "                                        <div id=\"stage-${%stage.id%}-buildCauseSummary\" class=\"popup\">\n" +
            "                                            <div class=\"popup-arrow\"></div>\n" +
            "                                            <button class=\"close-popup\" onclick=\"pipelinePage.buildCauseActor.hideOrShowBuildCause('${%stage.id%}')\"></button>\n" +
            "                                            <div class=\"build-cause-summary-container\">\n" +
            "                                                {if stage.buildCauseSummaries.length > 0}\n" +
            "                                                <table>\n" +
            "                                                    <tr>\n" +
            "                                                        <th>Modifier</th>\n" +
            "                                                        <th>Comments</th>\n" +
            "                                                        <th class=\"last\">Revision</th>\n" +
            "                                                    </tr>\n" +
            "                                                    {for buildCauseSummary in stage.buildCauseSummaries}\n" +
            "                                                    <tr>\n" +
            "                                                        <td>${% buildCauseSummary.username %}</td>\n" +
            "                                                        <td>${% buildCauseSummary.comment %}</td>\n" +
            "                                                        <td class=\"last\">${% buildCauseSummary.revision %}</td>\n" +
            "                                                    </tr>\n" +
            "                                                    {/for}\n" +
            "                                                </table>\n" +
            "                                                {else}\n" +
            "                                                    No modifications\n" +
            "                                                {/if}\n" +
            "                                            </div>\n" +
            "                                        </div>\n" +
            "                                    {/if}\n" +
            "                                </span>\n" +
            "\n" +
            "                                <span class=\"divider\">|</span>\n" +
            "\n" +
            "                                <span class=\"last-success-revision\">\n" +
            "                                    {if !stage.last_successful_label}\n" +
            "                                        no successful stage\n" +
            "                                    {else}\n" +
            "                                        <a href=\"$req.getContextPath()/pipelines/${%stage.last_successful_stage_id%}#tab-artifacts\" title=\"View stage details for ${%stage.last_successful_label%}\">\n" +
            "                                            last successful: ${%stage.last_successful_label%}</a>\n" +
            "                                    {/if}\n" +
            "                                </span>\n" +
            "                                <div style=\"clear:both\"></div>\n" +
            "                            </th>\n" +
            "                        </tr>\n" +
            "                    </thead>\n" +
            "                </table>\n" +
            "            </h2>\n" +
            "\n" +
            "            <div class=\"stage-build-plan-container\">\n" +
            "                {for build in stage.builds}\n" +
            "                <div class=\"stage-build-plan-status ${%build.current_status%}\">\n" +
            "                    <div class=\"stage-build-plan-content\">\n" +
            "                        <h3>\n" +
            "                            {if isEstimatable(build.current_status) }\n" +
            "                              <div class=\"progress-info\">\n" +
            "                                {if build.last_build_duration && build.last_build_duration != '' && (parseInt(build.current_build_duration) <= parseInt(build.last_build_duration))}\n" +
            "                                    <div class=\"progress-bar\">\n" +
            "                                        <div class=\"progress\" style=\"width: {if parseInt(build.current_build_duration) > parseInt(build.last_build_duration)}100{else}${ 100 * parseInt(build.current_build_duration) / parseInt(build.last_build_duration) }{/if}%;\">\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                {/if}\n" +
            "                                {if build.last_build_duration && build.last_build_duration != ''}\n" +
            "                                    {if parseInt(build.current_build_duration) > parseInt(build.last_build_duration)}\n" +
            "                                        Longer by: ${%moment.duration(parseInt(build.current_build_duration) - parseInt(build.last_build_duration), 's').humanizeForGoCD()%}\n" +
            "                                    {else}\n" +
            "                                        Elapsed: ${%moment.duration(parseInt(build.current_build_duration), 's').humanizeForGoCD() %}, ETA: ${%moment.duration(parseInt(build.last_build_duration) - parseInt(build.current_build_duration), 's').humanizeForGoCD() %}\n" +
            "                                    {/if}\n" +
            "                                {else}\n" +
            "                                    Elapsed: ${%moment.duration(parseInt(build.current_build_duration), 's').humanizeForGoCD() %}\n" +
            "                                {/if}\n" +
            "                              </div>\n" +
            "                            {/if}\n" +
            "\n" +
            "                            {if build.id > 0}\n" +
            "                                <a id=\"${% build.name %}_link\"\n" +
            "                                   class=\"buildplan-name\"\n" +
            "                                   href=\"$req.getContextPath()/tab/build/detail/${%pipeline.name%}/${%stage.stageName%}/${%build.name%}{if 'failed' == build.current_status}#tab-failures{/if}{if 'passed' == build.current_status}#tab-artifacts{/if}\"\n" +
            "                                   title=\"Job Detail of ${% build.name %}\">${% build.name %}</a>\n" +
            "                                <a class=\"buildplan-name\" href=\"$req.getContextPath()/tab/build/detail/${%pipeline.name%}/${%stage.stageName%}/${%build.name%}{if 'failed' == build.current_status}#tab-failures{/if}{if 'passed' == build.current_status}#tab-artifacts{/if}\" title=\"Job Detail of ${% build.name %}\">\n" +
            "                                    <span class=\"status-message\">\n" +
            "                                        {if pipelinePage.ifShowAgentInBuildStatusMessage(build.current_status)}\n" +
            "                                            ${%build.current_status %}{if build.current_build_event} (${% build.current_build_event %}){/if} on ${% build.agent %}\n" +
            "                                        {else}\n" +
            "                                            ${% build.current_status %}{if build.current_build_event} (${% build.current_build_event %}){/if}\n" +
            "                                        {/if}\n" +
            "                                    </span>\n" +
            "                                    <span class=\"pipeline-nav\">[view details]</span>\n" +
            "                                </a>\n" +
            "                            {else}\n" +
            "                                <span class=\"buildplan-name\">${% build.name %}</span>\n" +
            "                            {/if}\n" +
            "                            <div class=\"clear\"></div>\n" +
            "                        </h3>\n" +
            "                    </div>\n" +
            "                    <div class=\"clear\"></div>\n" +
            "                </div>\n" +
            "                {/for}\n" +
            "            </div>\n" +
            "        </div><!--stage container end -->\n" +
            "        {/for}\n" +
            "        {/if}\n" +
            "    </div>\n" +
            "</div><!--pipeline container end -->\n" +
            "{/if}\n" +
            "{/for}\n" +
            "{/if}\n" +
            "</textarea>\n" +
            "\n" +
            "<div id=\"build-status-actions\" class=\"bd-container\">\n" +
            "    <div class=\"ab-bg\">\n" +
            "        <span class=\"ab-corner lvl1\"></span> <span class=\"ab-corner lvl2\"></span> <span\n" +
            "            class=\"ab-corner lvl3\"></span> <span class=\"ab-corner lvl4\"></span></div>\n" +
            "    <div class=\"bd-c-wrapper\">\n" +
            "        <div class=\"actions-bar\">\n" +
            "            <a id=\"expand-all\" href=\"javascript:void(0)\" title=\"Expand all pipelines panel\"\n" +
            "                                    onclick=\"pipelinePage.expandAllStagePanels()\"> Expand All </a>\n" +
            "            <a id=\"collapse-all\" href=\"javascript:void(0)\" title=\"Collapse all pipelines panel\"\n" +
            "                onclick=\"pipelinePage.collapseAllStagePanels()\"> Collapse All </a></div>\n" +
            "        <div class=\"clear\"></div>\n" +
            "    </div>\n" +
            "    <div class=\"ab-bg\"><span class=\"ab-corner lvl4\"></span> <span class=\"ab-corner lvl3\"></span> <span\n" +
            "            class=\"ab-corner lvl2\"></span> <span class=\"ab-corner lvl1\"></span></div>\n" +
            "</div>\n" +
            "\n" +
            "\n" +
            "<!-- pipeline start -->\n" +
            "<div id=\"build-pipelines\">\n" +
            "<!-- pipeline end -->\n" +
            "</div>\n"
);

        render_the_page_again_immediately();
//        new PeriodicalExecuter(function (pe) {
//            render_the_page_again_immediately();
//        }, 1);

    });

    afterEach(function(){
        contextPath = undefined;
        pipelinePage = undefined;
        pipelineActions = undefined;
    });

    var statisticsObserver;
//    var pipelinePage;
//    var pipelineActions;
    var build1 = { "agent": "Not yet assigned", "current_status": "failed", "result": "Failed", "name": "functional", "build_completed_date": "Mon Apr 21 16:36:16 CST 2008", "id": "8", "is_completed": "true" };
    var build2 = { "agent": "Not yet assigned", "current_status": "failed", "result": "Failed", "name": "functional", "build_completed_date": "Mon Apr 21 16:36:16 CST 2008", "id": "9", "is_completed": "true" };

    var pipeline1stageJson = {
        "buildCause": "",
        "buildCauseSummaries": [],
        "builds": [build1],
        "uniqueStageId": "studios-mingle-100",
        "id": "100",
        "stageName": "mingle",
        "stage_completed_date": "about 1 hour ago",
        "current_status": "failed"
    }
    var pipeline2stageJson = {
        "buildCause": "",
        "buildCauseSummaries": [],
        "builds": [build2],
        "uniqueStageId": "connectfour-StageName-200",
        "id": "200",
        "stageName": "StageName",
        "stage_completed_date": "about 1 hour ago",
        "current_status": "failed"
    }

    var pipeline1Json = { name: 'studios', id: '3', stages: [pipeline1stageJson]}
    var pipeline2Json = { name: 'connectfour', id: '4', stages: [pipeline2stageJson]}
    var pipelinesJson = { "pipelines": [pipeline1Json, pipeline2Json] }

    var uniqueId = pipeline1stageJson.uniqueStageId;
    var id = "pipeline-" + uniqueId;
    var pipeline1StageCssSelector = "#" + id + " .stage-build-plan-container";

    function render_the_page_again_immediately() {
        $('build-pipelines').innerHTML = $('new-pipeline-list-template').value.process({data: window.last_transfered_json });
    }

    it("test_should_toggle_content_of_pipeline_when_click_collapse", function () {
        //assume it's already collapsed
        assertTrue($$(pipeline1StageCssSelector).first().match('.closed .stage-build-plan-container'));

        //now expand it
        pipelinePage.toggleStagePanel(uniqueId);
        assertFalse($$(pipeline1StageCssSelector).first().match('.closed .stage-build-plan-container'));

        //now collapse it
        pipelinePage.toggleStagePanel(uniqueId);
        assertTrue($$(pipeline1StageCssSelector).first().match('.closed .stage-build-plan-container'));
    });

    it("test_isPipelineScheduleButtonEnabled_returns_false_when_forcedBuild", function () {
        var pipelineName = pipeline1Json.name;
        pipeline1Json.forcedBuild = 'true';
        assertFalse(pipelinePage.isPipelineScheduleButtonEnabled(pipeline1Json));
    });

    it("test_should_keep_current_status_of_collapse_after_render", function () {
        assertTrue($$(pipeline1StageCssSelector).first().match('.closed .stage-build-plan-container'));
        render_the_page_again_immediately();
        assertTrue($$(pipeline1StageCssSelector).first().match('.closed .stage-build-plan-container'));
        pipelinePage.toggleStagePanel(uniqueId);
        assertFalse($$(pipeline1StageCssSelector).first().match('.closed .stage-build-plan-container'));
        render_the_page_again_immediately();
        assertFalse($$(pipeline1StageCssSelector).first().match('.closed .stage-build-plan-container'));
    });


    it("test_should_collapse_all_pipelines_when_click_collapse_all_button", function () {
        $('collapse-all').onclick();
        assertTrue($$('.stage-build-plan-container').all(function (container) {
            return $(container).match('.closed .stage-build-plan-container');
        }));
        render_the_page_again_immediately();
        assertTrue($$('.stage-build-plan-container').all(function (container) {
            return $(container).match('.closed .stage-build-plan-container');
        }));
    });


    it("test_should_expand_all_pipelines_when_click_expand_all_button", function () {
        $('collapse-all').onclick();
        assertTrue($$('.stage-build-plan-container').all(function (container) {
            return $(container).match('.closed .stage-build-plan-container');
        }));
        $('expand-all').onclick();
        assertFalse($$('.stage-build-plan-container').any(function (container) {
            return $(container).match('.closed .stage-build-plan-container');
        }));
        render_the_page_again_immediately();
        assertFalse($$('.stage-build-plan-container').any(function (container) {
            return $(container).match('.closed .stage-build-plan-container');
        }));
    });

});

