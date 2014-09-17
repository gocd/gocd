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

ChooseExistingPackageDefinitionStateForNewPipelineWizard = function (data, selectedRepo, selectedPackage, repoContainer, packageContainer, packageConfigContainer, addNewOrChooseExistingControl, url) {
    init();

    ChooseExistingPackageDefinitionStateForNewPipelineWizard.prototype.initialize = function () {
        _populatePackageConfigContainerWithShowForm();
    }

    function init() {
        packageContainer.removeAttr('hidden');
        jQuery("input[name*='material[create_or_associate_pkg_def]'][value='associate']").attr("checked", "checked");
        _updateOptionsPkg(data[repoContainer.val()]);

        packageContainer.change(function () {
            if (addNewOrChooseExistingControl.val() == 'associate') {
                _populatePackageConfigContainerWithShowForm();
            }
        });
    }

    ChooseExistingPackageDefinitionStateForNewPipelineWizard.prototype.handleRepoChange = function () {
        clearPackageConfiguration();
        PackageMaterialDefinitionForNewPipelineWizard.prototype.displayErrorMessageIfPluginIsMissing();
        _updateOptionsPkg(data[repoContainer.val()]);
    }

    function isBlank(value) {
        return value == null || value == "" || value == undefined
    }

    function _populatePackageConfigContainerWithShowForm() {
        var repoId = repoContainer.val();
        var pkgId = packageContainer.val();
        PackageMaterialDefinitionForNewPipelineWizard.prototype.displayErrorMessageIfPluginIsMissing();
        if (!data[repoContainer.val()].is_plugin_missing) {
            if (!isBlank(pkgId)) {
                jQuery.ajax({
                    url: url.replace("$repoId$", repoId).replace("$packageId$", pkgId),
                    success: function (htmlContent) {
                        packageConfigContainer.html(htmlContent);
                    },
                    error: function () {
                        clearPackageConfiguration();
                    }
                });
            } else {
                clearPackageConfiguration();
            }
        }
    }

    function clearPackageConfiguration() {
        packageConfigContainer.html("");
    }

    function _updateOptionsPkg(repoDetails) {
        packageContainer.empty();
        packageContainer.append(jQuery("<option></option>").attr("value", "").text("[Select]"));
        if (repoDetails == undefined) {
            packageContainer.val("");
            packageContainer.attr('disabled', 'disabled');
            return;
        }
        packageContainer.removeAttr('disabled');

        jQuery.each(repoDetails.packages, function (index, value) {
            packageContainer.append(jQuery("<option></option>").attr("value", value.id).text(value.name));
        });
        if (!isBlank(selectedPackage)) {
            packageContainer.val(selectedPackage);
            packageContainer.trigger("change");
        }
    }
};
