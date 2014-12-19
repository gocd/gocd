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

var WizardPage = new (Class.create({

    checkConnection: function(pipelineName, check_url, username, password, url, scm, isEncrypted, projectPath, domain, view) {
        username = encodeURIComponent(username);
        password = encodeURIComponent(password);
        url = encodeURIComponent(url);
        projectPath = encodeURIComponent(projectPath);
        domain = encodeURIComponent(domain);
        view = encodeURIComponent(view);
        var messageId = 'vcsconnection-message_' + scm;
        $(messageId).removeClassName("error_message").update('Checking connection...').show();
        var params = 'username=' + username + '&password=' + password + '&url=' + url + '&scm=' + scm + '&isEncrypted=' + isEncrypted+ '&projectPath=' + projectPath+ '&domain=' + domain+ '&view=' + view;
        if (pipelineName) {
            params += "&pipelineName=" + pipelineName;
        }
        var ajaxRequest = new Ajax.Request(check_url, {
            method: 'GET',
            parameters: params,
            onSuccess: function(transport) {
                var jsonText = transport.responseText;
                var vcsConnection = jsonText.evalJSON();
                var isvalid = vcsConnection.isValid == "true" ? true : false;
                if (!isvalid) {
                    $(messageId).removeClassName("ok_message").addClassName("error_message").update(vcsConnection.error).show();
                } else {
                    $(messageId).removeClassName("error_message").addClassName("ok_message").update('Connection OK').show();
                }
            }
        });
    },
    checkVCSConnection:function(elem) {
        if (!WizardPage.Scm.validateURL()) return;
        WizardPage.checkConnection(null, context_path('vcs.json'), $('username').value, $('password').value, $('url').value, $('scm').value, false);
    },
    onSCMChange : function() {
        WizardPage.Scm._reset();
        WizardPage.Scm.current().switchedOn();
    },
    Scm: {
        _reset : function() {
            $('username-password-section').hide();
            $('p4-section').hide();
            $('git-section').hide();
            $('username').value = '';
            $('password').value = '';
            $('url-example').update('');
            $('label_url').update("*URL:");
            $('vcsconnection-message').update('');
        },
        current : function() {
            var scmName = $('scm').options[$('scm').selectedIndex].value.toLowerCase();
            return WizardPage.Scm[scmName];
        },
        validateURL : function() {
            var url = $('url').value;
            if (url.blank()) {
                $('vcsconnection-message').removeClassName("ok_message").addClassName("error_message").update('URL is required').show();
                return false;
            } else {
                $('vcsconnection-message').removeClassName("error_message").update("").hide();
                return true;
            }
        },
        svn : {
            switchedOn : function() {
                $('username-password-section').show();
            },
            validate : function() {
                return WizardPage.Scm.validateURL();
            }
        },
        p4: {
            switchedOn : function() {
                $('username-password-section').show();
                $('p4-section').show();
                $('url-example').update('Example: locahost:1666');
                $('label_url').update("*Port:");
            },
            validate : function() {
                return WizardPage.Scm.validateURL() & WizardPage.Scm.p4._validateView();
            },
            _validateView : function() {
                var view = $('view').value;
                if (view.blank()) {
                    $('view-message').removeClassName("ok_message").addClassName("error_message").update('View is required').show();
                    return false;
                } else {
                    $('view-message').removeClassName("error_message").update("").hide();
                    return true;
                }
            }

        },
        git: {
            switchedOn : function() {
                $('git-section').show();
            },
            validate : function() {
                return WizardPage.Scm.validateURL();
            }
        },
        hg: {
            switchedOn : function() {

            },
            validate : function() {
                return WizardPage.Scm.validateURL();
            }
        }

    }
}));



