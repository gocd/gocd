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

PackageRepositoryConfiguration = function (pluginsContainer, configurationContainer, url, globalErrorsHeading) {
    var pluginsContainer = jQuery(pluginsContainer)
    var configurationContainer = jQuery(configurationContainer)
    var url = url;
    var globalErrorsHeading = globalErrorsHeading;
    var self = this;

    PackageRepositoryConfiguration.prototype.init = function () {
        bind();
    }

    function getPluginConfigPath(selectedPlugin) {
        return url.replace("$pluginid$", selectedPlugin);
    }

    function bind() {

        pluginsContainer.bind('change keyup', function () {
            var selectedPlugin = jQuery(this).val();
            if (selectedPlugin != null && selectedPlugin != "" && selectedPlugin != undefined) {
                jQuery.ajax({
                    url: getPluginConfigPath(selectedPlugin),
                    beforeSend: function () {
                        configurationContainer.html('<span class="spinner">loading...</span>');
                    },
                    success: function (htmlContent) {
                        configurationContainer.html(htmlContent);
                    },
                    error: function () {
                        configurationContainer.html("");
                    }
                })
            }
            else {
                configurationContainer.html("");
            }
        });

        jQuery("#package_repositories_edit_form").submit(function (event) {
            clearErrors();
            clearMessages();
            AjaxForm.jquery_ajax_submit(jQuery("#package_repositories_edit_form")[0], AjaxForm.ConfigFormEditHandler, null, self.handleFailure);
            return false;
        });
    }

    function clearErrors() {
        jQuery(".field.error").each(function () {
            jQuery(this).removeClass("error");
            jQuery(this).find("span.error").remove();
        });
        jQuery("#ajax_form_submit_errors").html("");
    }

    function clearMessages() {
        jQuery(".messages").html("");
        jQuery("#messaging_wrapper #message_pane").html("");
    }

    PackageRepositoryConfiguration.prototype.handleFailure = function handleFailure(xhr) {
        var result = JSON.parse(xhr.responseText)
        var invisible_field_errors = []

        for (field in result.fieldErrors) {
            var inputField = jQuery("[name='" + field + "']");
            if (inputField.length == 0) {
                invisible_field_errors.push(result.fieldErrors[field])
            }
            var divFieldContainer = inputField.parent();
            jQuery(divFieldContainer).addClass("error");
            jQuery(divFieldContainer).append(jQuery("<span class='error'></span>").text(result.fieldErrors[field]));
        }
        displayErrorMessagesOnVisibleFields(invisible_field_errors)
    }

    function displayErrorMessagesOnVisibleFields(invisible_field_errors) {
        if (invisible_field_errors.length > 0) {
            var heading = "<h3>" + globalErrorsHeading + "</h3>";
            var errorMessagesTag = "<ul>";
            invisible_field_errors = invisible_field_errors.flatten().uniq();
            for (var i = 0; i < invisible_field_errors.length; i++) {
                errorMessagesTag = errorMessagesTag + "<li class=\"error\">" + invisible_field_errors[i] + "</li>"
            }
            errorMessagesTag = errorMessagesTag + "</ul>"
            jQuery("#ajax_form_submit_errors").html("<div class=\"errors\">"+heading + errorMessagesTag+"</div>");
        }
    }
};


Util.on_load(function () {
    jQuery('form input[type=text], form input[type=password], form select, form textarea').live('focusin',function () {
        jQuery(this).closest('.field').addClass('focus');
    }).live('focusout',function () {
                jQuery(this).closest('.field').removeClass('focus');
            }).first().each(function () {
                if (jQuery(this).val() == '') {
                    jQuery(this).focus();
                }
            })
});