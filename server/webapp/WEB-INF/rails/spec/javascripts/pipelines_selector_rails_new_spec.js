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

describe("pipelines_selector_rails_new", function () {
    afterEach(function () {
        AjaxRefreshers.clear();
    });

    beforeEach(function () {
        AjaxRefreshers.clear();
        setFixtures("<div class='under_test'>\n" +
            "    <input title='Search for Pipeline' placeholder='Search for Pipeline' id='pipeline-search' type='text'/>\n" +
            "    <button class='header_submit submit button' id='show_pipelines_selector' type='button' value='Personalize'>\n" +
            "        <span>PERSONALIZE</span>\n" +
            "    </button>\n" +
            "    <div id='pipelines_selector' class='enhanced_dropdown hidden'>\n" +
            "        <form accept-charset='UTF-8' action='/pipelines/select_pipelines' method='post'\n" +
            "              onsubmit=\"PipelineOperations.onPipelineSelector(this, '/pipelines/select_pipelines'); return false;; \">\n" +
            "            <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "            <div class='select_all_none_panel'>\n" +
            "                <div id='select_text'>Select:</div>\n" +
            "                <a  class='link_as_button' href='javascript:void(0);' id='select_all_pipelines'>All</a>\n" +
            "                <a  class='link_as_button' href='javascript:void(0);' id='select_no_pipelines'>None</a>\n" +
            "                <div id='show_new_pipelines_container'>" +
            "                    <input id='show_new_pipelines' type='checkbox' name='show_new_pipelines'/>" +
            "                    <label id='show_new_pipelines_label' for='show_new_pipelines'>Show New Pipelines</label>" +
            "                </div>" +
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

    function unselectAllUnder(elem) {
        _select(elem, false);
    }

    function selectAllUnder(elem) {
        _select(elem, true);
    }

    function _select(elem, select) {
        forEachCheckboxUnder(elem, function (checkbox) {
            checkbox.checked = select;
        });
    }

    function forEachCheckboxUnder(elem, lambda) {
        $(elem).select("input[type='checkbox']").each(lambda);
    }

    function assertAllSelected(elem) {
        _assertAllSelected(elem, true);
    }

    function assertAllUnselected(elem) {
        _assertAllSelected(elem, false);
    }

    function _assertAllSelected(elem, checked) {
        forEachCheckboxUnder(elem, function (checkbox) {
            assertEquals(checked, checkbox.checked);
        });
    }

    function isBrowserIE() {
        return window.navigator.userAgent.toLowerCase().indexOf("msie") != -1;
    }

    function setCheckBox(elem, value) {
        if (!isBrowserIE()) {
            elem.checked = value;
            return;
        }
        if (value) {
            document.getElementById(elem.id).setAttribute('checked', 'checked');
        } else {
            document.getElementById(elem.id).removeAttribute('checked');
        }
    }

    it("test_check_uncheck_all_pipelines_when_group_selected", function () {
        unselectAllUnder('selector_group_group-1');
        assertAllUnselected('selector_group_group-1');
        fire_event($('select_group_group-1'), 'click', function (_, elem) {
            setCheckBox(elem, true);
        });
        assertAllSelected('selector_group_group-1');
        fire_event($('select_group_group-1'), 'click', function (_, elem) {
            setCheckBox(elem, false);
        });
        assertAllUnselected('selector_group_group-1');
    });


    it("test_check_uncheck_group_when_all_pipelines_are_checked", function () {
        selectAllUnder('selector_group_group-1');
        fire_event($('select_pipeline_pipeline-1'), 'click', function (_, elem) {
            setCheckBox(elem, false);
        });
        assertFalse("the group should not be checked if pipeline(pipeline-1) under it is unchecked", $('select_group_group-1').checked);
        fire_event($('select_pipeline_pipeline-2'), 'click', function (_, elem) {
            setCheckBox(elem, false);
        });
        assertFalse("the group should not be checked if pipeline(pipeline-2 and pipeline-1) under it are unchecked", $('select_group_group-1').checked);

        //select all piplines
        fire_event($('select_pipeline_pipeline-1'), 'click', function (_, elem) {
            setCheckBox(elem, true);
        });
        fire_event($('select_pipeline_pipeline-2'), 'click', function (_, elem) {
            setCheckBox(elem, true);
        });
        assertTrue("the group should be checked if all pipelines under it are checked", $('select_group_group-1').checked);
    });

    it("test_ALL_should_select_all_groups_pipelines", function () {
        fire_event($('select_all_pipelines'), "click");
        assertAllSelected('pipelines_selector_pipelines');
    });

    it("test_ALL_should_not_select_blacklist_checkbox", function () {
        assertFalse(jQuery("#show_new_pipelines").prop("checked"));
        fire_event($('select_all_pipelines'), "click");
        assertFalse(jQuery("#show_new_pipelines").prop("checked"));
    });

    it("test_NONE_should_unselect_all_groups_pipelines", function () {
        fire_event($('select_no_pipelines'), "click");
        assertAllUnselected('pipelines_selector_pipelines');
    });

    it("test_NONE_should_not_unselect_blacklist_checkbox", function () {
        jQuery("#show_new_pipelines").prop("checked", true);
        fire_event($('select_no_pipelines'), "click");
        assertTrue(jQuery("#show_new_pipelines").prop("checked"));
    });
});
