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

var CruiseTimeConverter = Class.create({
    fromSecondsToHHMMSS: function(seconds){
        var hour = Math.floor(seconds / 3600);
        seconds = seconds - hour * 3600;
        var minite = Math.floor(seconds / 60);
        seconds = seconds - minite * 60;
        
        return this._time_to_text(hour) + ':' +
               this._time_to_text(minite) + ':' +
               this._time_to_text(seconds);
    },
    _time_to_text: function(number) {
        return ((number > 9)?"":"0") + number;
    }
});
var cruiseTimeConverter = new CruiseTimeConverter();