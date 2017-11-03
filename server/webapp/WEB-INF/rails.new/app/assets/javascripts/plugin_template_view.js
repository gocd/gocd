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

/*
 * Angular application for handling plugin templates in forms.
 */
var PluginTemplateView = function () {
    var appName = "PluginTemplateViewApp";
    var controllerName = "PluginTemplateViewController";

    var initialize = function () {
        new AngularHelper().defineModule(appName);
        angular.module(appName)
        .controller(controllerName, ['$scope', function ($scope) {
          // Empty, just for a scope.
        } ])
        .directive('servererror', function () {
            return {
                restrict: 'A',
                require: '?ngModel',
                link: function ($scope, $element, $attributes, ngModel) {
                  $scope.$watch('errors', function (errors) {
                    if (errors && errors[$attributes.ngModel]) {
                        ngModel.$setValidity('server', false);
                        ngModel.$error["server"] = errors[$attributes.ngModel];
                    }
                  });
                }
            };
        })
        // Use this directive on an element containing plugin template.
        .directive('pluginTemplate', function ($compile) {
            return {
                restrict: 'A',
                link: function ($scope, $element, $attributes) {
                    var namePrefix = $attributes['pluginTemplate'];
                    $element.find("[ng-model]").each(function (index, element) {
                        element.setAttribute("name", namePrefix + "[" + element.getAttribute("ng-model") + "]");
                        element.setAttribute("servererror", undefined);
                    });
                    var html = $element.html();
                    var formName = jQuery(".submit.finish").closest("form").attr("name");
                    html = html.replace(new RegExp("GOFORMNAME", "g"), formName);
                    $element.find("[ng-model]").each(function (index, element) {
                        var inputName = element.getAttribute("ng-model");
                        html = html.replace(new RegExp("GOINPUTNAME\\[" + inputName + "\\]", "g"), formName + "['" + element.getAttribute("name") + "']");
                    });
                    $element.html(html);
                    $compile($element.contents())($scope);
                }
            };
        })
        /*
         * Use this directive on an element containing plugin data.
         * Format {"[config key]": {"value": "[value]", "errors": "[error message]"}}
         * Example: {"type": {"value": "X"}, "url": {"value":"Y", "errors": "incorrect format"}}
         */
        .directive('pluginData', function ($rootScope) {
            return {
                restrict: 'A',
                link: function ($scope, $element, $attributes) {
                    try{
                        var formName = jQuery(".submit.finish").closest("form").attr("name");
                        var data = JSON.parse($element.text());
                        var errors = "errors";
                        $scope[errors] = {};
                        angular.forEach(data, function (value, key) {
                            $scope[key] = value.value;
                            if (value.errors != null) {
                                $scope[errors][key] = value.errors;
                                $rootScope[formName].$setValidity('server', false);
                            }
                        });
                    }
                    catch(e){
                        console.log(e.message);
                    }
                }
            };
        });

        return this;
    };

    /*
     * Bootstrap an angularJS application for the form identified by "form_id".
     * 'extensions' is an array of objects with additional Angular controllers,
     * which are going to be initialized before bootstrap.
     */
    var bootstrapAngular = function (form_id, extensions) {
        if (!(new AngularHelper().isDefined(appName))) {
            initialize()
        }
        if (extensions != null && extensions instanceof Array) {
            for (var i = 0; i < extensions.length; i++) {
                extensions[i].initialize(appName);
            }
        }
        new AngularHelper().bootstrapAngular(form_id, appName);
    };

    return {
        bootstrapAngular: bootstrapAngular,
    };
};