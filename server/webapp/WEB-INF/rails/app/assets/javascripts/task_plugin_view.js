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

var TaskPluginView = function () {
    var angular_controller_element = null;
    var app_name = "task_app";
    var form = jQuery(".submit.finish").closest("form");

    /*
     * Initialize an angularJS application for the element identified by "angular_app_element_id",
     * by giving it an ng-app attribute and an ng-controller.
     *
     * The scope of the controller is populated with data configured for the task (taken from
     * the contents of the element with id "angular_data_element_id").
     *
     * PS: form name should not contain hyphen, brackets etc, otherwise angular would stop binding without giving any reasons. Same goes for field names, if field name has brackets, use the quote notation for binding ie. form_name['field_name_with_square_brackets']
     */
    var initialize = function (plugged_task_angular_controller_element_id, angular_data_element_id, form_name_prefix) {
        var angular_task_controller_name = plugged_task_angular_controller_element_id + "_controller";
        var task_data_element = jQuery(document.getElementById(angular_data_element_id)).text();
        angular_controller_element = document.getElementById(plugged_task_angular_controller_element_id);
        this.configureFormSerialization(form_name_prefix);
        updateNamesInTemplate();

        new AngularHelper().defineModule(app_name);
        angular.module(app_name).controller(angular_task_controller_name, ['$scope', function ($scope) {

            try{
                var data = JSON.parse(task_data_element);
            var formName = form.attr("name");
            var errors = "errors";
            $scope[errors] = {};
            angular.forEach(data, function (value, key) {
                $scope[key] = value.value;
                if (value.errors !== null) {
                    $scope[errors][key] = value.errors;
                    $scope[formName].$setValidity('server', false);
                }
            });
            }
            catch(e){
                console.log(e.message);
            }

        }]);

        angular.module(app_name).directive('servererror', function () {
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
        });

        angular_controller_element.setAttribute("ng-controller", angular_task_controller_name);
        return this;
    };

    /*
     * Templates use 'formname.inputname.<expression>' format for error messages. These expressions need to be specified as GO_INPUT_NAME[<ng-model-value>].$error.
     * These expressions need to be updated with actual formname and input-name.
     * */
    var updateNamesInTemplate = function () {
        var html = jQuery(angular_controller_element).html();
        var formName = form.attr("name");
        html = html.replace(new RegExp("GOFORMNAME", "g"), formName);
        jQuery(angular_controller_element).find("[ng-model]").each(function (index, element) {
            var inputName = element.getAttribute("ng-model");
            html = html.replace(new RegExp("GOINPUTNAME\\[" + inputName + "\\]", "g"), formName + "['" + element.getAttribute("name") + "']");
        });
        jQuery(angular_controller_element).html(html);
    };

    /*
     * Change the "name" attribute of the input elements in the angular app, so that a Rails
     * form submit will send back data with the right keys, so that setConfigAttributes can
     * work.
     */
    var configureFormSerialization = function (form_name_prefix) {
        jQuery(angular_controller_element).find("[ng-model]").each(function (index, element) {
            element.setAttribute("name", form_name_prefix + "[" + element.getAttribute("ng-model") + "]");
            element.setAttribute("servererror", undefined);
        });
    };

    var bootstrapAngular = function () {
        if (new AngularHelper().isDefined(app_name)){
            new AngularHelper().bootstrapAngular(form.attr("id"), app_name);
        }
    };

    return {
        initialize: initialize,
        configureFormSerialization: configureFormSerialization,
        bootstrapAngular: bootstrapAngular
    };
};