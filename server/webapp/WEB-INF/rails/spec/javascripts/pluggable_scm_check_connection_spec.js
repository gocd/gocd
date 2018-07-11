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

describe("pluggable_scm_check_connection", function () {
    beforeEach(function () {
        setFixtures("<form accept-charset=\"UTF-8\" action=\"/go/admin/pipelines/up42/materials/pluggable_scm/git\" class=\"popup_form ng-scope ng-pristine ng-invalid ng-invalid-required\" id=\"material_form\" method=\"post\" name=\"material_form\" novalidate=\"novalidate\" onsubmit=\"return AjaxForm.jquery_ajax_submit(this);\"><div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"✓\"><input name=\"authenticity_token\" type=\"hidden\" value=\"XZDkTl/L7FU/gchK/3d/2hwQunCW6kRFKmXgxEgYzs4=\"></div>\n" +
        "\n" +
        "    <input id=\"config_md5\" name=\"config_md5\" type=\"hidden\" value=\"9e238cd71ce1e8f1bbf269e1225992df\">\n" +
        "\n" +
        "    <input id=\"material_scmId\" name=\"material[scmId]\" type=\"hidden\">\n" +
        "    <div class=\"form_content exec_task_editor\">\n" +
        "        <div id=\"material\">\n" +
        "            <div class=\"fieldset\">\n" +
        "                <div id=\"material_angular_pluggable_material_git\" name=\"material_angular_pluggable_material_git\" class=\"plugged_material ng-scope\" ng-controller=\"material_angular_pluggable_material_git_controller\">\n" +
        "                    <div class=\"form_item\">\n" +
        "                        <div class=\"form_item_block required\">\n" +
        "                            <label for=\"material_name\">Material Name<span class=\"asterisk\">*</span></label>\n" +
        "                            <input class=\"form_input MB_focusable\" name=\"material[name]\" type=\"text\" value=\"scm-name\">                            \n" +
        "                        </div>\n" +
        "\n" +
        "                        <div class=\"plugged_material_template required\">\n" +
        "                            <div class=\"form_item_block\">\n" +
        "                                <label>URL:<span class=\"asterisk\">*</span></label>\n" +
        "                                <input type=\"text\" ng-model=\"url\" ng-required=\"true\" class=\"MB_focusable ng-pristine ng-invalid ng-invalid-required\" name=\"material[url]\" servererror=\"undefined\" required=\"required\" value=\"scm-url\">\n" +
        "                                <span class=\"form_error ng-binding\" ng-show=\"material_form['material[url]'].$error.server\" style=\"display: none;\"></span>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "\n" +
        "                        <div class=\"form_item_block with-padding-top\">\n" +
        "                            <button class=\"submit button MB_focusable\" id=\"check_connection_pluggable_scm\" type=\"button\" value=\"CHECK CONNECTION\"><span>CHECK CONNECTION</span></button>                            <span id=\"pluggable_scm_check_connection_message\"></span>\n" +
        "                        </div>\n" +
        "\n" +
        "                        <span id=\"material_data_pluggable_material_git\" class=\"plugged_material_data\" style=\"display: none\">\n" +
        "                            {}\n" +
        "                        </span>\n" +
        "\n" +
        "                        <div class=\"form_item_block checkbox_row material_options\">\n" +
        "                            <input name=\"material[autoUpdate]\" type=\"hidden\" value=\"0\"><input checked=\"checked\" class=\"form_input MB_focusable\" id=\"material_autoUpdate\" name=\"material[autoUpdate]\" type=\"checkbox\" value=\"true\">                            <label for=\"material_autoUpdate\">Poll for new changes</label>\n" +
        "\n" +
        "                        </div>\n" +
        "\n" +
        "                        <div class=\"form_item_block\">\n" +
        "                            <label>Destination Directory:</label>\n" +
        "                            <input class=\"form_input MB_focusable\" name=\"material[folder]\" type=\"text\" value=\"scm-destination\">                            \n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <p class=\"required\">\n" +
        "                    <span class=\"asterisk\">*</span> indicates a required field</p>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "\n" +
        "\n" +
        "            <h3>Blacklist</h3>\n" +
        "            <div class=\"fieldset\">\n" +
        "                <label>Enter the paths to be excluded. Separate multiple entries with a comma.</label>\n" +
        "\n" +
        "                <textarea class=\"form_input MB_focusable\" name=\"material[filterAsString]\" rows=\"2\">scm-filter</textarea>\n" +
        "                <div class=\"clear\"></div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "    <div class=\"form_buttons actions\">\n" +
        "        <button class=\"primary finish submit MB_focusable\" type=\"submit\" value=\"SAVE\"><span>SAVE</span></button>\n" +
        "        <button class=\"left submit close_modalbox_control MB_focusable\"><span>Cancel</span></button>\n" +
        "    </div>\n" +
        "\n" +
        "\n" +
        "\n" +
        "</form>\n");
    });

    it("testShouldCheckConnectionAndSetConnectionOkMessage", function () {
        var post_url = "admin/pipelines/pipeline_name/materials/pluggable_scm/check_connection/plugin_id";
        new PluggableSCMCheckConnection(post_url).bind("#material_form", "#check_connection_pluggable_scm", "#pluggable_scm_check_connection_message");
        var actual_options;
        jQuery.ajax = function (options) {
            actual_options = options;
            var waiting_message_element = jQuery('#pluggable_scm_check_connection_message');
            assertEquals('Checking connection...', waiting_message_element.text());
            assertEquals("Waiting message should not have success or error classes", false, waiting_message_element.hasClass("error_message"));
            assertEquals("Waiting message should not have success or error classes", false, waiting_message_element.hasClass("ok_message"));
            options.success({status:"success", messages:["Connection OK"]});
        };

        jQuery("#check_connection_pluggable_scm").click();

        assertEquals(post_url, actual_options.url);
        assertEquals("POST", actual_options.type);
        assertEquals(jQuery('#material_form').serialize(), actual_options.data);
        assertEquals("Should set ok_message class", true, jQuery("#pluggable_scm_check_connection_message").hasClass("ok_message"))
        assertEquals("Connection ok message not showing up", "Connection OK", jQuery("#pluggable_scm_check_connection_message").text())
    });

    it("testShouldCheckConnectionFailureMessage", function () {
        var post_url = "admin/pipelines/pipeline_name/materials/pluggable_scm/check_connection/plugin_id";
        new PluggableSCMCheckConnection(post_url).bind("#material_form", "#check_connection_pluggable_scm", "#pluggable_scm_check_connection_message");
        jQuery.ajax = function (options) {
            options.success({status:"failure", messages:["Connection NOT OK"]});
        };

        jQuery("#check_connection_pluggable_scm").click();

        assertEquals("Should set error_message class", true, jQuery("#pluggable_scm_check_connection_message").hasClass("error_message"))
        assertEquals("Error message not showing up", "Connection NOT OK", jQuery("#pluggable_scm_check_connection_message").text())
    });

    it("testShouldCheckConnectionHandleError", function () {
        var post_url = "admin/pipelines/pipeline_name/materials/pluggable_scm/check_connection/plugin_id";
        new PluggableSCMCheckConnection(post_url).bind("#material_form", "#check_connection_pluggable_scm", "#pluggable_scm_check_connection_message");
        jQuery.ajax = function (options) {
            options.error();
        };

        jQuery("#check_connection_pluggable_scm").click();

        assertEquals("Should set error_message class", true, jQuery("#pluggable_scm_check_connection_message").hasClass("error_message"))
        assertEquals("Error message not showing up", "Error occurred!", jQuery("#pluggable_scm_check_connection_message").text())
    });

    it("testShouldAssertThatOnlyTheRelevantFormIsSerialize", function () {
        var post_url = "admin/pipelines/pipeline_name/materials/pluggable_scm/check_connection/plugin_id";
        new PluggableSCMCheckConnection(post_url).bind("#material_form", "#check_connection_pluggable_scm", "#pluggable_scm_check_connection_message");
        var formData = "unknown";
        jQuery.ajax = function (options) {
            formData = options.data;
        };

        jQuery("#check_connection_pluggable_scm").click();

        assertNotEquals("unknown", formData);
        var parseData = parseQueryParams(formData);
        assertEquals("scm-name", parseData['material[name]']);
        assertEquals("scm-url", parseData['material[url]']);
        assertEquals("true", parseData['material[autoUpdate]']);
        assertEquals("scm-destination", parseData['material[folder]']);
        assertEquals("scm-filter", parseData['material[filterAsString]']);
    });

    function parseQueryParams(data) {
        var result = {};
        var allPairs = data.split("&");
        jQuery(allPairs).each(function (i, d) {
            var keyAndValue = d.split("=");
            result[decodeURI(keyAndValue[0])] = decodeURI(keyAndValue[1]);
        });
        return result;
    }
});
