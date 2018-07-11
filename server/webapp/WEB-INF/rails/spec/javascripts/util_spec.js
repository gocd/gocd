/*************************GO-LICENSE-START*********************************
 * Copyright 2018 ThoughtWorks, Inc.
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

describe("util", function () {
    beforeEach(function () {
        setFixtures("<input type=\"hidden\" value=\"1287652847\" name=\"server_time\" id=\"server_timestamp\"/>\n" +
            "<div class='under_test'>\n" +
            "    <span id=\"clickable\">clickable</span>\n" +
            "    <span title=\"1287651018131\" id=\"time_field\"></span>\n" +
            "    <input type=\"hidden\" value=\"1287651018131\"> \n" +
            "\n" +
            "    <div id=\"populatable\" class=\"\"></div>\n" +
            "    <input type=\"hidden\" id=\"shilpa_needs_to_work_more\"/>\n" +
            "    <button type=\"button\" id=\"btn\" name=\"button\">Push the button</button>\n" +
            "\n" +
            "    <a href=\"#\" id=\"foo_link\">name_foo</a>\n" +
            "    <input id=\"baz_input\" value=\"name_baz\"/>\n" +
            "\n" +
            "    <textarea id=\"id_bar\">id bar text</textarea>\n" +
            "    <textarea id=\"id_quux\">id quux text</textarea>\n" +
            "    <div id=\"update_on_evt\">Original content</div>\n" +
            "</div>");
    });
    var populatable;

  var orignialAjax = jQuery.ajax;

  afterEach(function () {
      jQuery.ajax = orignialAjax;
    }
  );

    beforeEach(function () {
        populatable = $('populatable');
        populatable.update("");
    });

    afterEach(function () {
        populatable.update("");
    });

    it("test_executes_javascript_on_event", function () {
        Util.on_load(function () {
            populatable.update("foo bar");
        });
        window.load;
        assertEquals("foo bar", populatable.innerHTML);
    });


    it("test_executes_javascript_if_event_has_been_fired", function () {
        window.load;
        Util.on_load(function () {
            populatable.update("foo bar1");
        });
        assertEquals("foo bar1", populatable.innerHTML);
    });


    it("test_appends_child_with_given_text_to_the_given_id", function () {
        Util.refresh_child_text('populatable', "This text gets overridden", "success");
        Util.refresh_child_text('populatable', "second text", "success");
        assertEquals(1, populatable.getElementsBySelector("p").length);
        var p = populatable.down('p');
        assertEquals("second text", p.innerHTML);
        assertTrue("Should have class name", p.hasClassName("success"));
    });


    it("test_does_not_execute_handler_except_for_the_first_time_the_event_is_fired", function () {
        Util.on_load(function () {
            populatable.update("foo bar1");
        });
        window.load;
        Util.on_load(function () {
            populatable.update("foo bar2");
        });

        populatable.update("bar baz");
        window.load;
        assertEquals("bar baz", populatable.innerHTML);
    });


    it("test_set_value", function () {
        var call_back = Util.set_value('shilpa_needs_to_work_more', "foo");
        call_back();
        assertEquals("foo", $('shilpa_needs_to_work_more').value);
    });


    it("test_enable_disable", function () {
        Util.disable("btn");
        assertTrue($("btn").disabled);
        assertTrue($("btn").hasClassName("disabled"));

        Util.enable("btn");
        assertFalse($("btn").disabled);
        assertFalse($("btn").hasClassName("disabled"));
    });


    it("test_escapeDotsFromId", function () {
        assertEquals("#2\\.1\\.1\\.2", Util.escapeDotsFromId("2.1.1.2"));
    });


    it("test_ajax_modal_success", function () {

        var ajax_options = null;
        var ajax_request = {};

        jQuery.ajax = function (options) {
            ajax_options = options;
            return ajax_request;
        };

        ajax_request.done = function (func) {
            func();
        };
        ajax_request.fail = function (func) {
        };
        ajax_request.responseText = 'response_body';

        var modal_box_options = null;
        var modal_box_content = null;
        Modalbox.show = function (data) {
            modal_box_content = data;
        };

        Util.ajax_modal("some_url", {title: "some_title"});

        assertEquals("some_url", ajax_options.url);
        assertContains('response_body', modal_box_content);
    });


    it("test_ajax_modal_failure", function () {

        var ajax_options = null;
        var ajax_request = {};

        jQuery.ajax = function (options) {
            ajax_options = options;
            return ajax_request;
        };

        ajax_request.done = function (func) {
        };
        ajax_request.fail = function (func) {
            func();
        };
        ajax_request.responseText = 'response_body';

        var modal_box_options = null;
        var modal_box_content = null;
        Modalbox.show = function (data, options) {
            modal_box_content = data;
            modal_box_options = options;
        };

        Util.ajax_modal("some_url", {title: "some_title"});

        assertEquals("some_url", ajax_options.url);
        assertContains('response_body', jQuery(modal_box_content)[0].innerHTML);
    });


    it("test_updates_dom_elements_on_callback", function () {
        var mapping = {name_foo: "id_bar", name_baz: "id_quux"};
        jQuery('#foo_link').click(Util.domUpdatingCallback(mapping, jQuery('#update_on_evt'), function () {
            return this.innerHTML;
        }));
        jQuery('#baz_input').click(Util.domUpdatingCallback(mapping, jQuery('#update_on_evt'), function () {
            return this.value;
        }));
        assertEquals("Original content", jQuery('#update_on_evt').text());
        fire_event($("foo_link"), 'click');
        assertEquals("id bar text", jQuery('#update_on_evt').text());
        fire_event($("baz_input"), 'click');
        assertEquals("id quux text", jQuery('#update_on_evt').text());
    });
});

describe("disable input fields", function () {
  beforeEach(function () {
    setFixtures("<div id='search_users_table' class='users_table'>\n"
    + "<input type='hidden'\n"
    + "id='foo'\n"
    + "name='foo'\n"
    + "value='foo'\n"
    + "/>\n"
    + "<input type='hidden'\n"
    + "id='bar'\n"
    + "name='bar'\n"
    + "value='bar'\n"
    + "/>\n"
    + "</div>");
  });

  it("should disable all hidden input fields", function () {
    assertFalse(jQuery("#foo")[0].disabled);
    assertFalse(jQuery("#bar")[0].disabled);

    Util.disable_all_hidden_fields("#search_users_table");

    assertTrue(jQuery("#foo")[0].disabled);
    assertTrue(jQuery("#bar")[0].disabled);
  });
});
