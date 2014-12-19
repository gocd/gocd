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

describe("i18n", function(){
    i18n_messages = { "building" : "Building(zh_CN)", "failed" : "Failed(zh_CN)", "discontinued" : "Discontinued", "unknown" : "Unknown", "queued" : "Queued", "passed" : "Passed", "waiting" : "Waiting", "paused" : "Paused" }

    beforeEach(function(){
        setFixtures("<div id=\"project1_current_status\"></div>");
    });

    beforeEach(function() {
        $("project1_current_status").update("Waiting");
    });

    it("test_should_get_i18n_message", function(){
        assertEquals("Building(zh_CN)", getMessage("Building"))
    });

    it("test_should_update_status_without_key", function(){
        $("project1_current_status").innerHTML = "Building";
        updateI18nStatus("project1_current_status");
        assertEquals("Building(zh_CN)", $("project1_current_status").innerHTML);
    });

    it("test_should_update_status_with_key", function(){
        $("project1_current_status").innerHTML = "Building";
        updateI18nStatus("project1_current_status", "Failed");
        assertEquals("Failed(zh_CN)", $("project1_current_status").innerHTML);
    });

    it("test_should_not_update_status_when_no_message", function(){
        $("project1_current_status").innerHTML = "Building";
        updateI18nStatus("project1_current_status", "non_existed_key");
        assertEquals("Building", $("project1_current_status").innerHTML);
    });
});
