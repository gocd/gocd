/*
 * Copyright 2023 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
describe("json_to_css", function () {
    beforeEach(function () {
        setFixtures(
          "<div id=\"build_status\"></div>\n" +
          "<div id=\"job_details_header\"></div>\n"
        );
    });

    it("should add current status as a class to build status and job details header", function () {
        new JsonToCss().update_build_detail_header(construct_new_json('job1', 'Failed', 'Failed'));
        assertTrue($("build_status").hasClassName('failed'));
        assertTrue($("job_details_header").hasClassName('failed'));
    });

    it("should replace old status with current status as a class to build status and job details header", function () {
        $("build_status").className = "failed";
        $("job_details_header").className = "failed";

        new JsonToCss().update_build_detail_header(construct_new_json('job1', 'passed', 'Passed'));

        assertTrue($("build_status").hasClassName('passed'));
        assertFalse($("build_status").hasClassName('failed'));
        assertTrue($("job_details_header").hasClassName('passed'));
        assertFalse($("job_details_header").hasClassName('failed'));
    });
});
