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
describe("BuildSummaryObserverSpec", function BuildSummaryObserverSpec() {
  var observer;
  beforeEach(function () {
    setFixtures(`
      <div id="container" class="build_detail">
        <span class="page_panel"><b class="rtop"><b class="r1"></b> <b class="r2"></b> <b class="r3"></b> <b class="r4"></b></b></span>
      
        <div id="build_status" class="build-status"></div>
        <div class="build_detail_summary">
            <ul class="summary">
                <li><strong>Building since:</strong> $buildSince</li>
                <li><strong>Elapsed time:</strong> <span id="$\{projectName}_time_elapsed"></span></li>
                <li><strong>Previous successful build:</strong> $durationToSuccessfulBuild</li>
                <li><strong>Remaining time:</strong> <span id="$\{projectName}_time_remaining"></span></li>
                <span id="build_status"></span>
            </ul>
        </div>
        <span class="page_panel"><b class="rbottom"><b class="r4"></b> <b class="r3"></b> <b class="r2"></b> <b class="r1"></b></b></span>
      </div>
      
      <span class="buildoutput_pre"></span>
      
      <div id="trans_content"></div>
      `);

    $('.buildoutput_pre').html('');

    observer = new BuildSummaryObserver($(".build_detail_summary"));
    $('#container').addClass("building_passed");

    $('#trans_content').html('');
    TransMessage.prototype.initialize = function() {};
  });

  it("test_ajax_periodical_refresh_active_build_should_update_css", function () {
    var status = $(".build-status").addClass("building_passed");
    var json = {
      building_info: {
        name: 'project1',
        build_completed_date: "1 day ago",
        current_status: "Waiting",
        result: "Failed"
      }
    };
    observer.updateBuildResult(json);
    expect(status.hasClass("failed")).toBe(true);
  });
});
