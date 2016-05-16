/*
 * Copyright 2015 ThoughtWorks, Inc.
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

var CheckConnection = function() {

    function valueOf(selector) {
        return jQuery(selector).val();
    }

    function _hookupCheckConnection(id, pipelineName, materialType, materialUrl, username, password, encryptedPassword, isChanged, projectPath, domain, view, branch) {
        jQuery(id).click(function() {
            var isEncrypted = false;
            var finalPass = '';
            if (password) {
                if (encryptedPassword === "false" || jQuery(isChanged).is(':checked')) {
                    finalPass = valueOf(password);
                } else {
                    finalPass = valueOf(encryptedPassword);
                    isEncrypted = true;
                }
            }
            WizardPage.checkConnection(pipelineName, valueOf(username), finalPass, valueOf(materialUrl), materialType, isEncrypted, valueOf(projectPath), valueOf(domain), valueOf(view), valueOf(branch));
        });
    }

    return {
        hookupCheckConnection: _hookupCheckConnection
    }
};
