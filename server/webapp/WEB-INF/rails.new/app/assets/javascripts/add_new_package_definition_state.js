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

AddNewPackageDefinitionState = function (data, selectedRepo, selectedPackage, repoContainer, packageContainer, packageConfigContainer, addNewOrChooseExistingControl, saveButton, url) {
    init();

    function init() {
        saveButton.text("SAVE PACKAGE AND MATERIAL");
        jQuery("input[name*='material[create_or_associate_pkg_def]'][value='create']").attr("checked", "checked");
        packageContainer.attr('disabled', 'disabled');
        packageContainer.attr('hidden', 'hidden');
    }

    AddNewPackageDefinitionState.prototype.handleRepoChange = function () {
        _populatePackageConfigContainerWithNewForm();
    }

    AddNewPackageDefinitionState.prototype.initialize = function () {
        _populatePackageConfigContainerWithNewForm();
    }

    function _populatePackageConfigContainerWithNewForm() {
        if (repoContainer.val() == "") {
            clearPackageConfiguration();
            return;
        }
        PackageMaterialDefinition.prototype.displayErrorMessageIfPluginIsMissing();
        if (!data[repoContainer.val()].is_plugin_missing) {
            jQuery.ajax({
                url: url.replace("$repoId$", repoContainer.val()),
                success: function (htmlContent) {
                    packageConfigContainer.html(htmlContent);
                    Modalbox.focusableElements = Modalbox._findFocusableElements();
                },
                error: function () {
                    clearPackageConfiguration();
                }
            })
        }
        ;
    }

    function clearPackageConfiguration() {
        packageConfigContainer.html("");
    }
};
