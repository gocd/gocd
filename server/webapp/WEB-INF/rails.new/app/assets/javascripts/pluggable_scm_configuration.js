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
 * PluggableSCMConfigurationController expects to be nested directly inside
 * PluginTemplateViewController in order to be able to read the form data.
 */
var PluggableSCMConfiguration = function () {
    var angular_controller_name = "PluggableSCMConfigurationController";

    var initialize = function(appName) {
        angular.module(appName)
        .controller(angular_controller_name, ['$scope', function ($scope) {
            $scope.checkConnection = checkConnection;
            $scope.check_connection_state = "";
            $scope.check_connection_message = "";
        }])
        .directive('pluggedMaterialConfigurationKeys', ['$rootScope', function ($rootScope) {
            return {
                restrict: 'A',
                link: function ($scope, $element, $attributes) {
                    try{
                        $scope.keys = JSON.parse($element.text());
                        $scope.enclosing_form = $element.closest("form");
                    }
                    catch(e){
                        console.error(e.message);
                    }
                }
            };
        }]);
    };

    var checkConnection = function (url) {
        var scope = this;
        var material = {};
        angular.forEach(scope.keys, function(key) {
            this[key] = scope.$parent[key] || "";
        }, material);
        var data = {};
        data["authenticity_token"] = scope.enclosing_form.find('[name=authenticity_token]').val();
        data["utf8"] = scope.enclosing_form.find('[name=utf8]').val();
        data["material"] = material;
        scope.check_connection_message = 'Checking connection...';
        scope.check_connection_state = '';
        jQuery.ajax({
            type: "POST",
            url: url,
            data: data,
            dataType: 'json',
            success: function (data) {
                if (data.status == "failure") {
                    scope.check_connection_message = data.messages;
                    scope.check_connection_state = 'error_message';
                    scope.$apply();
                } else {
                    scope.check_connection_message = data.messages;
                    scope.check_connection_state = 'ok_message';
                    scope.$apply();
                }
            },
            error: function (xhr, options, thrownError) {
                scope.check_connection_message = xhr.status + ' - ' + thrownError;
                scope.check_connection_state = 'error_message';
                scope.$apply();
            }
        });
        return false;
    };

    return {
        initialize: initialize,
    };
};
