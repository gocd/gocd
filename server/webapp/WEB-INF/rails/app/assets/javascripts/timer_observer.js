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

var TimerObserver = Class.create();

TimerObserver.prototype = {
    timers : [],
    initialize : function(name) {
        this.name = name;
    },
    notify : function(jsonArray) {
        for (var i = 0; i < jsonArray.length; i++) {
            if (!jsonArray[i]) return;
            if(this.name && this.name != jsonArray[i].building_info.name) {
                continue;
            }
            $('build-detail-summary').innerHTML = $('build-summary-template').value.process({build:jsonArray[i].building_info});
        }
    }
}
