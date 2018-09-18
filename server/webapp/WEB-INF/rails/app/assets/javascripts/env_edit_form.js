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

AjaxForm = function() {

    function disablePrimaryButton() {
        Util.disable(jQuery('.primary')[0]);
    }

    function enablePrimaryButton() {
        Util.enable(jQuery('.primary')[0]);
    }

    function redirectTo(req) {
        location.href = req.getResponseHeader('Location');
    }

    function addErrorMessage(req, container) {
        jQuery(container).html(Util.flash_message(req.responseText));
    }

    function focusModalBox() {
        var focusables = Modalbox._findFocusableElements();
        var realFocusables = [];
        for(var i = 0; i < focusables.length; i++) {
            var focusable = focusables[i];
            if (focusable.up('.fieldWithErrors') != null) {
                realFocusables.push(focusable);
            }
        }
        Modalbox.focusableElements = (realFocusables.length === 0) ? focusables : realFocusables;
        Modalbox._setFocus();
    }

    function handlePopupFailure(req, container) {
        addErrorMessage(req, container);
        focusModalBox();
        enablePrimaryButton();
    }

    return {
        error_box_selector: null,
        jquery_ajax_submit: function(form, handler, about_to_submit_handler,form_error_binding_callback) {
            handler = handler || AjaxForm.PopupHandler;
            var self = this;
            disablePrimaryButton();
            about_to_submit_handler && about_to_submit_handler();
            jQuery.ajax({
                url: form.action,
                type: form.method,
                data: jQuery(form).serialize(),
                success: function(data, status, request){
                    handler.onSuccess(request)
                },
                error: function(request){
                    handler.onFailure(request, self.error_box_selector, form_error_binding_callback);
                }
            });
            return false;
        },
        PopupHandler: {
            onSuccess: function(req) {
                Modalbox.hide();
                redirectTo(req);
            },
            onFailure: function(req, container) {
                handlePopupFailure(req, container);
            }
        },
        NonRedirectingPopupHandler: {
            onSuccess: function() {
                Modalbox.hide();
            },
            onFailure: function(req, container) {
                handlePopupFailure(req, container);
            }
        },
        ConfigFormEditHandler: {
            onSuccess: redirectTo,
            onFailure: function(req, container, form_error_binding_callback) {
                form_error_binding_callback && form_error_binding_callback(req)
                addErrorMessage(req, container);
                var conflictErrorMsg = '<div id="config_save_actions" class="flash">' +
                        '<button id="reload_config" class="reload_config primary">RELOAD</button>' +
                        '<label class="inline">This will refresh the page and you will lose your changes on this page.</label>' +
                        '</div>';
                var htmlContent = '<p class="error">' + req.getResponseHeader('Go-Config-Error') + '</p>' + (req.status == 409 ? conflictErrorMsg : '');
                jQuery('#message_pane').html(htmlContent);

                if (req.status == 409) {
                    jQuery("#reload_config").click(function() {
                        window.location.reload();
                    });
                }
                enablePrimaryButton();
            }
        }
    }
}();