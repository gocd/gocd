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

describe("ajax_micro_content_handler", function () {
    var popup = null;
    var popup_shower = null;
    var show_link = null;
    var actual_ajax_request = jQuery.ajax;
    var periodical_opts = null;

    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
                "    <a href=\"#\" id=\"show_popup\">show it to me!</a>\n" +
                "\n" +
                "    <div id=\"content_box\">\n" +
                "    </div>\n" +
                "</div>\n"
        );
        jQuery.ajax = function (options) {
            periodical_opts = options;
        };
        jQuery('#content_box').html("stale text");

        var content_box = jQuery('#content_box').get(0);
        popup = new MicroContentPopup(content_box, new AjaxPopupHandler('http://foo/baz', content_box));
        popup_shower = new MicroContentPopup.ClickShower(popup);
        show_link = jQuery('#show_popup').get(0);
        popup_shower.bindShowButton(show_link);
    });

    afterEach(function () {
        jQuery.ajax = actual_ajax_request;
        periodical_opts = null;
        jQuery('#content_box').html("stale text");
        popup_shower.close();
    });

    it("test_updates_content_on_ajax_success", function () {
        var content_box = jQuery('#content_box');
        assertEquals("stale text", content_box.html());
        fire_event(show_link, 'click');
        assertEquals("", content_box.html());
        periodical_opts.success("hi there!");
        assertEquals("hi there!", content_box.html());
    });
    it("test_updates_content_on_ajax_error", function () {
        var content_box = jQuery('#content_box');
        assertEquals("stale text", content_box.html());
        fire_event(show_link, 'click');
        assertEquals("", content_box.html());
        periodical_opts.error("hi there!");
        assertEquals("No data available", content_box.html());
    });
    it("test_updates_content_on_ajax_success_using_contextual_ajax_handler", function () {
        var content_box = jQuery('#content_box');
        popup.callback_handler = new ContextualAjaxPopupHandler(content_box.get(0), function (event) {
            return 'http://foo/baz';
        });
        assertEquals("stale text", content_box.html());
        fire_event(show_link, 'click');
        assertEquals("", content_box.html());
        periodical_opts.success("hi there!");
        assertEquals("hi there!", content_box.html());
    });
    it("test_updates_content_on_ajax_error_using_contextual_ajax_handler", function () {
        var content_box = jQuery('#content_box');
        popup.callback_handler = new ContextualAjaxPopupHandler(content_box.get(0), function (event) {
            return 'http://foo/baz';
        });
        assertEquals("stale text", content_box.html());
        fire_event(show_link, 'click');
        assertEquals("", content_box.html());
        periodical_opts.error("hi there!");
        assertEquals("No data available", content_box.html());
    });
});

