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
describe("ajax_refresher", function () {
  let actual_ajax_request;
  let after_called;
  let actual_periodical_executor;
  let newPageUrl;
  let ajax_opts;

  beforeEach(function () {
    actual_ajax_request        = $.ajax;
    after_called               = false;
    actual_periodical_executor = PeriodicExecutor;
    newPageUrl        = null;
    ajax_opts = null;

    setFixtures(`
      <div class='under_test'>
        <div id="elem_id"></div>
      </div>`
    );

    after_called = false;

    $.ajax = function (options) {
      ajax_opts = options;
    };

    $('#elem_id').html("im_old_content");
  });

  afterEach(function () {
    PeriodicExecutor = actual_periodical_executor;
  });

  it("test_updates_dom_elements", function () {
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: "new_content"}});
    expect($('#elem_id').html()).toBe("new_content");
  });

  it("test_not_bomb_when_the_html_is_empty", function () {
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: ""}});
    expect($('#elem_id').html()).toBe("");
  });

  it("test_should_process_data", function () {
    let opts;
    $.ajax = function (options) {
      opts = options;
    };
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {
      time: 0, dataFetcher: function () {
        return {"foo-name": "foo-value", "bar-name": "bar-value"};
      }
    });
    refresher.stopRefresh();
    refresher.restartRefresh();

    expect(opts.data["bar-name"]).toBe("bar-value");
    expect(opts.data["foo-name"]).toBe("foo-value");
  });

  it("test_errors_out_when_no_html_element_given", function () {
    new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0, updateOnce: true});
    try {
      ajax_opts.success({elem_id: {parent_id: "bar", index: 10}});
      throw "succeeded unexpectedly - should throw up when no html given";
    } catch (e) {
      expect(e).toBe("no 'html' given for dom id 'elem_id'");
    }
    expect($('#elem_id').html()).toBe("im_old_content");
  });

  it("test_errors_out_when_no_parent_id_or_index_element_given", function () {
    new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0, updateOnce: true});
    try {
      ajax_opts.success({elem_id: {html: "bar"}});
    } catch (e) {
      throw "should not throw up when no parent_id or index given" + e;
    }
    expect($('#elem_id').html()).toBe("bar");
  });

  it("test_calls_manipulate_replacement_before_replacement", function () {
    let before_elem_id, before_dom_inner_html, actual_dom_inner_html;
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {
      time: 0,
      manipulateReplacement: function (elem_id, dom) {
        before_elem_id = elem_id;
        before_dom_inner_html = dom.innerHTML;
        actual_dom_inner_html = $('#elem_id').html();
      }
    });
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: "<div>new_content</div>", parent_id: "daddy", index: 1, type: 'type'}});
    expect(before_elem_id).toBe('elem_id');
    expect(before_dom_inner_html).toContain('new_content');
    expect(actual_dom_inner_html).toBe('im_old_content');
  });

  it("test_uses_dom_manipulated_by_manipulate_replacement_as_replacement", function () {
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {
      time: 0,
      manipulateReplacement: function (elem_id, dom) {
        $(dom).find('#foo_bar_baz').click(function () {
          this.innerHTML = "on click honored";
        });
        return true;
      }
    });
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: "<div>new_content<span id='foo_bar_baz'>empty</span></div>", parent_id: "daddy", index: 1, type: 'type'}});
    const replaced_content_holder = $('#foo_bar_baz');
    replaced_content_holder.click();
    expect(replaced_content_holder.html()).toBe("on click honored");
  });

  it("test_calls_transient_after_refresh_only_once_after_refresh_is_done", function () {
    let after_elem_id, actual_dom_inner_html;
    let call_count = 0;
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    refresher.afterRefreshOf("elem_id", function (elem_id) {
      after_elem_id = elem_id;
      actual_dom_inner_html = $('#elem_id').html();
      call_count++;
    });
    ajax_opts.success({elem_id: {html: "<div>new_content</div>"}});
    expect(after_elem_id).toBe('elem_id');
    expect(actual_dom_inner_html).toContain('new_content');
    expect(call_count).toBe(1);
    ajax_opts.success({elem_id: {html: "<div>new_fancy_content</div>"}});
    expect(actual_dom_inner_html).toContain('new_content');
    expect($("#elem_id").html()).toContain('new_fancy_content');
    expect(call_count).toBe(1);
  });

  it("test_calls_permanent_after_refresh_once_after_every_refresh", function () {
    let after_elem_id, actual_dom_inner_html;
    let call_count = 0;
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    refresher.afterRefreshOf("elem_id", function (elem_id) {
      after_elem_id = elem_id;
      actual_dom_inner_html = $('#elem_id').html();
      call_count++;
    }, true);
    ajax_opts.success({elem_id: {html: "<div>new_content</div>"}});
    expect(after_elem_id).toBe('elem_id');
    expect(actual_dom_inner_html).toContain('new_content');
    expect(call_count).toBe(1);
    ajax_opts.success({elem_id: {html: "<div>new_fancy_content</div>"}});
    expect(actual_dom_inner_html).toContain('new_fancy_content');
    expect($("#elem_id").html()).toContain("new_fancy_content");
    expect(call_count).toBe(2);
  });

  it("test_refreshes_only_once_if_requested", function () {
    let periodical_executor_instantiated = false;
    PeriodicExecutor = function () {
      const execute = function () {
        periodical_executor_instantiated = true;
      };
    };
    $.ajax = function (options) {
      options.success({elem_id: {html: "new_content"}});
    };
    new AjaxRefresher("http://blah/refresh_stage_detail", {updateOnce: true});
    expect(periodical_executor_instantiated).toBe(false);
    expect($('#elem_id').html()).toBe("new_content");
  });

  it("test_should_stop_refreshing_on_stop", function () {
    let periodical_executor_executing = true;
    PeriodicExecutor = function (callback, options) {
      this.execute = function () {
        periodical_executor_executing = true;
        callback();
      };
      this.registerCallback = function () {
      };
      this.stop = function () {
        periodical_executor_executing = false;
      };
    };
    let onSuccess;
    $.ajax = function (options) {
      onSuccess = options.success;
    };
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    refresher.stopRefresh();
    onSuccess({"elem_id": {html: "new_text"}});
    expect(periodical_executor_executing).toBe(false);
    expect($('#elem_id').html()).toBe("im_old_content");
  });

  it("test_should_be_able_to_restart_refreshing", function () {
    let periodical_executor_executing = true;
    let callback_registered = true;
    PeriodicExecutor = function (callback, options) {
      this.execute = function () {
        periodical_executor_executing = true;
        callback();
      };
      this.registerCallback = function () {
        callback_registered = true;
      };
      this.stop = function () {
        periodical_executor_executing = false;
      };
    };
    let onSuccess;
    $.ajax = function (options) {
      onSuccess = options.success;
    };
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    expect(periodical_executor_executing).toBe(false);
    refresher.restartRefresh();
    expect(periodical_executor_executing).toBe(true);
    expect(callback_registered).toBe(true);
    onSuccess({"elem_id": {html: "new_text"}});
    expect($('#elem_id').html()).toBe("new_text");
  });
});

