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

describe("environment_wizard", function () {
    var init;
    var originalSubTabsPrototypeOpen = SubTabs.prototype.open;

    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <div class=\"sub_tabs_container\">\n" +
            "        <ul class=\"tabs\">\n" +
            "            <li id=\"some_link\" class=\"current_tab\">\n" +
            "                <a href=\"#\"><div>Step 1</div>Name</a>\n" +
            "                <a href=\"#\" class=\"tab_button_body_match_text\">env-name</a>\n" +
            "            </li>\n" +
            "            <li id=\"another_link\" class=\"subsequent_tab  disabled\">\n" +
            "                <a href=\"#\"><div>Step 2</div>Pipelines</a>\n" +
            "                <a href=\"#\" class=\"tab_button_body_match_text\">env-pipelines</a>\n" +
            "            </li>\n" +
            "        </ul>\n" +
            "    </div>\n" +
            "    <div class=\"sub_tab_container_content\">\n" +
            "        <div id=\"tab-content-of-env-name\">\n" +
            "            <span class=\"content_inside_enabled\">Visible</span>\n" +
            "        </div>\n" +
            "        <div id=\"tab-content-of-env-pipelines\">\n" +
            "            <span class=\"content_inside_disabled\">Hidden</span>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <div class=\"environment_variables_section\">\n" +
            "        <div class=\"section_title\">Environment Variables (Name = Value)</div>\n" +
            "        <textarea id=\"environment_variables_template\" class=\"template\">\n" +
            "            <input type=\"text\" name=\"environment_variables[][name]\" class=\"form_input environment_variable_name\"/>\n" +
            "            <span class=\"equals_sign\">=</span>\n" +
            "            <input type=\"text\" name=\"environment_variables[][value]\" class=\"form_input environment_variable_value\"/>\n" +
            "            <span class=\"icon_remove delete_parent\"></span>\n" +
            "            <span class=\"wizard_error_message\"></span>\n" +
            "        </textarea>\n" +
            "        <ul class=\"variables\">\n" +
            "        </ul>\n" +
            "        <a class=\"add_item\" id=\"add_variable\">Add</a>\n" +
            "    </div>\n" +
            "    <div id=\"finish_box\">\n" +
            "        <button id=\"finish\" name=\"FINISH\" type=\"submit\"></button>\n" +
            "    </div>\n" +
            "</div>");
        SubTabs.prototype.open = originalSubTabsPrototypeOpen;
    });


    beforeEach(function () {
        init = $$(".under_test").first().innerHTML;
        var rowCreator = new EnvironmentVariables.RowCreator(jQuery('#environment_variables_template'), 'li', '.delete_parent');
        var validatorList = null;
        var validators = new EnvironmentVariables.Validators({'.environment_variable_name': ['blur']}, validatorList);
        environmentVariables = new EnvironmentVariables(jQuery('ul.variables'), rowCreator, validators);
        environmentVariables.registerAddButton(jQuery("#add_variable"));
        environmentVariables.registerFinishButton(jQuery("#finish"));
        finish_succeeded = false;
        jQuery('#finish_box').click(function () {
            finish_succeeded = true;
        });
        add_button = jQuery('#add_variable').get(0);
    });

    afterEach(function() {
        $$(".under_test").first().innerHTML = init;
        SubTabs.prototype.open = originalSubTabsPrototypeOpen;
    });

    function setValue(variable, value) {
        jQuery(variable).find(".environment_variable_name").val(value);
    }

    it("test_finishes_if_valid", function () {
        addVar();
        var added = jQuery(jQuery('.variables > li').get(0));
        setValue(added, "abc");
        clickFinish();
        assertEquals('', added.find(".wizard_error_message").html());
        assertTrue("finish should have succeeded", finish_succeeded);
    });

    it("test_add_default_row", function () {
        environmentVariables.addDefaultRow();
        var rows = jQuery('.variables > li');
        assertContains("environment_variable_name", rows.html());
        assertContains("environment_variable_value", rows.html());
        assertContains("environment_variables[][value]", rows.html());
        assertContains("environment_variables[][name]", rows.html());
    });

    it("test_do_not_handle_click_for_element_with_disabled_parent", function () {
        new TabsManager("env-name", 'environment_form', 'new-environment');
        var opened = false;
        SubTabs.prototype.open = function (event) {
            opened = true;
        }
        fire_event(jQuery("#tab-link-of-env-pipelines").get(0), 'click');
        assertFalse(opened);
        fire_event(jQuery("#tab-link-of-env-name").get(0), 'click');
        assertTrue(opened);
    });

    function addVar() {
        fire_event(add_button, 'click');
    }

    function clickFinish() {
        fire_event(jQuery('#finish').get(0), 'click');
    }

    function clickDelete(zeroth) {
        fire_event(jQuery(zeroth).find('.delete_parent').get(0), 'click');
    }

    it("test_should_remove_a_env_var_on_delete", function () {
        addVar();
        addVar();

        var rows = jQuery('.variables > li');
        var zeroth = jQuery(rows.get(0));
        var first = jQuery(rows.get(1));

        clickFinish();

        setValue(first, "foo");

        clickDelete(zeroth);
        clickFinish();

        assertTrue("Should finish because invalid row is deleted", finish_succeeded);
    });
});
