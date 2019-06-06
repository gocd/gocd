/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var WizardPage = new (Class.create({

  checkConnection: function (pipelineName, username, password, url, scm, isEncrypted, projectPath, domain, view, branch, port) {

    // copied verbatim from string-plus.js
    var snakeCaser = function (key, value) {
      if (value && typeof value === 'object' && !_.isArray(value)) {
        var replacement = {};
        for (var k in value) {
          if (Object.hasOwnProperty.call(value, k)) {
            replacement[_.snakeCase(k)] = value[k];
          }
        }
        return replacement;
      }
      return value;
    };

    var messageBox = jQuery('#vcsconnection-message_' + scm);

    var urlParams = new URLSearchParams(window.location.search);
    var pipelineGroup = urlParams.get('group');

    var requestBody = {
      type: scm,
      pipeline_group: pipelineGroup,
      pipeline_name: pipelineName,
      attributes: {
        username: username,
        url: url,
        port: port,
        branch : branch,
        domain: domain,
        project_path: projectPath,
        view: view
      }
    };
    if (isEncrypted) {
      requestBody.attributes["encrypted_password"] = password;
    } else {
      requestBody.attributes["password"] = password;
    }

    jQuery.ajax({
      url:     Routes.apiv1AdminInternalMaterialTestPath(),
      type:    'POST',
      cache:   false,
      headers: {
        'Accept':       'application/vnd.go.cd.v1+json',
        'Content-Type': "application/json"
      },
      data:    JSON.stringify(requestBody, snakeCaser),
      beforeSend: function(){
        messageBox.removeClass("error_message").removeClass('ok_message').text('Checking connection...').show();
      },
      success: function (data, status, xhr) {
        messageBox.addClass('ok_message').text(data.message);
      },
      error:   function (xhr, status, error) {
        var message = 'There was an unknown error while checking connection';
        if (xhr.status === 422) {
          message = JSON.parse(xhr.responseText).message;
        }
        messageBox.addClass('error_message').html(jQuery('<pre>').text(message));
      }
    });
  }
}));



