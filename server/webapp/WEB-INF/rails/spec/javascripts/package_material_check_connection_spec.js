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

describe("package_material_check_connection", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <form class='invalid_form_for_testing'>\n" +
            "        <input id='invalid_input_element' name=\"invalid_input_element\" value=\"FOOOOOO\" />\n" +
            "    </form>\n" +
            "    <form id=\"package_repo\">\n" +
            "        <div class=\"selected-package-repository\">\n" +
            "            <div class=\"messages\">\n" +
            "                Some messages\n" +
            "            </div>\n" +
            "            <div class=\"form_item required fieldWithErrors\">\n" +
            "                <label>fieldName</label>\n" +
            "                <input type=\"text\" value=\"fieldValue\">\n" +
            "\n" +
            "                <div class=\"form_error\">error message here</div>\n" +
            "            </div>\n" +
            "            <div>\n" +
            "                <input name=\"field1\" type=\"text\" value=\"field1Value\">\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div id=\"parent_container\">\n" +
            "            <fieldset>\n" +
            "                <div class=\"field required\">\n" +
            "                    <input id=\"package_repository_configuration_0_configurationKey_name\" name=\"package_repository[configuration][0][configurationKey][name]\" type=\"hidden\" value=\"REPO_URL\">\n" +
            "                    <label for=\"package_repository_configuration_0_configurationValue_value\">Repository URL<span class=\"asterisk\">*</span></label>\n" +
            "                    <input id=\"package_repository_configuration_0_configurationValue_value\" name=\"package_repository[configuration][0][configurationValue][value]\" size=\"30\" type=\"text\" value=\"file:///tmp/repo/\">\n" +
            "                </div>\n" +
            "                <div class=\"field required\">\n" +
            "                    <input id=\"package_repository_configuration_1_configurationKey_name\" name=\"package_repository[configuration][1][configurationKey][name]\" type=\"hidden\" value=\"USER\">\n" +
            "                    <label for=\"package_repository_configuration_1_configurationValue_value\">Repository URL<span class=\"asterisk\">*</span></label>\n" +
            "                    <input id=\"package_repository_configuration_1_configurationValue_value\" name=\"package_repository[configuration][1][configurationValue][value]\" size=\"30\" type=\"text\" value=\"user_name\">\n" +
            "                </div>\n" +
            "            </fieldset>\n" +
            "            <button class=\"submit button\" id=\"check_connection_button\" type=\"button\" value=\"CHECK CONNECTION\"><span>CHECK CONNECTION</span></button>\n" +
            "            <span id=\"connection_message\" class=\"ok_message error_message\"></span>\n" +
            "        </div>\n" +
            "    </form>\n" +
            "\n" +
            "</div>");
    });
    it("testShouldCheckConnectionAndSetConnectionOkMessage", function () {
        new PackageMaterialCheckConnection("admin/package_repositories/check_connection").bind("#parent_container", "#check_connection_button", "#connection_message");
        var actual_options;
        jQuery.ajax = function (options) {
            actual_options = options;
            var waiting_message_element = jQuery('#connection_message');
            options.complete({"responseText": '{ "success": "Connection OK. Successfully accessed repository metadata at file:///tmp/repo/repodata/repomd.xml" }'});
        };
        jQuery("#check_connection_button").click();
        assertEquals("admin/package_repositories/check_connection", actual_options.url);
        assertEquals("POST", actual_options.type);
        assertEquals("text", actual_options.dataType);
        assertEquals(jQuery('#parent_container :input').serialize(), actual_options.data);
        assertEquals("Should set ok_message class", true, jQuery("#connection_message").hasClass("ok_message"));
        assertEquals("Connection ok message not showing up", "Connection OK. Successfully accessed repository metadata at file:///tmp/repo/repodata/repomd.xml", jQuery("#connection_message").text())
    });

    it("testShouldCheckConnectionFailureMessage", function () {
        new PackageMaterialCheckConnection("admin/package_repositories/check_connection_button").bind("#parent_container", "#check_connection_button", "#connection_message");
        jQuery.ajax = function (options) {
            options.complete({"responseText": '{"error":"Error in connection!!"}' });
        };
        jQuery("#check_connection_button").click();
        assertEquals("Should set error_message class", true, jQuery("#connection_message").hasClass("error_message"));
        assertEquals("Error message not showing up", "Error in connection!!", jQuery("#connection_message").text())
    });

    it("testShouldAssertThatOnlyTheRelevantFormIsSerialize", function () {
        var formData = "unknown";
        jQuery.ajax = function (options) {
            formData = options.data;
        };
        var checkConnectionButton = jQuery("#check_connection_button");
        var checkConnection = new PackageMaterialCheckConnection("admin/package_repositories/check_connection");
        checkConnection.bind("#parent_container", "#" + checkConnectionButton.attr('id'), "#connection_message");

        checkConnectionButton.click();

        assertNotEquals("unknown", formData);
        var parseData = parseQueryParams(formData);
        assertEquals("USER", parseData['package_repository[configuration][1][configurationKey][name]']);
        assertEquals("user_name", parseData['package_repository[configuration][1][configurationValue][value]']);
        assertEquals("REPO_URL", parseData['package_repository[configuration][0][configurationKey][name]']);
        assertEquals("file:///tmp/repo/", unescape(parseData['package_repository[configuration][0][configurationValue][value]']));
        assertEquals("undefined", typeof(parseData['invalid_input_element']));
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
