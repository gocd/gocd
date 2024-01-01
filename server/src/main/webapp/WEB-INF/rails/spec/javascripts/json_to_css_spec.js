/*
 * Copyright 2024 Thoughtworks, Inc.
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

  function building_info_for(projectname, current_status, result) {
    return {
      building_info: {
        name: projectname,
        build_completed_date: "1 day ago",
        current_status: current_status,
        result: result
      }
    };
  }


  it("should add current status as a class to build status and job details header", function () {
    new JsonToCss().update_build_detail_header(building_info_for('job1', 'Failed', 'Failed'));
    expect($("#build_status").hasClass('failed')).toBe(true);
    expect($("#job_details_header").hasClass('failed')).toBe(true);
  });

  it("should replace old status with current status as a class to build status and job details header", function () {
    $("#build_status").attr('class', "failed");
    $("#job_details_header").attr('class', "failed");

    new JsonToCss().update_build_detail_header(building_info_for('job1', 'passed', 'Passed'));

    expect($("#build_status").hasClass('passed')).toBe(true);
    expect($("#build_status").hasClass('failed')).toBe(false);
    expect($("#job_details_header").hasClass('passed')).toBe(true);
    expect($("#job_details_header").hasClass('failed')).toBe(false);
  });
});
