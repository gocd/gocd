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

describe("pipelines_selector_ajax_refresher", function () {
    afterEach(function () {
        AjaxRefreshers.clear();
    });

    beforeEach(function () {
        AjaxRefreshers.clear();
        setFixtures("<div class='under_test'>\n" +
            "    <button class='select submit button' id='show_pipelines_selector' type='button' value='Edit View'>\n" +
            "        <span>EDIT VIEW<img src='/images/g9/button_select_icon.png?N/A'/></span>\n" +
            "    </button>\n" +
            "    <div id='pipelines_selector' class='enhanced_dropdown hidden'>\n" +
            "        <form action='/pipelines/select_pipelines' method='post'\n" +
            "              onsubmit='AjaxRefreshers.disableAjax();; new Ajax.Request(&apos;/pipelines/select_pipelines&apos;, {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage(&apos;/auth/login&apos;);}, onComplete:function(request){AjaxRefreshers.enableAjax();PipelineFilter.close();}, parameters:Form.serialize(this)}); return false;'>\n" +
            "            <div class='select_all_none_panel'>\n" +
            "                <div id='select_text'>Select:</div>\n" +
            "                <a  class='link_as_button' href='javascript:void(0);' id='select_all_pipelines'>All</a>\n" +
            "                <a  class='link_as_button' href='javascript:void(0);' id='select_no_pipelines'>None</a>\n" +
            "            </div>\n" +
            "            <div id='pipelines_selector_pipelines' class='scrollable_panel'>\n" +
            "                <div id='selector_group_group-1' class='selector_group'>\n" +
            "                    <input type='checkbox' id='select_group_group-1' name='selector[group][]' value='group-1' checked='checked'/>\n" +
            "                    <label for='select_group_group-1' class='label inline'>group-1</label>\n" +
            "\n" +
            "                    <div id='selector_pipeline_pipeline-1' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-1' name='selector[pipeline][]' value='pipeline-1' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-1' class='label inline'>pipeline-1</label>\n" +
            "                    </div>\n" +
            "                    <div id='selector_pipeline_pipeline-2' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-2' name='selector[pipeline][]' value='pipeline-2' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-2' class='label inline'>pipeline-2</label>\n" +
            "                    </div>\n" +
            "                    <div id='selector_pipeline_pipeline-3' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-3' name='selector[pipeline][]' value='pipeline-3' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-3' class='label inline'>pipeline-3</label>\n" +
            "                    </div>\n" +
            "                    <div id='selector_pipeline_pipeline-4' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-4' name='selector[pipeline][]' value='pipeline-4' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-4' class='label inline'>pipeline-4</label>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div id='selector_group_group-2' class='selector_group'>\n" +
            "                    <input type='checkbox' id='select_group_group-2' name='selector[group][]' value='group-2' checked='checked'/>\n" +
            "                    <label for='select_group_group-2' class='label inline'>group-2</label>\n" +
            "\n" +
            "                    <div id='selector_pipeline_pipeline-2-1' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-2-1' name='selector[pipeline][]' value='pipeline-2-1' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-2-1' class='label inline'>pipeline-2-1</label>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div id='selector_group_group-x' class='selector_group'>\n" +
            "                    <input type='checkbox' id='select_group_group-x' name='selector[group][]' value='group-x' checked='checked'/>\n" +
            "                    <label for='select_group_group-x' class='label inline'>group-x</label>\n" +
            "\n" +
            "                    <div id='selector_pipeline_pipeline-x1' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-x1' name='selector[pipeline][]' value='pipeline-x1' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-x1' class='label inline'>pipeline-x1</label>\n" +
            "                    </div>\n" +
            "                    <div id='selector_pipeline_pipeline-x2' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-x2' name='selector[pipeline][]' value='pipeline-x2' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-x2' class='label inline'>pipeline-x2</label>\n" +
            "                    </div>\n" +
            "                    <div id='selector_pipeline_pipeline-x3' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-x3' name='selector[pipeline][]' value='pipeline-x3' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-x3' class='label inline'>pipeline-x3</label>\n" +
            "                    </div>\n" +
            "                    <div id='selector_pipeline_pipeline-x4' class='selector_pipeline'>\n" +
            "                        <input type='checkbox' id='select_pipeline_pipeline-x4' name='selector[pipeline][]' value='pipeline-x4' checked='checked'/>\n" +
            "                        <label for='select_pipeline_pipeline-x4' class='label inline'>pipeline-x4</label>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div class='add_panel'>\n" +
            "                <button class='primary submit' id='apply_pipelines_selector' type='submit' value='Apply'>\n" +
            "                    <span>APPLY</span>\n" +
            "                </button>\n" +
            "            </div>\n" +
            "        </form>\n" +
            "    </div>\n" +
            "</div>");
        PipelineFilter.initialize();
        fire_event($('show_pipelines_selector'), 'click');
    });

    it("test_should_disable_ajax_while_micro_content_is_visible", function () {
        fire_event($('show_pipelines_selector'), 'click');//it was already open
        var stopped = false;
        AjaxRefreshers.addRefresher({stopRefresh: function () {
            stopped = true;
        }, restartRefresh: function () {
            stopped = false;
        }});
        fire_event($('show_pipelines_selector'), 'click');
        assertTrue("ajax should be STOPPED when selector IS shown", stopped);
        fire_event($('apply_pipelines_selector'), 'click');
        assertTrue("pipeline selector should be DISABLED", $('show_pipelines_selector').disabled);
        PipelineFilter.close();
        assertFalse("ajax should be RESUMED when selector IS NOT shown", stopped);
    });

});

