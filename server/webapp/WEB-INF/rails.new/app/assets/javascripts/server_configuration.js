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

ServerConfiguration = function(validation_url) {
    var validate = function(id, error_id, type) {
        var value = $(id).value;
        data = {};
        data['authenticity_token'] = jQuery('form').find('[name=authenticity_token]').val();
        data[type] = encodeURIComponent(value);

        jQuery.ajax({
            type: "POST",
            url: validation_url,
            data: data,
            dataType: 'json',
            beforeSend: function(xhr) {
                xhr.setRequestHeader('Confirm', 'true');
            },
            complete: function(response) {
                var jsonText = response.responseText;
                var value = jsonText.evalJSON();
                if(value.error) {
                    showFailure(error_id, value.error);
                } else {
                    $(error_id).removeClassName("error_message").removeClassName("ok_message").update("");
                }
            }
        });
    };

    var showMessage = function(elementId, message) {
        $(elementId).removeClassName("ok_message").removeClassName("error_message").update(message).show();
    };

    var showSuccess = function(elementId, message) {
        $(elementId).addClassName("ok_message").removeClassName("error_message").update(message).show();
    };

    var showFailure = function(elementId, message) {
        $(elementId).removeClassName("ok_message").addClassName("error_message").update(message).show();
    };

    var reloadCommandRepoCache = function(url, statusElementId, csrfToken) {
        showMessage(statusElementId, "Reloading ...");
        new Ajax.Request(url, {
            method: 'POST',
            requestHeaders: {
                'X-CSRF-Token': csrfToken,
                'Confirm': 'true'
            },
            onSuccess: function() {
                showSuccess(statusElementId, "Done!");
            },
            onFailure: function() {
                showFailure(statusElementId, "Failed!");
            }
        });
    };

    return {
        validateEmail : function(id, error_id) {
            validate(id, error_id, "email");
        },
        validateHostName: function(id, error_id) {
            validate(id, error_id, "hostName");
        },
        sendTestEmail: function(form, url, csrfToken) {
            showSuccess('admin_mail_error_message', 'Sending...');
            new Ajax.Request(url, {
                method: 'POST',
                parameters: 'server_configuration_form[hostName]=' + encodeURIComponent($('server_configuration_form_hostName').value) +
                    '&server_configuration_form[port]=' + encodeURIComponent($('server_configuration_form_port').value) +
                    '&server_configuration_form[username]=' + encodeURIComponent($('server_configuration_form_username').value) +
                    '&server_configuration_form[encrypted_password]=' + encodeURIComponent($('server_configuration_form_encrypted_password').value) +
                    '&server_configuration_form[password_changed]=' + encodeURIComponent($('server_configuration_form_password_changed').checked) +
                    '&server_configuration_form[password]=' + encodeURIComponent($('server_configuration_form_password').value) +
                    '&server_configuration_form[tls]=' + $('server_configuration_form_tls').checked +
                    '&server_configuration_form[from]=' + encodeURIComponent($('server_configuration_form_from').value) +
                    '&server_configuration_form[adminMail]=' + encodeURIComponent($('server_configuration_form_adminMail').value),
                requestHeaders: {
                    'X-CSRF-Token': csrfToken
                },
                onSuccess: function(transport) {
                    var jsonText = transport.responseText;
                    var value = jsonText.evalJSON();
                    if (value.error) {
                        showFailure('admin_mail_error_message', value.error);
                    } else {
                        showSuccess('admin_mail_error_message', value.success);
                    }
                }
            });
        },
        validateLdapSettings: function(form, url, messageId, csrfToken) {
            showMessage(messageId, 'Testing ldap connectivity...');
            new Ajax.Request(url, {
                method: 'POST',
                parameters: 'server_configuration_form[ldap_uri]=' + encodeURIComponent($('server_configuration_form_ldap_uri').value) +
                    '&server_configuration_form[ldap_username]=' + encodeURIComponent($('server_configuration_form_ldap_username').value) +
                    '&server_configuration_form[ldap_password]=' + encodeURIComponent($('server_configuration_form_ldap_password').value) +
                    '&server_configuration_form[ldap_password_changed]=' + encodeURIComponent($('server_configuration_form_ldap_password_changed').checked) +
                    '&server_configuration_form[ldap_encrypted_password]=' + encodeURIComponent($('server_configuration_form_ldap_encrypted_password').value) +
                    '&server_configuration_form[ldap_search_base]=' + encodeURIComponent($('server_configuration_form_ldap_search_base').value) +
                    '&server_configuration_form[ldap_search_filter]=' + encodeURIComponent($('server_configuration_form_ldap_search_filter').value),
                requestHeaders: {
                    'X-CSRF-Token': csrfToken
                },
                onSuccess: function(transport) {
                    var jsonText = transport.responseText;
                    var value = jsonText.evalJSON();
                    if (value.error) {
                        showFailure(messageId, value.error);
                    } else {
                        showSuccess(messageId, value.success);
                    }
                }
            });
        },
        validatePort: function(id, error_id) {
            validate(id, error_id, "port");
        },
        reloadCommandRepoCache: reloadCommandRepoCache
    }
};