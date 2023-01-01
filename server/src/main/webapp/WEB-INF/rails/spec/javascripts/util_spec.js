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

  var originalAjax = jQuery.ajax;

  afterEach(function () {
      jQuery.ajax = originalAjax;
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

    it("test_escapeDotsFromId", function () {
        assertEquals("#2\\.1\\.1\\.2", Util.escapeDotsFromId("2.1.1.2"));
    });
});