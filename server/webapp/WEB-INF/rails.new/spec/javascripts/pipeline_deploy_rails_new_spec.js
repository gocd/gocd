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

describe("pipeline_deploy_rails_new", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <form accept-charset='UTF-8' action='/api/pipelines/pipline-name/schedule' method='post' onsubmit=\"PipelineOperations.onTriggerWithOptionsDialog(this, 'pipline-name', '/api/pipelines/pipline-name/schedule'); return false;; \">\n" +
            "        <div style='margin:0;padding:0;display:inline'>\n" +
            "            <input name='utf8' type='hidden' value='&#x2713;'/>\n" +
            "        </div>\n" +
            "        <div class='change_materials'>\n" +
            "            <div class='sub_tabs_container'>\n" +
            "                <ul>\n" +
            "                    <li id='materials_tab' class='current_tab'>\n" +
            "                        <a class='tab_button_body_match_text'>materials</a>\n" +
            "                        <a>Materials</a>\n" +
            "                    </li>\n" +
            "                    <li id='environment_variables_tab'>\n" +
            "                        <a class='tab_button_body_match_text'>environment-variables</a>\n" +
            "                        <a>Environment Variables</a>\n" +
            "                    </li>\n" +
            "                </ul>\n" +
            "            </div>\n" +
            "            <div class='sub_tab_container'>\n" +
            "                <div class='sub_tab_container_content'>\n" +
            "                    <div class='materials' id='tab-content-of-materials'>\n" +
            "                        <div class='material_summaries'>\n" +
            "                            <div class='material_summary selected first'>\n" +
            "                                <div id='material-number-0' class='revision_number' title='1234'>1234</div>\n" +
            "                                <div class='material_name' title='SvnName'>SvnName</div>\n" +
            "                            </div>\n" +
            "                            <div class='material_summary '>\n" +
            "                                <div id='material-number-1' class='revision_number' title='9fdcf27f16eadc362733328dd481d8a2c29915e1'>9fdcf27f16ea</div>\n" +
            "                                <div class='material_name' title='hg-url'>hg-url</div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class='content_wrapper_outer'>\n" +
            "                            <div class='content_wrapper_inner'>\n" +
            "                                <div class='material_details'>\n" +
            "                                    <div class='material_detail '>\n" +
            "                                        <dl>\n" +
            "                                            <dt>Subversion</dt>\n" +
            "                                            <dd>\n" +
            "                                                <span class='material_name' title='SvnName'>SvnName</span>\n" +
            "                                            </dd>\n" +
            "                                            <dt>Dest:</dt>\n" +
            "                                            <dd class='folder '> Folder </dd>\n" +
            "                                            <dt>Date:</dt>\n" +
            "                                            <dd class='date' title='REPLACED_DATE'>REPLACED_RELATIVE_TIME</dd>\n" +
            "                                            <dt>User:</dt>\n" +
            "                                            <dd class='user'>username</dd>\n" +
            "                                            <dt>Comment:</dt>\n" +
            "                                            <dd class='comment'>I changed something</dd>\n" +
            "                                            <dt>Currently Deployed:</dt>\n" +
            "                                            <dd>\n" +
            "                                                <span id='currently-deployed-0' class='revision_number' title='1234'>1234</span>\n" +
            "                                            </dd>\n" +
            "                                            <dt>Revision to Deploy:</dt>\n" +
            "                                            <dd>\n" +
            "                                                <input id='material-number-0-original' type='hidden' class='original-revision' name='original_fingerprint[36284ebcd009959b86b5e0c30be7e3803d0328f3ae5b8608285f2223de07faa7]' value='1234'/>\n" +
            "                                                <input id='material-number-0-autocomplete' type='text' class='autocomplete-input' name='material_fingerprint[36284ebcd009959b86b5e0c30be7e3803d0328f3ae5b8608285f2223de07faa7]'/>\n" +
            "                                                <div id='material-number-0-autocomplete-content' class='autocomplete bring_to_front'></div>\n" +
            "                                            </dd>\n" +
            "                                        </dl>\n" +
            "                                    </div>\n" +
            "                                    <div class='material_detail hidden'>\n" +
            "                                        <dl>\n" +
            "                                            <dt>Mercurial</dt>\n" +
            "                                            <dd>\n" +
            "                                                <span class='material_name' title='hg-url'>hg-url</span>\n" +
            "                                            </dd>\n" +
            "                                            <dt>Dest:</dt>\n" +
            "                                            <dd class='folder not_set '> not-set </dd>\n" +
            "                                            <dt>Date:</dt>\n" +
            "                                            <dd class='date' title='REPLACED_DATE'>REPLACED_RELATIVE_TIME</dd>\n" +
            "                                            <dt>User:</dt>\n" +
            "                                            <dd class='user'>user2</dd>\n" +
            "                                            <dt>Comment:</dt>\n" +
            "                                            <dd class='comment'>comment2</dd>\n" +
            "                                            <dt>Currently Deployed:</dt>\n" +
            "                                            <dd>\n" +
            "                                                <span id='currently-deployed-1' class='revision_number' title='9fdcf27f16eadc362733328dd481d8a2c29915e1'>9fdcf27f16ea</span>\n" +
            "                                            </dd>\n" +
            "                                            <dt>Revision to Deploy:</dt>\n" +
            "                                            <dd>\n" +
            "                                                <input id='material-number-1-original' type='hidden' class='original-revision' name='original_fingerprint[28fc4e7dda814b71db7fd4f4fbb781583155d64d7bbf526ae740687999286e1e]' value='9fdcf27f16eadc362733328dd481d8a2c29915e1'/>\n" +
            "                                                <input id='material-number-1-autocomplete' type='text' class='autocomplete-input' name='material_fingerprint[28fc4e7dda814b71db7fd4f4fbb781583155d64d7bbf526ae740687999286e1e]'/>\n" +
            "                                                <div id='material-number-1-autocomplete-content' class='autocomplete bring_to_front'></div>\n" +
            "                                            </dd>\n" +
            "                                        </dl>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                    <div class='variables' id=\"tab-content-of-environment-variables\" style='display: none;'>\n" +
            "                        <div class=\"content_wrapper_outer\">\n" +
            "                            <div class=\"content_wrapper_inner\">\n" +
            "                                <span>Override Environment and Pipeline level variables:</span>\n" +
            "                                <div class='variable'>\n" +
            "                                    <label for='variables[foo]'>foo</label>\n" +
            "                                    <input id=\"variable-0\" type='text' name='variables[foo]' value='foo_value'/>\n" +
            "                                    <input type='hidden' name='original_variables[foo]' value='foo_value'/>\n" +
            "                                    <span class='message hidden'>Overwritten. Default: foo_value</span>\n" +
            "                                </div>\n" +
            "                                <div class='variable'>\n" +
            "                                    <label for='variables[bar]'>bar</label>\n" +
            "                                    <input id=\"variable-1\" type='text' name='variables[bar]' value='bar_value'/>\n" +
            "                                    <input type='hidden' name='original_variables[bar]' value='bar_value'/>\n" +
            "                                    <span class='message hidden'>Overwritten. Default: bar_value</span>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "\n" +
            "            <div class='actions'>\n" +
            "                <button class='primary submit disabled' disabled='disabled' type='submit' value='Deploy Changes'>\n" +
            "                    <span>DEPLOY CHANGES</span>\n" +
            "                </button>\n" +
            "                <button class='submit button' id='close' type='button' value='Close'>\n" +
            "                    <span>CLOSE</span>\n" +
            "                </button>\n" +
            "            </div>\n" +
            "\n" +
            "\n" +
            "        </div>\n" +
            "\n" +
            "    </form>\n" +
            "</div>");
    });
    var actual_timeout = setTimeout;

    var material_detail_panes = null;
    var material_summary_panes = null;

    var actual_ajax_request = Ajax.Request;

    beforeEach(function () {
        setTimeout = function(fn) {
            fn();
        }
        material_detail_panes = $$(".materials .material_details .material_detail");
        material_summary_panes = $$(".materials .material_summaries .material_summary");

        init_trigger_popup();
        CruiseAutocomplete.materialSearch(true, 'pipline-name', '70fd7f3da3a0e91de9a2bc0a9e02efc96134271fd7574b925028f9692aa61405', 'material-number-0-autocomplete',
            function (short_revision, long_revision) {
                var external_revision = $('material-number-0');
                external_revision.update(short_revision);
                external_revision.writeAttribute('title', long_revision);
                if ($("currently-deployed-0").title == long_revision) {
                    external_revision.removeClassName('updated');
                    materialDirtyTracker.undoUpdate(0);
                } else {
                    external_revision.addClassName('updated');
                    materialDirtyTracker.update(0);
                }
            });


        CruiseAutocomplete.materialSearch(false, 'pipline-name', '4290e91721d0a0be34955725cfd754113588d9c27c39f9bd1a97c15e55832515', 'material-number-1-autocomplete',
            function (short_revision, long_revision) {
                var external_revision = $('material-number-1');
                external_revision.update(short_revision);
                external_revision.writeAttribute('title', long_revision);
                if ($("currently-deployed-1").title == long_revision) {
                    external_revision.removeClassName('updated');
                    materialDirtyTracker.undoUpdate(1);
                } else {
                    external_revision.addClassName('updated');
                    materialDirtyTracker.update(1);
                }
            });


        materialDirtyTracker = new DirtyTracker('materials_tab');
        environmentVariableDirtyTracker = new DirtyTracker('environment_variables_tab');
        new TabsManager('materials_tab', 'material_popup', 'UNIQUE_ID');
    });

    afterEach(function () {
        Ajax.Request = actual_ajax_request;
        setTimeout = actual_timeout;
        materialDirtyTracker = null;
        environmentVariableDirtyTracker = null;
    });

    it("test_should_toggle_display_between_materials", function () {
        fire_event(material_summary_panes[1], 'click');

        assertFalse("First summary should not be selected on click of second summary", material_summary_panes[0].hasClassName('selected'));
        assertTrue("First detail should be hidden on click of second summary", material_detail_panes[0].hasClassName('hidden'));

        assertTrue("second summary should be selected on click of second summary", material_summary_panes[1].hasClassName('selected'));
        assertFalse("Second detail should be shown on click of second", material_detail_panes[1].hasClassName('hidden'));
    });

    it("test_second_click_should_keep_the_details_open_if_it_already_is_blah_blah", function () {
        fire_event(material_summary_panes[1], 'click');
        assertFalse("should select on first click", material_detail_panes[1].hasClassName('hidden'));
        assertTrue("should open on first click", material_summary_panes[1].hasClassName('selected'));

        fire_event(material_summary_panes[1], 'click');
        assertFalse("stay selected after second click", material_detail_panes[1].hasClassName('hidden'));
        assertTrue("stay open after second click", material_summary_panes[1].hasClassName('selected'));
    });

    it("test_should_update_revision_to_deploy_when_selected", function () {
        Ajax.Request = function (url, options) {
            options.onComplete({responseText: '<ul class="smartfill_content">'
                + '<li id="matched-revision-0">'
                + '  <div class="revision" title="long-revision1">rev1</div>'
                + '  <div class="user">user1</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">comment1</div>'
                + '</li>'
                + '<li id="matched-revision-1">'
                + '  <div class="revision" title="long-revision2">rev2</div>'
                + '  <div class="user">user2</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">comment2</div>'
                + '</li>'
                + '</ul>'
            });
        };
        $('material-number-0-autocomplete').setValue("rev");
        fire_event($('material-number-0-autocomplete'), 'keydown');
        fire_event($('matched-revision-0'), "click");
        assertEquals("First was selected", $('material-number-0-autocomplete').getValue(), "long-revision1");
    });

    it("test_should_update_navigation_bar_with_selected_revision", function () {
        Ajax.Request = function (url, options) {
            options.onComplete({responseText: '<ul class="smartfill_content">'
                + '<li id="matched-revision-0">'
                + '  <div class="revision" title="long-revision1">rev1</div>'
                + '  <div class="user">user1</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">comment1</div>'
                + '</li>'
                + '<li id="matched-revision-1">'
                + '  <div class="revision" title="long-revision2">rev2</div>'
                + '  <div class="user">user2</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">comment2</div>'
                + '</li>'
                + '<li id="matched-revision-2">'
                + '  <div class="revision" title="1234">1234</div>'
                + '  <div class="user">username</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">existing material</div>'
                + '</li>'
                + '</ul>'
            });
        };
        $('material-number-0-autocomplete').setValue("rev");
        fire_event($('material-number-0-autocomplete'), 'keydown');

        // Choosing new revision
        fire_event($('matched-revision-1'), "click");
        assertEquals("Short revision set", $('material-number-0').textContent, "rev2");
        assertEquals("Long revision set", $('material-number-0').title, "long-revision2");
        assertTrue("Contains 'updated' class", $('material-number-0').hasClassName('updated'));

        // Choosing existing revision
        fire_event($('matched-revision-2'), "click");
        assertEquals("Short revision set", $('material-number-0').textContent, "1234");
        assertEquals("Long revision set", $('material-number-0').title, "1234");
        assertFalse("Contains 'updated' class", $('material-number-0').hasClassName('updated'));
    });

    it("test_should_preload_with_latest_5", function () {
        Ajax.Request = function (url, options) {
            options.onComplete({responseText: '<ul class="smartfill_content">'
                + '<li id="matched-revision-0">'
                + '  <div class="revision" title="long-revision1">rev1</div>'
                + '  <div class="user">user1</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">comment1</div>'
                + '</li>'
                + '<li id="matched-revision-1">'
                + '  <div class="revision" title="long-revision2">rev2</div>'
                + '  <div class="user">user2</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">comment2</div>'
                + '</li>'
                + '<li id="matched-revision-2">'
                + '  <div class="revision" title="1234">1234</div>'
                + '  <div class="user">username</div>'
                + '  <div class="checkinTime">2009-01-01</div>'
                + '  <div class="comment">existing material</div>'
                + '</li>'
                + '</ul>'
            });
        };
        fire_event($('material-number-0-autocomplete'), 'focus');

        // Choosing new revision
        fire_event($('matched-revision-0'), "click");
        assertEquals("First was selected", $('material-number-0-autocomplete').getValue(), "long-revision1");
    });

    it("test_should_show_message_when_default_value_changes", function () {
        var foo_text_box = $$("input[name='variables[foo]']")[0];
        fire_event(foo_text_box, "change");
        assertTrue($$(".message")[0].hasClassName("hidden"));
        foo_text_box.value = "changed";
        fire_event(foo_text_box, "change");
        assertFalse($$(".message")[0].hasClassName("hidden"));

    });

    it("test_remove_unchanged", function () {
        disable_unchanged();
        assertTrue($$("input[name='variables[bar]']")[0].disabled);
        assertTrue($$("input[name='variables[foo]']")[0].disabled);

        $$(".variable input[type='text']").each(function (textbox) {
            textbox.enable();
            textbox.value = "changed";
        });
        disable_unchanged();
        assertFalse($$("input[name='variables[bar]']")[0].disabled);
        assertFalse($$("input[name='variables[foo]']")[0].disabled);
    });
});
