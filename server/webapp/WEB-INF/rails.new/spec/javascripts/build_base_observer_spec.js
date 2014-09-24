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

describe("build_base_observer", function(){
    var observer;

    beforeEach(function(){
        observer = new BuildBaseObserver();
    });

    afterEach(function(){
    });

    it("test_should_return_build_detail_href_for_building", function(){
        var json = building_json("project1");
        assertEquals("build/detail/project1", observer.get_link(json));
    });

    it("test_should_return_build_detail_href_for_passed", function(){
        var json = passed_json("project1");
        assertEquals("build/detail/project1", observer.get_link(json));
    });

    it("test_should_return_build_detail_href_for_failed", function(){
        var json = failed_json("project1");
        assertEquals("build/detail/project1", observer.get_link(json));
    });

    it("test_should_return_build_detail_href_for_paused", function(){
        var json = paused_json("project1");
        assertEquals("build/detail/project1", observer.get_link(json));
    });

    it("test_should_return_build_detail_href_for_discontinued", function(){
        var json = discontinued_json("project1");
        assertEquals("build/detail/project1", observer.get_link(json));
    });
});
