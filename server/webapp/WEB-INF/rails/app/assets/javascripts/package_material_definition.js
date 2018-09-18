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

PackageMaterialDefinition = function (data, selectedRepo, selectedPackage, showPackageDefPath, newPackageDefPath, pluginMissingErrorMessage) {
    var repoContainer = jQuery("#repository");
    var packageContainer = jQuery("#package");
    var packageConfigContainer = jQuery(".package_configuration");
    var addNewOrChooseExistingControl = jQuery("input[name*='material[create_or_associate_pkg_def]']");
    var associatePkgDef = "associate";
    var createPkgDef = "create";
    var state;
    var saveButton = jQuery(".primary.finish.submit");

    PackageMaterialDefinition.prototype.init = function (initialState) {
        _initialize(initialState);
    };

    function _initialize(initialState) {
        _updateOptionsRepo(repoContainer, data);
        _switchStateTo(initialState);
        _registerChangeListener();
    }

    function _switchStateTo(newState) {
        packageContainer.off();
        _disableControlsIfRepoIsNotSelected();
        if (newState == associatePkgDef || newState == "" || newState == undefined) {
            state = new ChooseExistingPackageDefinitionState(data, selectedRepo, selectedPackage, repoContainer, packageContainer, packageConfigContainer, addNewOrChooseExistingControl, saveButton,
                    showPackageDefPath);
        }
        else {
            state = new AddNewPackageDefinitionState(data, selectedRepo, selectedPackage, repoContainer, packageContainer, packageConfigContainer, addNewOrChooseExistingControl, saveButton,
                    newPackageDefPath);
        }
    }

    PackageMaterialDefinition.prototype.displayErrorMessageIfPluginIsMissing = function () {
        var repoDetails = data[repoContainer.val()]
        if (repoDetails != undefined) {
            if (repoDetails.is_plugin_missing == true) {
                var errorDiv = "<div class='form_submit_errors'><div class='errors'>" + pluginMissingErrorMessage.replace("'{0}'", repoDetails.plugin_id) + "</div></div>"
                packageConfigContainer.html(errorDiv)
                saveButton.attr("disabled", "disabled");
            } else {
                saveButton.removeAttr("disabled");
            }
        }
    }

    function _disableControlsIfRepoIsNotSelected() {
        if (repoContainer.val() == "") {
            addNewOrChooseExistingControl.attr('disabled', 'disabled');
            saveButton.attr("disabled", "disabled");
        }
        else {
            addNewOrChooseExistingControl.removeAttr('disabled');
            saveButton.removeAttr("disabled");
        }
    }

    function _registerChangeListener() {
        repoContainer.change(function () {
            _disableControlsIfRepoIsNotSelected()
            state.handleRepoChange();
        });

        addNewOrChooseExistingControl.change(function () {
            if (jQuery(this).is(":checked") && jQuery(this).val() == associatePkgDef) {
                _switchStateTo(associatePkgDef);
                state.initialize();
            }
            else if (jQuery(this).is(":checked") && jQuery(this).val() == createPkgDef) {
                _switchStateTo(createPkgDef);
                state.initialize();
            }
        });
    }

    function _updateOptionsRepo(container, data) {
        container.empty();
        container.append(jQuery("<option></option>").attr("value", "").text("[Select]"));
        if (data == undefined) {
            return;
        }
        jQuery.each(data, function (repoId, repoDetails) {
            container.append(jQuery("<option></option>").attr("value", repoId).text(repoDetails.name));
        });
        repoContainer.val(selectedRepo);
    }
};