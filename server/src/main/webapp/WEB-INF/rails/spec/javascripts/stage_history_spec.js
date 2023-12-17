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
describe("stage_history", function () {
  beforeEach(function () {
    AjaxRefreshers.clear();
    setFixtures(`
      <a href="#" id="stage_history_3">3</a>
      <input id="stage-history-page" name="stage-history-page">`
    );
  });

  var actual_jquery_ajax = $j.ajax;
  var options;
  var afterRef;

  beforeEach(function () {
    $j.ajax = function (opts) {
      options = opts;
    };
    afterRef = null;
  });

  afterEach(function () {
    AjaxRefreshers.clear();
    $j.ajax = actual_jquery_ajax;
  });

  it("test_page_change", function () {
    StageHistory._changePage('url', "4");
    assertEquals("url", options.url);
    assertEquals("4", jQuery("#stage-history-page").val());
  });

  function stub_main_refresher() {
    AjaxRefreshers.addRefresher({
      afterRefreshOf: function (id, fn) {
        assertEquals('stage_history', id);
        afterRef = fn;
      },
      stopRefresh: () => {}
    }, true);
  }

  it("test_bind_link_no_link", async () => {
    stub_main_refresher();
    $j("#stage-history-page").val("should-not-change");
    expect(jQuery('#doesnt_exist')[0]).toBeUndefined();
    assertNull(document.getElementById('doesnt_exist'));
    await StageHistory.bindHistoryLink('#doesnt_exist', "url-to-page-3", 3);
    $j(document).click();
    assertEquals("should-not-change", jQuery("#stage-history-page").val());
    afterRef();
    $j(document).click();
    assertEquals("should-not-change", jQuery("#stage-history-page").val());
  });

  it("test_bind_link", async () => {
    stub_main_refresher();
    $j("#stage-history-page").val("0");
    await StageHistory.bindHistoryLink('#stage_history_3', "url-to-page-3", 3);
    $j('#stage_history_3').click();
    assertEquals("3", jQuery("#stage-history-page").val());
    $j("#stage-history-page").val("0");
    afterRef();
    $j('#stage_history_3').click();
    assertEquals("3", jQuery("#stage-history-page").val());
  });
});
