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

describe("package_repository_configuration", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "\n" +
            "    <select id=\"plugin\">\n" +
            "        <option value=\"\">[Select]</option>\n" +
            "        <option value=\"yum\">yum</option>\n" +
            "    </select>\n" +
            "\n" +
            "    <select id=\"plugin_to_test_edit\">\n" +
            "        <option value=\"\">[Select]</option>\n" +
            "        <option value=\"yum\" selected>yum</option>\n" +
            "    </select>\n" +
            "\n" +
            "    <div id=\"messaging_wrapper\">\n" +
            "        <div id=\"message_pane\">\n" +
            "            Some flash message\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <form id=\"package_repositories_edit_form\">\n" +
            "        <div id=\"ajax_form_submit_errors\">\n" +
            "            foobar\n" +
            "        </div>\n" +
            "\n" +
            "        <div class=\"selected-package-repository\">\n" +
            "            <div class=\"messages\">\n" +
            "                Some messages\n" +
            "            </div>\n" +
            "            <div class=\"form_item required field error\">\n" +
            "                <label>fieldName</label>\n" +
            "                <input type=\"text\" value=\"fieldValue\">\n" +
            "\n" +
            "                <span class=\"error\">error message here</span>\n" +
            "            </div>\n" +
            "            <div class=\"form_item required field\">\n" +
            "                <input name=\"field1\" type=\"text\" value=\"field1Value\">\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </form>\n" +
            "    <div id=\"pluginConfigurations\"></div>\n" +
            "</div>");
    });

    var orignialAjax = jQuery.ajax;
    var original_jquery_ajax_submit = AjaxForm.jquery_ajax_submit;
    var plugins = ["yum", "deb", "foo"];

    afterEach(function () {
        jQuery.ajax = orignialAjax;
        AjaxForm.jquery_ajax_submit = original_jquery_ajax_submit;
    });

    beforeEach(function () {
    });

    it("testShouldGetPluginConfigurationBasedOnSelectedPluginid", function () {
        new PackageRepositoryConfiguration("#plugin", "#pluginConfigurations", "admin/package_repositories/config/$pluginid$", "The following error(s) need to be resolved in order to perform this action:").init();
        var wasCalled = false;
        jQuery.ajax = function (options) {
            if (options.url == "admin/package_repositories/config/yum") {
                wasCalled = true
            }
        };

        jQuery("#plugin").val("yum");
        jQuery("#plugin").trigger("change");

        assertEquals(true, wasCalled);

    });
    it("testShouldClearPluginConfigurationIfNoPluginIsSelected", function () {
        new PackageRepositoryConfiguration("#plugin", "#pluginConfigurations", "", "The following error(s) need to be resolved in order to perform this action:", "The following error(s) need to be resolved in order to perform this action:").init();
        var wasCalled = false;
        jQuery.ajax = function (options) {
            wasCalled = true;
        };
        jQuery("#pluginConfigurations").html("some config");
        jQuery("#plugin").val("");
        jQuery("#plugin").trigger("change");

        assertEquals(false, wasCalled);
        assertEquals("", jQuery("#pluginConfigurations").text());
    });

    it("testShouldClearMessagesAndErrorsBeforeSubmittingForm", function () {
        var wasCalled = false;
        AjaxForm.jquery_ajax_submit = function (form, handler, about_to_submit_handler, form_error_binding_callback) {
            wasCalled = true;
            assertEquals(jQuery("#package_repositories_edit_form")[0], form);
            assertEquals(AjaxForm.ConfigFormEditHandler, handler);
            assertEquals(null, about_to_submit_handler);
            assertEquals(PackageRepositoryConfiguration.prototype.handleFailure, form_error_binding_callback);
        };
        new PackageRepositoryConfiguration("#plugin_to_test_edit", "#pluginConfigurations", "admin/package_repositories/config/$pluginid$", "The following error(s) need to be resolved in order to perform this action:").init();
        jQuery("#package_repositories_edit_form").submit();
        assertEquals(true, wasCalled);
        assertEquals("No fieldErrors should be found", 0, jQuery('.field.error').length);
        assertEquals("No form_error should be found", 0, jQuery('.error').length);
        assertEquals("No Messages should be found", "", jQuery('.selected-package-repository .messages').html());
        assertEquals("No Messages pane should be found", "", jQuery('#messaging_wrapper #message_pane').html());
        assertEquals("No form submit errors should be found", "", jQuery('#ajax_form_submit_errors').html());
    });

    it("testShouldSetErrorOnRelevantFieldsOnFailureAfterEscaping", function () {
        var wasCalled = false;
        AjaxForm.jquery_ajax_submit = function (form, handler, about_to_submit_handler, form_error_binding_callback) {
            wasCalled = true;
            var responseText = '{"fieldErrors":{"field1":["<error 1>"]},"globalErrors":["global1","global2"],"message":"Save failed","isSuccessful":false,"subjectIdentifier":"id"}';
            var xhr = function
                () {
                return {
                    responseText: responseText
                }
            }
            form_error_binding_callback(new xhr())
        };
        new PackageRepositoryConfiguration("#plugin_to_test_edit", "#pluginConfigurations", "admin/package_repositories/config/$pluginid$", "The following error(s) need to be resolved in order to perform this action:").init();
        jQuery("#package_repositories_edit_form").submit();
        assertEquals(true, wasCalled);
        assertEquals("fieldWithErrors class should be added", 1, jQuery(".field.error [name='field1']").length);
        assertEquals("field error should be added", "&lt;error 1&gt;", jQuery("span.error").html());

    });

    it("testShouldDisplayErrorMessagesOnInvisibleFieldsAsGlobalErrors", function () {
        var wasCalled = false;
        AjaxForm.jquery_ajax_submit = function (form, handler, about_to_submit_handler, form_error_binding_callback) {
            wasCalled = true;
            var responseText = '{"fieldErrors":{"field1":["error 1"], "invisible_field_1":["error 2"], "invisible_field_2":["error 3"]},"globalErrors":[],"message":"Save failed","isSuccessful":false,"subjectIdentifier":"id"}';
            var xhr = function () {
                return {
                    responseText: responseText
                }
            }
            form_error_binding_callback(new xhr())
        };
        new PackageRepositoryConfiguration("#plugin_to_test_edit", "#pluginConfigurations", "admin/package_repositories/config/$pluginid$", "The following error(s) need to be resolved in order to perform this action:").init();
        jQuery("#package_repositories_edit_form").submit();
        assertEquals(true, wasCalled);
        assertEquals("<li class=\"error\">error 2</li><li class=\"error\">error 3</li>", jQuery("#ajax_form_submit_errors .errors ul").html())
        assertEquals("The following error(s) need to be resolved in order to perform this action:", jQuery("#ajax_form_submit_errors .errors h3").html())
    });

    it("testShouldNotDisplayGlobalErrorMessagesIfNonePresent", function () {
        var wasCalled = false;
        AjaxForm.jquery_ajax_submit = function (form, handler, about_to_submit_handler, form_error_binding_callback) {
            wasCalled = true;
            var responseText = '{"fieldErrors":{"field1":["error 1"]},"globalErrors":[],"message":"Save failed","isSuccessful":false,"subjectIdentifier":"id"}';
            var xhr = function () {
                return {
                    responseText: responseText
                }
            }
            form_error_binding_callback(new xhr())
        };
        new PackageRepositoryConfiguration("#plugin_to_test_edit", "#pluginConfigurations", "admin/package_repositories/config/$pluginid$", "The following error(s) need to be resolved in order to perform this action:").init();
        jQuery("#package_repositories_edit_form").submit();
        assertEquals(true, wasCalled);
        assertEquals("", jQuery("#ajax_form_submit_errors").html())
    });
});
