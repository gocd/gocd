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

var LogObserver = Class.create();

LogObserver.prototype = {
    initialize : function() {},
    notify : function(jsonArray) {
        var errors = $A();
        var warnings = $A();
        $A(jsonArray).each(function(array_Item){
            if (array_Item.level == 'WARNING') {
                warnings.push(array_Item);
            } else {
                errors.push(array_Item);
            }
        });
        if (!errors || errors.length == 0) {
            FlashMessageLauncher.hide("global-error");
        } else {
            FlashMessageLauncher.global_errors(errors, true);
        }
        if (!warnings || warnings.length == 0) {
            FlashMessageLauncher.hide("global-warning");
        } else {
            FlashMessageLauncher.global_warnings(warnings, true);
        }
    }
};