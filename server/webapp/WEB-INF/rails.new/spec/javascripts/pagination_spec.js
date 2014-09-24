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

describe("pagination", function () {
    setupForPaginationSpec = function(){
        paginator = new Paginator();
    }

    beforeEach(function () {
        setupForPaginationSpec();
        setFixtures("<textarea id=\"pagination-template\" style=\"display: none;\" rows=\"0\" cols=\"0\">\n" +
            "{if data.pipelines && data.pipelines.length > 0}\n" +
            "{for pipeline in data.pipelines}\n" +
            "<div class=\"pipeline-container rounded-corner-grey-border ${pipeline.current_status} {if pipelinePage.isStageCollapsed(pipeline.name)} closed{/if}\" id=\"pipeline-${pipeline.name}\">\n" +
            "    <b class=\"c1\"></b><b class=\"c2\"></b><b class=\"c3\"></b><b class=\"c4\"></b>\n" +
            "    <h2>\n" +
            "        <a href=\"javascript:void(0)\" id=\"${pipeline.name}-collapse-link\" class=\"collapse-or-expand-button {if pipelinePage.isStageCollapsed(pipeline.name)} expand{else} collapse{/if}\" onclick=\"pipelinePage.toggleStagePanel('${pipeline.name}');\">${pipeline.name}</a>\n" +
            "        <span class=\"current-revision\">\n" +
            "            ${pipeline.current_label}: ${pipeline.current_status} ${pipeline.stage_completed_date}\n" +
            "        </span>\n" +
            "        <span class=\"current-revision-status\">\n" +
            "            {if !pipeline.last_successful_label}\n" +
            "                no successful stage\n" +
            "            {else}\n" +
            "                last successful: ${pipeline.last_successful_label}\n" +
            "            {/if}\n" +
            "            <!--2 of 5 remaining, 3 errors (artifacts)    Revision 5755    : 2 of 5 failed, 27 errors (artifacts)-->\n" +
            "        </span>\n" +
            "    </h2>\n" +
            "    <div class=\"stage-build-plan-container\n" +
            "\">\n" +
            "        {for build in pipeline.builds}\n" +
            "        <div class=\"stage-build-plan-status ${build.current_status}\">\n" +
            "            <div class=\"stage-build-plan-content\">\n" +
            "                <h3>\n" +
            "                    {if build.current_status != 'scheduled' && build.current_status != 'assigned'}\n" +
            "                        <a id=\"${build.name}_link\" class=\"buildplan-name\" href=\"$req.getContextPath()/tab/build/detail/${pipeline.name}/${build.name}\" title=\"Build Detail of ${build.name}\">${build.name}</a>\n" +
            "                    {else}\n" +
            "                        <span class=\"buildplan-name\">${build.name}</span>\n" +
            "                    {/if}\n" +
            "                    <span class=\"status-message\">\n" +
            "                        {if $A(['building', 'preparing', 'completing']).include(build.current_status)}\n" +
            "                            ${build.current_status} on ${build.agent}\n" +
            "                        {else}\n" +
            "                            ${build.current_status}\n" +
            "                        {/if}\n" +
            "                    </span>\n" +
            "                </h3>\n" +
            "            </div>\n" +
            "            <div class=\"clear\"></div>\n" +
            "        </div>\n" +
            "        {/for}\n" +
            "    </div>\n" +
            "</div>\n" +
            "{/for}\n" +
            "{/if}\n" +
            "</textarea>\n" +
            "\n" +
            "<p id=\"page-links\" class=\"pages\">\n" +
            "</p>\n" +
            "\n" +
            "<script type=\"text/javascript\">\n" +
            "setupForPaginationSpec();\n" +
            "</script>");

        contextPath = "/go";
        pipelinePage = new PipelinePage();
    });

    afterEach(function(){
        contextPath = undefined;
    });
    var paginator;
    var pipelinePage;

    function render_the_page_again_immediately() {
        $('build-pipelines').innerHTML = $('new-pipeline-list-template').value.process({data: window.last_transfered_json });
    }

    it("test_should_return_default_parameters_when_first_load", function () {
        assertEquals(10, paginator.perPage);
        assertEquals(0, paginator.start);
    });

    it("test_should_store_pagination_information_after_seted", function () { //Is this a useful test?
        paginator.setPageParameters({pipelineName: 'mingle'});
        paginator.setPerPage(15);
        paginator.setStart(15);

        assertEquals(15, paginator.perPage);
        assertEquals(15, paginator.start);
        assertEquals('mingle', paginator.parameters.pipelineName);
    });

    it("test_should_restore_default_parameters_after_call_reset", function () {
        paginator.setPageParameters({pipelineName: 'mingle'});
        paginator.setPerPage(15);
        paginator.setStart(15);
        paginator.reset();

        assertEquals(10, paginator.perPage);
        assertEquals(0, paginator.start);
        assertEquals(undefined, paginator.parameters.pipelineName);
    });

    it("test_should_get_parameters_correctly_from_json", function () {
        var json = {
            history: [],
            count: 100,
            start: 10,
            perPage: 10
        }

        paginator.setParametersFromJson(json);

        assertEquals(10, paginator.perPage);
        assertEquals(10, paginator.start);
        assertEquals(100, paginator.count);
        assertEquals(10, paginator.totalPages);
    });

    it("test_should_get_total_pages_number_correctly_after_seted_count_and_perPage", function () {
        var json = {
            count: 99,
            start: 20,
            perPage: 10
        }

        paginator.setParametersFromJson(json);
        assertEquals(10, paginator.totalPages);

        paginator.setCount(101);
        assertEquals(11, paginator.totalPages);

        paginator.setCount(1);
        assertEquals(1, paginator.totalPages);

        paginator.setCount(9);
        assertEquals(1, paginator.totalPages);

        paginator.setCount(10);
        assertEquals(1, paginator.totalPages);

        paginator.setCount(11);
        assertEquals(2, paginator.totalPages);

        paginator.setCount(0);
        assertEquals(1, paginator.totalPages);
    });

    it("test_should_get_current_page_number_correctly_after_seted_start_offset", function () {
        var json = {
            count: 10000,
            start: 20,
            perPage: 10
        }

        paginator.setParametersFromJson(json);
        assertEquals(3, paginator.currentPage);

        paginator.setStart(0);
        assertEquals(1, paginator.currentPage);

        paginator.setStart(20);
        assertEquals(3, paginator.currentPage);

        paginator.setStart(19);
        assertEquals(2, paginator.currentPage);

        paginator.setStart(21);
        assertEquals(3, paginator.currentPage);
    });

    it("test_should_convert_string_paramerter_to_number", function () {
        var json = {
            count: "1000",
            start: "20",
            perPage: "10"
        }

        paginator.setParametersFromJson(json);
        assertEquals(3, paginator.currentPage);
        assertEquals(10, paginator.perPage);
        assertEquals(20, paginator.start);
        assertEquals(1000, paginator.count);
    });
});