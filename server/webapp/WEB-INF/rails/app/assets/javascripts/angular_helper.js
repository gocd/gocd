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

var AngularHelper = function () {
    var bootstrapAngular = function (form_id, app_name) {
        angular.bootstrap(document.getElementById(form_id), [app_name]);
    };

    var isDefined = function (app_name) {
        try {
            angular.module(app_name)
            return true;
        } catch (err) {
            return false;
        }
    };

    var defineModule = function (app_name) {
        try {
            angular.module(app_name)
        } catch (err) {
            angular.module(app_name, [])
        }
    };

    return {
        bootstrapAngular: bootstrapAngular,
        defineModule: defineModule,
        isDefined: isDefined
    };
};
