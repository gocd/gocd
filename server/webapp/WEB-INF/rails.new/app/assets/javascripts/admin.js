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

var AdminPage = Class.create();

AdminPage.prototype = {
    initialize: function() {
        if(AdminPage.instance){
            return AdminPage.instance; 
        }
        //If this page is not admin page, then not initialize
        if (!$('tab-content-of-source-xml')) {
            return;
        }
        this.bindObserver();
        AdminPage.instance = this;
    },

    bindObserver: function() {
        Event.observe('source-xml-tab-button', 'click', this._onConfigTabClick.bindAsEventListener(this));
        Event.observe('edit_config', 'click', this.showConfigurationEditor.bindAsEventListener(this));
        Event.observe('cancel_edit', 'click', this.hideConfigurationEditor.bindAsEventListener(this));
        Event.observe('save_config', 'click', this.saveConfiguration.bindAsEventListener(this));
        Event.observe(window, 'load', this.checkErrorStatus.bindAsEventListener(this));
    },

    _onConfigTabClick: function() {
        if ($('collapse-sidebar-link')) {
            expandSidebar($('collapse-sidebar-link'));
        }
        var adminPage = this;

        if(this._is_editing()) return;

        new Ajax.Request(context_path("tab/admin/configuration.json"), {
            asynchronous:1,
            method: 'GET',
            onSuccess: adminPage._updateConfiguration
        });

    },
    _is_editing : function() {
        return $('content_area').getElementsBySelector('textarea').length > 0;
    },
    showConfigurationEditor: function() {
        this._hideConfigurationError();

        var adminPage = this;
        new Ajax.Request(context_path("tab/admin/configuration.json"), {
            asynchronous:1,
            method: 'GET',
            onSuccess: adminPage._generateConfigurationEditor
        });
    },

    hideConfigurationEditor: function() {
        this._hideConfigurationError();
        $('content_area').innerHTML = "<pre id='content_container' class='wrap_pre'>" + $('content').value.escapeHTML() + "</pre>";
        this._hideEditButtons();
    },

    saveConfiguration: function() {
        $('config_editor_form').submit();
    },

    _updateConfiguration: function(transport) {
        var json = transport.responseText.evalJSON();

        var  pre = "<pre id='content_container' class='wrap_pre'>" + json.config.content.escapeHTML() + "</pre>";

        $('content_area').innerHTML = pre;
    },

    _generateConfigurationEditor: function(transport) {
        var json = transport.responseText.evalJSON();
        var editor = "<textarea style='display:none' id='content' rows='40' cols='300'></textarea><textarea id='editing_content' name='configFileContent' rows='40' cols='100'></textarea><input type='hidden' name='configMd5' id='editingConfigMd5'/>";
        $('content_area').innerHTML = editor;
        $('editing_content').value = json.config.content;
        $('content').value = json.config.content;
        $('editingConfigMd5').value =json.config.md5;
        if (transport.status == '401') {
            $('error_message').innerHTML = "Not authorized to edit configuration file.";
            $('error_message').show();
        } else {
            $('edit_config').hide();
            $('save_config').show();
            $('cancel_edit').show();
        }
    },

    _hideEditButtons: function() {
        $('edit_config').show();
        $('save_config').hide();
        $('cancel_edit').hide();
    },

    _hideConfigurationError: function() {
        if ($('error-container')) {
            $('error-container').hide();
        }
    },

    _showEditButtons: function() {
        $('edit_config').hide();
        $('save_config').show();
        $('cancel_edit').show();
    },

    checkErrorStatus: function() {
        if ($('editing_content') && $('editing_content').visible) {
            this._showEditButtons();
        }
    },

    _isPositiveInt: function(value) {
        var intY = parseInt(value);
        if (isNaN(intY))
            return false;
        
        if(intY <= 0)
            return false;
        
        return intY == value && value.toString() == intY.toString();
    },

    validateHostName : function() {
        this.validate('hostName', 'hostname_error_message', 'hostname', null);
    },

    validateFrom : function() {
        this.validate('from', 'from_error_message', 'email', null);
    },
    
    validateAdminMail : function() {
        this.validate('adminMail', 'admin_mail_error_message', 'email', null);
    },

    validatePort : function() {
        this.validate('port', 'port_error_message', 'port', null);
    },

    updateMailHost: function() {
        $('test-email-form').action = context_path("admin/configuration/server/mailhost");
        $('test-email-form').request({
            onSuccess: function(transport) {
                var json = transport.responseText.evalJSON();
                FlashMessageLauncher.success(json.success_message, false);
                return true;
            },
            onFailure: function(transport) {
                var json = transport.responseText.evalJSON();
                if (json.errorMessage) {
                    FlashMessageLauncher.error("Email notification configured failed", json.errorMessage, false);
                }
                return false;
            }
        });
        return false;
    },
};
