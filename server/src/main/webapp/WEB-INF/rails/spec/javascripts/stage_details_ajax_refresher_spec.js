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
describe("stage_details_ajax_refresher", function () {
  beforeEach(function () {
    setFixtures(`
      <div class='under_test'>
        <div id="jobs_failed">im failed jobs list</div>
        <input id="stage-history-page" name="stage-history-page">
      </div>
      <div id="jobs_grid_parent">
        <div id="jobs_grid">
          <table>
            <tr><input type="checkbox" name="foo" id="foo_checkbox" value="hello" class="job_selector"/></tr>
            <tr><input type="checkbox" name="bar" id="bar_checkbox" value="world" class="job_selector"/></tr>
          </table>
        </div>
      </div>
    `);
  });

  var actual_ajax_request = jQuery.ajax;
  var after_called = false;
  var options;


  beforeEach(function () {
    after_called = false;
    jQuery.ajax = function (opts) {
      options = opts;
    };
    jobs_markup_before = jQuery('#jobs_grid_parent').html();
  });

  afterEach(function () {
    jQuery.ajax = actual_ajax_request;
    jQuery('#jobs_grid_parent').html(jobs_markup_before);
  });

  it("test_updates_dom_elements", function () {
    var refresher = new StageDetailAjaxRefresher("http://blah/refresh_stage_detail", {}, {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();
    options.success({jobs_failed: {html: "new_jobs_failed"}});
    assertEquals("new_jobs_failed", jQuery('#jobs_failed').html());
  });

  it("test_invokes_callback_for_a_specified_id", function () {
    var refresher = new StageDetailAjaxRefresher("http://blah/refresh_stage_detail", {jobs_failed: function () {
      after_called = true;
    }}, {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    options.success({jobs_failed: {html: "new_jobs_failed"}});
    assertEquals("new_jobs_failed", jQuery('#jobs_failed').html());
    assertEquals(after_called, true);
  });

  it("test_invokes_callback_AFTER_REPLACEMENT", function () {
    var refresher = new StageDetailAjaxRefresher("http://blah/refresh_stage_detail", {jobs_failed: function () {
      assertEquals("new_jobs_failed", jQuery('#jobs_failed').html());
    }}, {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    options.success({jobs_failed: {html: "new_jobs_failed"}});
  });

  it("test_refresh_should_honor_page_number", function () {
    jQuery("#stage-history-page").val("3");
    var refresher = new StageDetailAjaxRefresher("http://blah/refresh_stage_detail", {jobs_failed: function () {
      assertEquals("new_jobs_failed", jQuery('#jobs_failed').html());
    }}, {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    assertEquals("3", options.data["stage-history-page"]);
  });


  it("test_update_page_keeps_the_current_selections", function () {
    var table = jQuery('#jobs_grid').html();
    var refresher = new StageDetailAjaxRefresher("http://blah/refresh_stage_detail", {}, {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    var chkbox = jQuery("#foo_checkbox").get(0);
    chkbox.checked = true;
    jQuery("#foo_checkbox").change();
    options.success({jobs_grid: {html: table}});
    chkbox = jQuery("#foo_checkbox").get(0);
    assertTrue(chkbox.checked);
  });

  it("test_updator_does_not_fail_when_jobs_table_is_not_present", function () {
    jQuery('#jobs_grid_parent').html('quux');
    var refresher = new StageDetailAjaxRefresher("http://blah/refresh_stage_detail", {}, {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    options.success({jobs_failed: {html: "foo"}});
    assertEquals("foo", jQuery('#jobs_failed').html());
  });
});
