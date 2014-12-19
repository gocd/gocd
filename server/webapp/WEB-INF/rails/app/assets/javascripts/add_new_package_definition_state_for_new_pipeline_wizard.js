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

AddNewPackageDefinitionStateForNewPipelineWizard = function (data, selectedRepo, selectedPackage, repoContainer, packageContainer, packageConfigContainer, addNewOrChooseExistingControl, url) {
    init();

    function init() {
        jQuery("#pipeline_edit_form").validate().resetForm();
        jQuery("input[name*='material[create_or_associate_pkg_def]'][value='create']").attr("checked", "checked");
        packageContainer.attr('disabled', 'disabled');
        packageContainer.attr('hidden', 'hidden');
    }

    AddNewPackageDefinitionStateForNewPipelineWizard.prototype.handleRepoChange = function () {
        _populatePackageConfigContainerWithNewForm();
    }

    AddNewPackageDefinitionStateForNewPipelineWizard.prototype.initialize = function () {
        _populatePackageConfigContainerWithNewForm();
    }

    function _populatePackageConfigContainerWithNewForm() {
        if (repoContainer.val() == "") {
            clearPackageConfiguration();
            return;
        }
        PackageMaterialDefinitionForNewPipelineWizard.prototype.displayErrorMessageIfPluginIsMissing();
        if (!data[repoContainer.val()].is_plugin_missing) {
            jQuery.ajax({
                url: url.replace("$repoId$", repoContainer.val()),
                success: function (htmlContent) {
                    packageConfigContainer.html(htmlContent);
                },
                error: function () {
                    clearPackageConfiguration();
                }
            });
        }
    }

    function clearPackageConfiguration() {
        packageConfigContainer.html("");
    }
};
