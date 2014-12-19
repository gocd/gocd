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

describe("ajax_refresher", function () {
    var actual_ajax_updater = Ajax.Updater;
    var actual_ajax_request = jQuery.ajax;
    var after_called = false;
    var actual_periodical_executor = PeriodicalExecuter;

    var newPageUrl = null;
    var util_load_page_fn = null;

    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
                "    <div id=\"elem_id\"></div>\n" +
                "</div>"
        );
        Ajax.Updater = function (container, url, options) {
            response_pane = container;
            resource_selection_url = url;
            resource_selection_request_option = options;
            ajax_request_for_tri_state_boxes_fired = true;
        };

        after_called = false;

        jQuery.ajax = function (options) {
            periodical_opts = options;
        };
        $('elem_id').update("im_old_content");
        util_load_page_fn = Util.loadPage;
        Util.loadPage = function (url) {
            newPageUrl = url;
        };
    });

    afterEach(function () {
        Util.loadPage = util_load_page_fn;
        Ajax.Updater = actual_ajax_updater;
        Ajax.PeriodicalUpdater = actual_ajax_request;
        PeriodicalExecuter = actual_periodical_executor;
    });

    it("test_updates_dom_elements", function () {
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: "new_content"}});
        assertEquals("new_content", $('elem_id').innerHTML);
    });

    it("test_not_bomb_when_the_html_is_empty", function () {
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: ""}});
        assertEquals("", $('elem_id').innerHTML);
    });

    it("test_should_process_data", function () {
        var opts;
        jQuery.ajax = function (options) {
            opts = options;
        };
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0, dataFetcher: function () {
            return { "foo-name": "foo-value", "bar-name": "bar-value"};
        }});
        refresher.stopRefresh();
        refresher.restartRefresh();

        assertEquals("bar-value", opts.data["bar-name"]);
        assertEquals("foo-value", opts.data["foo-name"]);
    });

    it("test_errors_out_when_no_html_element_given", function () {
        new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        try {
            periodical_opts.success({elem_id: {parent_id: "bar", index: 10}});
            fail("should throw up when no html given");
        } catch (e) {
            assertEquals("no 'html' given for dom id 'elem_id'", e);
        }
        assertEquals("im_old_content", $('elem_id').innerHTML);
    });

    it("test_errors_out_when_no_parent_id_or_index_element_given", function () {
        new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        try {
            periodical_opts.success({elem_id: {html: "bar"}});
        } catch (e) {
            fail("should not throw up when no parent_id or index given");
        }
        assertEquals("bar", $('elem_id').innerHTML);
    });

    it("test_calls_manipulate_replacement_before_replacement", function () {
        var before_elem_id, before_dom_inner_html, actual_dom_inner_html, before_parent_id, before_index, before_type;
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0, manipulateReplacement: function (elem_id, dom, value) {
            before_elem_id = elem_id;
            before_dom_inner_html = dom.innerHTML;
            actual_dom_inner_html = $('elem_id').innerHTML;
            before_parent_id = value['parent_id'];
            before_index = value['index'];
            before_type = value['type'];
        }});
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: "<div>new_content</div>", parent_id: "daddy", index: 1, type: 'type'}});
        assertEquals("must use the correct dom id", 'elem_id', before_elem_id);
        assertContains("must use the correct replacment dom", 'new_content', before_dom_inner_html);
        assertEquals("must not have replaced old dom when before called", 'im_old_content', actual_dom_inner_html);
        assertEquals("must get the parent id from json", 'daddy', before_parent_id);
        assertEquals("must get the index from json", 1, before_index);
        assertEquals("must get the type from json", 'type', before_type);
    });

    it("test_uses_dom_manipulated_by_manipulate_replacement_as_replacement", function () {
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0, manipulateReplacement: function (elem_id, dom, value) {
            jQuery(dom).find('#foo_bar_baz').click(function () {
                this.innerHTML = "on click honored";
            });
            return true;
        }});
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: "<div>new_content<span id='foo_bar_baz'>empty</span></div>", parent_id: "daddy", index: 1, type: 'type'}});
        var replaced_content_holder = jQuery('#foo_bar_baz');
        fire_event(replaced_content_holder.get(0), 'click');
        assertEquals('on click should have been honored', "on click honored", replaced_content_holder.html());
    });

    it("test_calls_before_refresh_before_manipulate_replacement", function () {
        var call_seq = [];
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0,
            beforeRefresh: function (elem_id, value) {
                call_seq.push({call: "before_refresh", replacement: value, elem_id: elem_id});
                return true;
            },
            manipulateReplacement: function (elem_id, dom, value) {
                call_seq.push({call: "manipulate_replacement", replacement: value, elem_id: elem_id, dom: dom});
            }
        });
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: "<div>new_content<span id='foo_bar_baz'>empty</span></div>", parent_id: "daddy", index: 1, type: 'type'}});

        assertEquals("before_refresh should be called first", call_seq[0].call, "before_refresh");
        assertEquals("before_refresh should get dom id", call_seq[0].elem_id, "elem_id");
        assertEquals("before_refresh should get replacement object", call_seq[0].replacement.parent_id, "daddy");
        assertUndefined("before_refresh should get replacement object", call_seq[0].dom);

        assertEquals("manipulate_replacement should be called after before_refresh", call_seq[1].call, "manipulate_replacement");
        assertEquals("manipulate_replacement should get dom id", call_seq[1].elem_id, "elem_id");
        assertEquals("manipulate_replacement should get replacement object", call_seq[1].replacement.parent_id, "daddy");
        assertNotNull("manipulate_replacement should get replacement object", call_seq[1].dom);
    });

    it("test_calls_manipulate_replacement_only_if_before_refresh_return_value_is_true", function () {
        var called_manipulate_replacement = false;
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0,
            beforeRefresh: function (elem_id, value) {
                return false;
            },
            manipulateReplacement: function (elem_id, dom, value) {
                called_manipulate_replacement = true;
            }
        });
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: "<div>new_content<span id='foo_bar_baz'>empty</span></div>", parent_id: "daddy", index: 1, type: 'type'}});
        assertFalse("manipulate_replacement should not be called if before returns false", called_manipulate_replacement);
    });

    it("test_should_not_replace_id_if_before_refresh_returns_false", function () {
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0, beforeRefresh: function (elem_id, parent_id) {
            return false;
        }});
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: "<div>new_content</div>"}});
        assertEquals("should not replace if the before refresh returns false", 'im_old_content', $('elem_id').innerHTML);
    });

    it("test_should_load_page_requested_when_GO_FORCE_LOAD_PAGE_is_set", function () {
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0, beforeRefresh: function (elem_id, parent_id) {
            return false;
        }});
        refresher.stopRefresh();
        refresher.restartRefresh();

        var header_name = null;

        var xhr = {
            getResponseHeader: function (name) {
                header_name = name;
                return "holy_cow_new_url_is_sooooo_cool!!!";
            }
        };

        periodical_opts.complete(xhr);
        assertEquals("holy_cow_new_url_is_sooooo_cool!!!", newPageUrl);
        assertEquals("X-GO-FORCE-LOAD-PAGE", header_name);
    });

    it("test_calls_after_refresh_after_replacement", function () {
        var after_elem_id, actual_dom_inner_html;
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0, afterRefresh: function (elem_id, dom) {
            after_elem_id = elem_id;
            actual_dom_inner_html = $('elem_id').innerHTML;
        }});
        refresher.stopRefresh();
        refresher.restartRefresh();

        periodical_opts.success({elem_id: {html: "<div>new_content</div>"}});
        assertEquals("must use the correct dom id", 'elem_id', after_elem_id);
        assertContains("must have replaced old dom when after called", 'new_content', actual_dom_inner_html);
    });

    it("test_calls_transient_after_refresh_only_once_after_refresh_is_done", function () {
        var after_elem_id, actual_dom_inner_html;
        var call_count = 0;
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        refresher.stopRefresh();
        refresher.restartRefresh();

        refresher.afterRefreshOf("elem_id", function (elem_id) {
            after_elem_id = elem_id;
            actual_dom_inner_html = $('elem_id').innerHTML;
            call_count++;
        });
        periodical_opts.success({elem_id: {html: "<div>new_content</div>"}});
        assertEquals("must use the correct dom id", 'elem_id', after_elem_id);
        assertContains("must have replaced old dom when after called", 'new_content', actual_dom_inner_html);
        assertEquals("must not call transient after refresh callback except for the first refresh after registration", 1, call_count);
        periodical_opts.success({elem_id: {html: "<div>new_fancy_content</div>"}});
        assertContains("must have replaced old dom when after called", 'new_content', actual_dom_inner_html);
        assertContains("refresh must go through inspite of after refresh callback being expired", "new_fancy_content", $("elem_id").innerHTML);
        assertEquals("must not call transient after refresh callback except for the first refresh after registration", 1, call_count);
    });

    it("test_calls_permanent_after_refresh_once_after_every_refresh", function () {
        var after_elem_id, actual_dom_inner_html;
        var call_count = 0;
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        refresher.stopRefresh();
        refresher.restartRefresh();

        refresher.afterRefreshOf("elem_id", function (elem_id) {
            after_elem_id = elem_id;
            actual_dom_inner_html = $('elem_id').innerHTML;
            call_count++;
        }, true);
        periodical_opts.success({elem_id: {html: "<div>new_content</div>"}});
        assertEquals("must use the correct dom id", 'elem_id', after_elem_id);
        assertContains("must have replaced old dom when after called", 'new_content', actual_dom_inner_html);
        assertEquals("must not call transient after refresh callback except for the first refresh after registration", 1, call_count);
        periodical_opts.success({elem_id: {html: "<div>new_fancy_content</div>"}});
        assertContains("must have replaced old dom when after called", 'new_fancy_content', actual_dom_inner_html);
        assertContains("refresh must go through inspite of after refresh callback being expired", "new_fancy_content", $("elem_id").innerHTML);
        assertEquals("must not call transient after refresh callback except for the first refresh after registration", 2, call_count);
    });

    it("test_refreshes_only_once_if_requested", function () {
        var periodical_executor_instantiated = false;
        PeriodicalExecuter = function () {
            var execute = function () {
                periodical_executor_instantiated = true;
            };
        };
        jQuery.ajax = function (options) {
            options.success({elem_id: {html: "new_content"}});
        };
        new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {updateOnce: true});
        assertEquals("should not have instantiated periodical executor", false, periodical_executor_instantiated);
        assertEquals("new_content", $('elem_id').innerHTML);
    });

    it("test_should_stop_refreshing_on_stop", function () {
        var periodical_executor_executing = true;
        PeriodicalExecuter = function (callback, options) {
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
        var onSuccess;
        jQuery.ajax = function (options) {
            onSuccess = options.success;
        };
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        refresher.stopRefresh();
        refresher.restartRefresh();

        refresher.stopRefresh();
        onSuccess({"elem_id": {html: "new_text"}});
        assertEquals("should have stopped the periodical executor", false, periodical_executor_executing);
        assertEquals("im_old_content", $('elem_id').innerHTML);
    });

    it("test_should_be_able_to_restart_refreshing", function () {
        var periodical_executor_executing = true;
        var callback_registered = true;
        PeriodicalExecuter = function (callback, options) {
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
        var onSuccess;
        jQuery.ajax = function (options) {
            onSuccess = options.success;
        };
        var refresher = new AjaxRefresher("http://blah/refresh_stage_detail", "foo", {time: 0});
        refresher.stopRefresh();
        assertEquals("should have stopped the periodical executor", false, periodical_executor_executing);
        refresher.restartRefresh();
        assertEquals("should have started the periodical executor", true, periodical_executor_executing);
        assertEquals("should have re-registered the periodical executor", true, callback_registered);
        onSuccess({"elem_id": {html: "new_text"}});
        assertEquals("new_text", $('elem_id').innerHTML);
    });
});

