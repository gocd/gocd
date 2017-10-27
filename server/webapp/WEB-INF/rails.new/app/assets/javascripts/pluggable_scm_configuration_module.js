/*************************GO-LICENSE-START*********************************
 * Copyright 2017 ThoughtWorks, Inc.
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

var PluggableSCMConfigurationModule = function () {
    var controller_element = null;
    var data_element = null;
    var configuration_element = null;
    var app_name = "pluggable_scm_configuration_module";
    var angular_controller_name = "PluggableSCMConfigurationController";
    var enclosing_form = jQuery(".submit.finish").closest("form");
    var enclosing_form_id = enclosing_form.attr("id");
    var enclosing_form_name = enclosing_form.attr("name");

    var initialize = function(controller_element_id, data_element_id, configuration_element_id, form_name_prefix) {
        controller_element = document.getElementById(controller_element_id);
        controller_element.setAttribute("ng-controller", angular_controller_name);
        data_element = document.getElementById(data_element_id);
        configuration_element = document.getElementById(configuration_element_id);
        configureFormSerialization(form_name_prefix);
        updateNamesInTemplate();
        if (!new AngularHelper().isDefined(app_name)) {
            defineAngularModule();
        }
        populateFieldsAndErrors();
        return this;
    };

    /*
     * Templates use 'formname.inputname.<expression>' format for error messages. These expressions need to be specified as GOINPUTNAME[<ng-model-value>].$error.
     * These expressions need to be updated with actual formname and input-name.
     * */
    var updateNamesInTemplate = function () {
        var html = jQuery(controller_element).html();
        jQuery(controller_element).find("[ng-model]").each(function (index, element) {
            var inputName = element.getAttribute("ng-model");
            html = html.replace(new RegExp("GOINPUTNAME\\[" + inputName + "\\]", "g"), enclosing_form_name + "['" + element.getAttribute("name") + "']");
        });
        jQuery(controller_element).html(html);
    };

    /*
     * Change the "name" attribute of the input elements in the angular app, so that a Rails
     * form submit will send back data with the right keys, so that setConfigAttributes can
     * work.
     */
    var configureFormSerialization = function (form_name_prefix) {
        jQuery(controller_element).find("[ng-model]").each(function (index, element) {
            element.setAttribute("name", form_name_prefix + "[" + element.getAttribute("ng-model") + "]");
            element.setAttribute("servererror", undefined);
        });
    };

    var defineAngularModule = function() {
        new AngularHelper().defineModule(app_name);
        angular.module(app_name)
        .controller(angular_controller_name, ['$scope', function ($scope) {
            populateFieldsAndErrors($scope);
            $scope.checkConnection = function(url) {
                var material = {};
                var configuration_keys = JSON.parse(jQuery(configuration_element).text());
                angular.forEach(configuration_keys, function(key) {
                    this[key] = $scope[key] || "";
                }, material);
                var data = {};
                data["authenticity_token"] = enclosing_form.find('[name=authenticity_token]').val();
                data["utf8"] = enclosing_form.find('[name=utf8]').val();
                data["material"] = material;
                $scope.check_connection_message = 'Checking connection...';
                $scope.check_connection_state = '';
                jQuery.ajax({
                    type: "POST",
                    url: url,
                    data: data,
                    dataType: 'json',
                    success: function (data) {
                        if (data.status == "failure") {
                            $scope.check_connection_message = data.messages;
                            $scope.check_connection_state = 'error_message';
                            $scope.$apply();
                        } else {
                            $scope.check_connection_message = data.messages;
                            $scope.check_connection_state = 'ok_message';
                            $scope.$apply();
                        }
                    },
                    error: function (xhr, options, thrownError) {
                        $scope.check_connection_message = xhr.status + ' - ' + thrownError;
                        $scope.check_connection_state = 'error_message';
                        $scope.$apply();
                    }
                });
                return false;
            };

            // check connection message state
            $scope.check_connection_state = "";
            $scope.check_connection_message = "";
        }])
       .directive('servererror', function () {
            return {
                restrict: 'A',
                require: '?ngModel',
                link: function (scope, element, attributes, ngModel) {
                    if (scope["errors"] && scope["errors"][attributes.ngModel]) {
                        ngModel.$setValidity('server', false);
                        ngModel.$error["server"] = scope["errors"][attributes.ngModel];
                    }
                }
            };
        })
    }

    var populateFieldsAndErrors = function ($scope) {
        try{
            var data = JSON.parse(jQuery(data_element).text());
            var errors = "errors";
            $scope[errors] = {};
            angular.forEach(data, function (value, key) {
                $scope[key] = value.value;
                if (value.errors !== null) {
                    $scope[errors][key] = value.errors;
                    if ($scope[enclosing_form_name] != null) {
                       $scope[enclosing_form_name].$setValidity('server', false);
                    }
                }
            });
        }
        catch(e){
            console.log(e.message);
        }
    }

    var bootstrap = function (bootstrap_element_id) {
        new AngularHelper().bootstrapAngular(enclosing_form_id, app_name);
    };

    return {
        initialize: initialize,
        bootstrap: bootstrap
    };
};
