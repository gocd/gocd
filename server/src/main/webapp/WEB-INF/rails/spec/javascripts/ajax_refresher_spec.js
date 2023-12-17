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
describe("ajax_refresher", function () {
  let actual_ajax_request;
  let after_called;
  let actual_periodical_executor;
  let newPageUrl;
  let ajax_opts;

  beforeEach(function () {
    actual_ajax_request        = jQuery.ajax;
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

    jQuery.ajax = function (options) {
      ajax_opts = options;
    };

    jQuery('#elem_id').html("im_old_content");
  });

  afterEach(function () {
    PeriodicExecutor = actual_periodical_executor;
  });

  it("test_updates_dom_elements", function () {
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: "new_content"}});
    assertEquals("new_content", jQuery('#elem_id').html());
  });

  it("test_not_bomb_when_the_html_is_empty", function () {
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: ""}});
    assertEquals("", jQuery('#elem_id').html());
  });

  it("test_should_process_data", function () {
    let opts;
    jQuery.ajax = function (options) {
      opts = options;
    };
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {
      time: 0, dataFetcher: function () {
        return {"foo-name": "foo-value", "bar-name": "bar-value"};
      }
    });
    refresher.stopRefresh();
    refresher.restartRefresh();

    assertEquals("bar-value", opts.data["bar-name"]);
    assertEquals("foo-value", opts.data["foo-name"]);
  });

  it("test_errors_out_when_no_html_element_given", function () {
    new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0, updateOnce: true});
    try {
      ajax_opts.success({elem_id: {parent_id: "bar", index: 10}});
      fail("should throw up when no html given");
    } catch (e) {
      assertEquals("no 'html' given for dom id 'elem_id'", e);
    }
    assertEquals("im_old_content", jQuery('#elem_id').html());
  });

  it("test_errors_out_when_no_parent_id_or_index_element_given", function () {
    new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0, updateOnce: true});
    try {
      ajax_opts.success({elem_id: {html: "bar"}});
    } catch (e) {
      fail("should not throw up when no parent_id or index given" + e);
    }
    assertEquals("bar", jQuery('#elem_id').html());
  });

  it("test_calls_manipulate_replacement_before_replacement", function () {
    let before_elem_id, before_dom_inner_html, actual_dom_inner_html;
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {
      time: 0,
      manipulateReplacement: function (elem_id, dom) {
        before_elem_id = elem_id;
        before_dom_inner_html = dom.innerHTML;
        actual_dom_inner_html = jQuery('#elem_id').html();
      }
    });
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: "<div>new_content</div>", parent_id: "daddy", index: 1, type: 'type'}});
    assertEquals("must use the correct dom id", 'elem_id', before_elem_id);
    assertContains("must use the correct replacement dom", 'new_content', before_dom_inner_html);
    assertEquals("must not have replaced old dom when before called", 'im_old_content', actual_dom_inner_html);
  });

  it("test_uses_dom_manipulated_by_manipulate_replacement_as_replacement", function () {
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {
      time: 0,
      manipulateReplacement: function (elem_id, dom) {
        jQuery(dom).find('#foo_bar_baz').click(function () {
          this.innerHTML = "on click honored";
        });
        return true;
      }
    });
    refresher.stopRefresh();
    refresher.restartRefresh();

    ajax_opts.success({elem_id: {html: "<div>new_content<span id='foo_bar_baz'>empty</span></div>", parent_id: "daddy", index: 1, type: 'type'}});
    const replaced_content_holder = jQuery('#foo_bar_baz');
    replaced_content_holder.click();
    assertEquals('on click should have been honored', "on click honored", replaced_content_holder.html());
  });

  it("test_calls_transient_after_refresh_only_once_after_refresh_is_done", function () {
    let after_elem_id, actual_dom_inner_html;
    let call_count = 0;
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    refresher.afterRefreshOf("elem_id", function (elem_id) {
      after_elem_id = elem_id;
      actual_dom_inner_html = jQuery('#elem_id').html();
      call_count++;
    });
    ajax_opts.success({elem_id: {html: "<div>new_content</div>"}});
    assertEquals("must use the correct dom id", 'elem_id', after_elem_id);
    assertContains("must have replaced old dom when after called", 'new_content', actual_dom_inner_html);
    assertEquals("must not call transient after refresh callback except for the first refresh after registration", 1, call_count);
    ajax_opts.success({elem_id: {html: "<div>new_fancy_content</div>"}});
    assertContains("must have replaced old dom when after called", 'new_content', actual_dom_inner_html);
    assertContains("refresh must go through inspite of after refresh callback being expired", "new_fancy_content", jQuery("#elem_id").html());
    assertEquals("must not call transient after refresh callback except for the first refresh after registration", 1, call_count);
  });

  it("test_calls_permanent_after_refresh_once_after_every_refresh", function () {
    let after_elem_id, actual_dom_inner_html;
    let call_count = 0;
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    refresher.afterRefreshOf("elem_id", function (elem_id) {
      after_elem_id = elem_id;
      actual_dom_inner_html = jQuery('#elem_id').html();
      call_count++;
    }, true);
    ajax_opts.success({elem_id: {html: "<div>new_content</div>"}});
    assertEquals("must use the correct dom id", 'elem_id', after_elem_id);
    assertContains("must have replaced old dom when after called", 'new_content', actual_dom_inner_html);
    assertEquals("must not call transient after refresh callback except for the first refresh after registration", 1, call_count);
    ajax_opts.success({elem_id: {html: "<div>new_fancy_content</div>"}});
    assertContains("must have replaced old dom when after called", 'new_fancy_content', actual_dom_inner_html);
    assertContains("refresh must go through inspite of after refresh callback being expired", "new_fancy_content", jQuery("#elem_id").html());
    assertEquals("must not call transient after refresh callback except for the first refresh after registration", 2, call_count);
  });

  it("test_refreshes_only_once_if_requested", function () {
    let periodical_executor_instantiated = false;
    PeriodicExecutor = function () {
      const execute = function () {
        periodical_executor_instantiated = true;
      };
    };
    jQuery.ajax = function (options) {
      options.success({elem_id: {html: "new_content"}});
    };
    new AjaxRefresher("http://blah/refresh_stage_detail", {updateOnce: true});
    assertEquals("should not have instantiated periodical executor", false, periodical_executor_instantiated);
    assertEquals("new_content", jQuery('#elem_id').html());
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
    jQuery.ajax = function (options) {
      onSuccess = options.success;
    };
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    refresher.restartRefresh();

    refresher.stopRefresh();
    onSuccess({"elem_id": {html: "new_text"}});
    assertEquals("should have stopped the periodical executor", false, periodical_executor_executing);
    assertEquals("im_old_content", jQuery('#elem_id').html());
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
    jQuery.ajax = function (options) {
      onSuccess = options.success;
    };
    const refresher = new AjaxRefresher("http://blah/refresh_stage_detail", {time: 0});
    refresher.stopRefresh();
    assertEquals("should have stopped the periodical executor", false, periodical_executor_executing);
    refresher.restartRefresh();
    assertEquals("should have started the periodical executor", true, periodical_executor_executing);
    assertEquals("should have re-registered the periodical executor", true, callback_registered);
    onSuccess({"elem_id": {html: "new_text"}});
    assertEquals("new_text", jQuery('#elem_id').html());
  });
});

