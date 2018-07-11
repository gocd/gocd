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

describe("json_to_css", function () {
    var json_to_css;

    beforeEach(function () {
        setFixtures(
          "<a id=\"project1_forcebuild\"></a>\n" +
          "<a id=\"project1_config_panel\"></a>\n" +
          "<div id=\"build_status\"></div>\n" +
          "<div id=\"job_details_header\"></div>\n"
        );
    });

    beforeEach(function () {
        json_to_css = new JsonToCss();
        $("project1_forcebuild").className = "";
        $("project1_config_panel").className = "";
    });

    it("test_should_return_force_build_disabled_when_current_status_is_discontinued", function () {
        json_to_css.update_force_build(discontinued_json("project1"));
        assertTrue($("project1_forcebuild").className, $("project1_forcebuild").hasClassName("force_build_disabled"));
    });

    it("test_should_return_force_build_disabled_when_current_status_is_building", function () {
        json_to_css.update_force_build(building_json("project1"));
        assertTrue($("project1_forcebuild").className, $("project1_forcebuild").hasClassName("force_build_disabled"));
    });

    it("test_should_return_force_build_disable_when_current_status_is_paused", function () {
        json_to_css.update_force_build(paused_json("project1"));
        assertTrue($("project1_forcebuild").className, $("project1_forcebuild").hasClassName("force_build_disabled"));
    });

    it("should add current status as a class to build status and job details header", function () {
        json_to_css.update_build_detail_header(construct_new_json('job1', 'Failed', 'Failed'));
        assertTrue($("build_status").hasClassName('failed'));
        assertTrue($("job_details_header").hasClassName('failed'));
    });

    it("should replace old status with current status as a class to build status and job details header", function () {
        $("build_status").className = "failed";
        $("job_details_header").className = "failed";

        json_to_css.update_build_detail_header(construct_new_json('job1', 'passed', 'Passed'));

        assertTrue($("build_status").hasClassName('passed'));
        assertFalse($("build_status").hasClassName('failed'));
        assertTrue($("job_details_header").hasClassName('passed'));
        assertFalse($("job_details_header").hasClassName('failed'));
    });
});
