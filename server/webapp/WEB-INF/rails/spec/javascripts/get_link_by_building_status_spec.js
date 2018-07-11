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

var BuildBaseObserver = function() { };

BuildBaseObserver.prototype = {
  get_link : function (json) {
    if (!json)  return;
    if (!json.building_info) return;
    if (!json.building_info.current_status) return;
    return 'build/detail/' + json.building_info.name
  }
};

describe("get_link_by_building_status", function(){

    it("test_should_return_link_to_commit_message_when_status_is_not_building", function() {
        var json = passed_json('project1')
        var expected = 'build/detail/' + json.building_info.name
        assertEquals(expected, new BuildBaseObserver().get_link(json));
        json = failed_json('project1')
        assertEquals(expected, new BuildBaseObserver().get_link(json));
    });

    it("test_should_return_link_to_commit_message_when_status_is_building", function() {
        var json = building_json('project1')
        var expected = 'build/detail/' + json.building_info.name;
        assertEquals(expected, new BuildBaseObserver().get_link(json));
    });

    it("test_should_return_null_when_json_is_null", function() {
        assertUndefined(new BuildBaseObserver().get_link(null));
    });

});
