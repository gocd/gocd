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
    assertTrue(jQuery("#build_status").hasClass('failed'));
    assertTrue(jQuery("#job_details_header").hasClass('failed'));
  });

  it("should replace old status with current status as a class to build status and job details header", function () {
    jQuery("#build_status").attr('class', "failed");
    jQuery("#job_details_header").attr('class', "failed");

    new JsonToCss().update_build_detail_header(construct_new_json('job1', 'passed', 'Passed'));

    assertTrue(jQuery("#build_status").hasClass('passed'));
    assertFalse(jQuery("#build_status").hasClass('failed'));
    assertTrue(jQuery("#job_details_header").hasClass('passed'));
    assertFalse(jQuery("#job_details_header").hasClass('failed'));
  });
});
