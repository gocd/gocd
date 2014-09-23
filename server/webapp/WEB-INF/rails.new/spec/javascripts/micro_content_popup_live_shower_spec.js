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

describe("micro_content_popup_live_shower", function () {
    beforeEach(function () {
        ajax_content = '<div id="elem_within_popup">some text</div>';
        jQuery.ajax = function (options) {
            periodical_opts = options;
            periodical_opts.success(ajax_content);
        };
        setFixtures("<div class='under_test'>\n" +
            "    <a href=\"#\" id=\"show_popup\" class=\"build_cause_link\">show it to me!</a>\n" +
            "    <a href=\"#\" id=\"another_popup\" class=\"build_cause_link\">show it to me!</a>\n" +
            "    <span id=\"random_element\">show it to me!</span>\n" +
            "\n" +
            "    <div id=\"content_box\" class=\"hidden\">\n" +
            "        <div id=\"elem_within_popup\">some text</div>\n" +
            "    </div>\n" +
            "</div>");

        content_box = jQuery('#content_box').get(0);
        popup = new MicroContentPopup(content_box, new ContextualAjaxPopupHandler(content_box, function (event) {
            return 'http://foo/baz';
        }));
        popup_shower = new MicroContentPopup.LiveShower(popup);
        show_link = jQuery('#show_popup');
        jQuery(jQuery('.build_cause_link')).live('click', function (event) {
            popup_shower.toggle_popup(event, this);
        });
    });
    var popup = null;
    var popup_shower = null;
    var show_link = null;
    var content_box = null;
    var ajax_content = null;

    var actual_ajax_request = jQuery.ajax;
    var periodical_opts = null;

    afterEach(function () {
        jQuery.ajax = actual_ajax_request;
        periodical_opts = null;
        popup_shower.close();
    });

    xit("test_should_close_popup_when_the_link_is_clicked_again", function () {
        fire_event(show_link.get(0), 'click');
        assertFalse("Should not be hidden once the box is open", jQuery(content_box).hasClass("hidden"));
        fire_event(show_link.get(0), 'click');
        assertTrue("Should be hidden once link is clicked", jQuery(content_box).hasClass("hidden"));
    });

    xit("test_should_close_popup_when_a_random_element_is_clicked", function () {
        fire_event(show_link.get(0), 'click');
        assertFalse("Should have opened the popup", jQuery(content_box).hasClass("hidden"));
        fire_event(jQuery("#random_element").get(0), 'click');
        assertTrue("Should have closed the popup", jQuery(content_box).hasClass("hidden"));
    });

    xit("test_should_keep_the_popup_open_when_a_point_within_the_popup_is_clicked", function () {
        fire_event(show_link.get(0), 'click');
        assertFalse("Should have opened the popup", jQuery(content_box).hasClass("hidden"));
        fire_event(jQuery("#elem_within_popup").get(0), 'click');
        assertFalse("Should keep the popup open", jQuery(content_box).hasClass("hidden"));
    });

    xit("test_should_open_a_different_popup_when_that_is_opened", function () {
        ajax_content = 'first_popup';
        fire_event(show_link.get(0), 'click');
        assertEquals('first_popup', jQuery('#content_box').html());
        assertFalse("Should have opened the popup", jQuery(content_box).hasClass("hidden"));

        ajax_content = 'second_popup';
        fire_event(jQuery("#another_popup").get(0), 'click');
        assertEquals('second_popup', jQuery('#content_box').html());
        assertFalse("Should keep the popup open", jQuery(content_box).hasClass("hidden"));
    });
});
