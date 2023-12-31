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
describe("stage_history", function () {
  beforeEach(function () {
    AjaxRefreshers.clear();
    setFixtures(`
      <a href="#" id="stage_history_3">3</a>
      <input id="stage-history-page" name="stage-history-page">`
    );
  });

  var actual_jquery_ajax = $.ajax;
  var options;
  var afterRef;

  beforeEach(function () {
    $.ajax = function (opts) {
      options = opts;
    };
    afterRef = null;
  });

  afterEach(function () {
    AjaxRefreshers.clear();
    $.ajax = actual_jquery_ajax;
  });

  it("test_page_change", function () {
    StageHistory._changePage('url', "4");
    expect(options.url).toBe("url");
    expect($("#stage-history-page").val()).toBe("4");
  });

  function stub_main_refresher() {
    AjaxRefreshers.addRefresher({
      afterRefreshOf: function (id, fn) {
        expect(id).toBe('stage_history');
        afterRef = fn;
      },
      stopRefresh: () => {}
    }, true);
  }

  it("test_bind_link_no_link", async () => {
    stub_main_refresher();
    $("#stage-history-page").val("should-not-change");
    expect($('#doesnt_exist')[0]).toBeUndefined();
    expect(document.getElementById('doesnt_exist')).toBeNull();
    await StageHistory.bindHistoryLink('#doesnt_exist', "url-to-page-3", 3);
    $(document).click();
    expect($("#stage-history-page").val()).toBe("should-not-change");
    afterRef();
    $(document).click();
    expect($("#stage-history-page").val()).toBe("should-not-change");
  });

  it("test_bind_link", async () => {
    stub_main_refresher();
    $("#stage-history-page").val("0");
    await StageHistory.bindHistoryLink('#stage_history_3', "url-to-page-3", 3);
    $('#stage_history_3').click();
    expect($("#stage-history-page").val()).toBe("3");
    $("#stage-history-page").val("0");
    afterRef();
    $('#stage_history_3').click();
    expect($("#stage-history-page").val()).toBe("3");
  });
});
