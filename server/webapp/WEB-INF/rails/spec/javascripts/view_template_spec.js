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

describe("view_template", function () {
    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <div class=\"templates\">\n" +
            "        <a href='#' class=\"view_template_link\"></a>\n" +
            "        <select id=\"select_template\" name=\"pipeline[templateName]\">\n" +
            "            <option value=\"t2\">t2</option>\n" +
            "            <option value=\"up_t\" selected=\"selected\">up_t</option>\n" +
            "        </select>\n" +
            "    </div>\n" +
            "\n" +
            "</div>");
    });

    var actual_url = null;
    var actual_options = null;
    var actual_error_wrapper = null;

    beforeEach(function () {
        Util.ajax_modal = function (url, options, error_wrapper) {
            actual_url = url;
            actual_options = options;
            actual_error_wrapper = error_wrapper;
        }
    });

    afterEach(function () {
        actual_url = null;
        actual_options = null;
        actual_error_wrapper = null;
    });

    it("test_should_open_modal_with_selected_template", function () {
        new ViewTemplate("go/config_view/templates/__template_name__").addListener('a.view_template_link');
        fire_event(jQuery('a.view_template_link').get(0), 'click');

        assertEquals("go/config_view/templates/up_t", actual_url);
        assertEquals(false, actual_options.overlayClose);
        assertEquals('up_t', actual_options.title);
        assertEquals('text', actual_error_wrapper("text"));
    });


    it("test_should_open_modal_with_given_template_selector", function () {
        new ViewTemplate("go/config_view/templates/__template_name__").addListener('a.view_template_link', function () {
            return "temp_name"
        });
        fire_event(jQuery('a.view_template_link').get(0), 'click');

        assertEquals("go/config_view/templates/temp_name", actual_url);
        assertEquals(false, actual_options.overlayClose);
        assertEquals('temp_name', actual_options.title);
        assertEquals('text', actual_error_wrapper("text"));
    });
});
