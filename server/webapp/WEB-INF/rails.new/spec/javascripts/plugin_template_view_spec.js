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

describe("plugin_template_view", function () {
    beforeEach(function () {
        setFixtures("<form id=\"form_id\" name=\"form_id\">\n" +
            "    <div id=\"plugin_template_view_angular_app_id\" class=\"plugin_template_view\" ng-controller=\"PluginTemplateViewController\">\n" +
            "        <div class=\"plugin_template_view_template\" plugin-template=\"task[onCancel]\">\n" +
            "            <input type='text' ng-model='test_data'/>\n" +
            "            <p id=\"test_data_test_para\">{{test_data}}</p>\n" +
            "\n" +
            "            <input type='text' ng-model='something_else'/>\n" +
            "            <p id=\"something_else_test_para\">{{something_else}}</p>\n" +
            "\n" +
            "            <textarea cols=\"50\" id=\"more_data\" ng-model=\"more_data\" rows=\"5\" wrap=\"off\"></textarea>\n" +
            "            <p id=\"more_data_test_para\">{{more_data}}</p>\n" +
            "        </div>\n" +
            "\n" +
            "    <span id=\"plugin_template_view_data_element_id\" class=\"plugin_template_view_data\" plugin-data style=\"display: none\">\n" +
            "        {\"test_data\": {\"value\":\"X\"}, \"something_else\": {\"value\":\"Y\"}, \"more_data\": {\"value\":\"More\\nData\"}}\n" +
            "    </span>\n" +
            "    </div>\n" +
            "\n" +
            "\n" +
            "    <div id=\"error_plugin_template_view_angular_app_id\" ng-controller=\"PluginTemplateViewController\" class=\"plugin_template_view\">\n" +
            "        <div class=\"plugin_template_view_template\" plugin-template=\"form_name_prefix\">\n" +
            "            <input type='text' ng-model='test_data' />\n" +
            "            <input type='text' ng-model='something_else' />\n" +
            "\n" +
            "            <p id=\"something_else_server_error\" ng-show=\"GOINPUTNAME[something_else].$error.server\">{{ errors[\"something_else\"] }}</p>\n" +
            "            <p id=\"something_else_server_error_with_dollar_error_notation\" ng-show=\"GOINPUTNAME[something_else].$error.server\">{{ GOINPUTNAME[something_else].$error.server }}</p>\n" +
            "            <p id=\"something_else_server_error_without_ng_show\">{{ errors[\"something_else\"] }}</p>\n" +
            "            <p id=\"test_data_server_error\" ng-show=\"GOINPUTNAME[test_data].$error.server\">{{ errors[\"test_data\"]}}</p>\n" +
            "            <p id=\"form_server_error\" ng-show=\"GOFORMNAME.$error.server\">Some server error</p>\n" +
            "        </div>\n" +
            "\n" +
            "    <span id=\"error_plugin_template_view_data_element_id\" class=\"plugin_template_view_data\" plugin-data style=\"display: none\">\n" +
            "        {\"test_data\": {\"value\":\"X\"}, \"something_else\": {\"errors\": \"required, incorrect format\", \"value\":\"Y\"}}\n" +
            "    </span>\n" +
            "    </div>\n" +
            "\n" +
            "\n" +
            "    <div id=\"empty_data_plugin_template_view_angular_app_id\" ng-controller=\"PluginTemplateViewController\" class=\"plugin_template_view\">\n" +
            "        <div class=\"plugin_template_view_template\" plugin-template=\"prefix\">\n" +
            "            <input type='text' ng-model='initially_empty'/>\n" +
            "\n" +
            "            <p id=\"initially_empty_para\">{{initially_empty}}</p>\n" +
            "        </div>\n" +
            "\n" +
            "    <span id=\"empty_plugin_template_view_data_element_id\" class=\"plugin_template_view_data\" plugin-data style=\"display: none\">\n" +
            "        {\"initially_empty\": \"\"}\n" +
            "    </span>\n" +
            "    </div>\n" +
            "\n" +
            "    <button class=\"submit finish\"/>\n" +
            "\n" +
            "</form>");
        jQuery('#plugin_template_view_angular_app_id input').val("").trigger("input");
        jQuery('#empty_plugin_template_view_angular_app_id input').val("").trigger("input");
    });

    var html;
    var appName = "task_app";

    it("test_should_create_the_app_and_fill_inputs_with_attached_data", function () {
        new PluginTemplateView().bootstrapAngular('form_id');

        // just to make sure it's there
        assertEquals("PluginTemplateViewController", jQuery("#plugin_template_view_angular_app_id").attr("ng-controller"));
        assertNotNull(angular.module("PluginTemplateViewApp"));

        assertEquals("X", textbox_1().val());
        assertEquals("X", para_1().text());

        assertEquals("Y", textbox_2().val());
        assertEquals("Y", para_2().text());

        assertEquals("More\nData", textarea().val());
        assertEquals("More\nData", para_3().text());
    });

    if (window.navigator.userAgent.indexOf("MSIE")<=0) {
        it("test_should_ensure_angular_binding_for_plugin_template_view_works", function () {
            new PluginTemplateView().bootstrapAngular('form_id');

            textbox_1().val("NEW_VALUE_1").trigger("input");
            assertEquals("NEW_VALUE_1", textbox_1().val());
            assertEquals("NEW_VALUE_1", para_1().text());

            textbox_2().val("NEW_VALUE_2").trigger("input");
            assertEquals("NEW_VALUE_2", textbox_2().val());
            assertEquals("NEW_VALUE_2", para_2().text());
        });
    }


    it("test_should_change_name_of_all_input_elements_to_have_form_name", function () {
        new PluginTemplateView().bootstrapAngular('form_id');

        assertEquals("task[onCancel][test_data]", textbox_1().attr("name"));
        assertEquals("task[onCancel][something_else]", textbox_2().attr("name"));
    });


    it("test_should_change_name_of_textarea_elements_to_have_form_name", function () {
        new PluginTemplateView().bootstrapAngular('form_id');

        assertEquals("task[onCancel][more_data]", textarea().attr("name"));
    });

    if (window.navigator.userAgent.indexOf("MSIE")<=0) {
        it("test_should_not_fail_to_bind_when_there_is_no_data", function () {
            new PluginTemplateView().bootstrapAngular('form_id');

            var initially_empty_textbox = jQuery("#empty_data_plugin_template_view_angular_app_id input[ng-model='initially_empty']");
            var initially_empty_para = jQuery("#empty_data_plugin_template_view_angular_app_id p#initially_empty_para");

            assertEquals("", initially_empty_textbox.val());
            assertEquals("", initially_empty_para.text());

            initially_empty_textbox.val("NEW_VALUE_1").trigger("input");
            assertEquals("NEW_VALUE_1", initially_empty_textbox.val());
            assertEquals("NEW_VALUE_1", initially_empty_para.text());
        });
    }


    it("test_should_map_server_side_validation_errors", function () {
        new PluginTemplateView().bootstrapAngular('form_id');

        assertEquals("X", jQuery("#error_plugin_template_view_angular_app_id input[ng-model='test_data']").val());
        assertEquals("Y", jQuery("#error_plugin_template_view_angular_app_id input[ng-model='something_else']").val());

        assertEquals("required, incorrect format", jQuery("#error_plugin_template_view_angular_app_id #something_else_server_error").text());
        assertEquals(true, jQuery("#error_plugin_template_view_angular_app_id #something_else_server_error").is(":visible"));

        assertEquals("required, incorrect format", jQuery("#error_plugin_template_view_angular_app_id #something_else_server_error_with_dollar_error_notation").text());
        assertEquals(true, jQuery("#error_plugin_template_view_angular_app_id #something_else_server_error_with_dollar_error_notation").is(":visible"));

        assertEquals("required, incorrect format", jQuery("#error_plugin_template_view_angular_app_id #something_else_server_error_without_ng_show").text());
        assertEquals(true, jQuery("#error_plugin_template_view_angular_app_id #something_else_server_error_without_ng_show").is(":visible"));

        assertEquals("Some server error", jQuery("#error_plugin_template_view_angular_app_id #form_server_error").text());
        assertEquals(true, jQuery("#error_plugin_template_view_angular_app_id #form_server_error").is(":visible"));

        assertEquals("", jQuery("#error_plugin_template_view_angular_app_id #test_data_server_error").text());
        assertEquals(false, jQuery("#error_plugin_template_view_angular_app_id #test_data_server_error").is(":visible"));
    });


    function textbox_1() {
        return jQuery("#plugin_template_view_angular_app_id input[ng-model='test_data']");
    }

    function textbox_2() {
        return jQuery("#plugin_template_view_angular_app_id input[ng-model='something_else']");
    }

    function para_1() {
        return jQuery("#plugin_template_view_angular_app_id p#test_data_test_para");
    }

    function para_2() {
        return jQuery("#plugin_template_view_angular_app_id p#something_else_test_para");
    }

    function textarea() {
        return jQuery("#plugin_template_view_angular_app_id textarea[ng-model='more_data']");
    }

    function para_3() {
        return jQuery("#plugin_template_view_angular_app_id p#more_data_test_para");
    }
});
