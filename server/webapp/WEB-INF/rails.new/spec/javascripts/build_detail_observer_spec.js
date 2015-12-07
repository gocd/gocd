/*
 * Copyright 2015 ThoughtWorks, Inc.
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

describe("build_detail_observer", function () {
    var orig_write_attribute = Element.writeAttribute;
    var contextPath = "/dashboard";

    var observer;
    beforeEach(function () {
        setFixtures("<div id=\"container\">\n" +
            "    <span class=\"page_panel\"><b class=\"rtop\"><b class=\"r1\"></b> <b class=\"r2\"></b> <b class=\"r3\"></b> <b class=\"r4\"></b></b></span>\n" +
            "\n" +
            "    <div class=\"build_detail_summary\">\n" +
            "        <h3>project1 is now <span id=\"build_status_id\" class='build_status'></span></h3>\n" +
            "        <ul class=\"summary\">\n" +
            "            <li><strong>Building since:</strong> $buildSince</li>\n" +
            "            <li><strong>Elapsed time:</strong> <span id=\"${projectName}_time_elapsed\"><img\n" +
            "                    src=\"images/spinner.gif\"/></span></li>\n" +
            "            <li><strong>Previous successful build:</strong> $durationToSuccessfulBuild</li>\n" +
            "            <li><strong>Remaining time:</strong> <span id=\"${projectName}_time_remaining\"><img\n" +
            "                    src=\"images/spinner.gif\"/></span></li>\n" +
            "            <span id=\"build_name_status\"></span>\n" +
            "        </ul>\n" +
            "    </div>\n" +
            "    <span class=\"page_panel\"><b class=\"rbottom\"><b class=\"r4\"></b> <b class=\"r3\"></b> <b class=\"r2\"></b> <b\n" +
            "            class=\"r1\"></b></b></span>\n" +
            "</div>\n" +
            "\n" +
            "<span class=\"buildoutput_pre\"></span>\n" +
            "\n" +
            "<div id=\"trans_content\"></div>");
        Element.addMethods({writeAttribute: orig_write_attribute});
        jQuery('.buildoutput_pre').html('');

        observer = new BuildOutputObserver(1, "project1");
        jQuery('#container').addClass("building_passed");

        jQuery('#trans_content').html('');
        TransMessage.prototype.initialize = Prototype.emptyFunction;
    });

    it("test_ajax_periodical_refresh_active_build_should_update_css", function () {
        jQuery('.build_detail_summary').parent().addClass("building_passed");
        var json = failed_json('project1')
        observer.update_page(json);
        assertTrue("failed", jQuery('.build_detail_summary').parent().hasClass("failed"));
    });

    it("test_ajax_periodical_refresh_active_build_output_executer_oncomplete_should_update_output", function () {
        var build_output = "Build Failed.";
        observer._update_live_output_color(build_output);
        assertEquals("Build Failed.", jQuery('.buildoutput_pre').text());
    });

    it("test_should_invoke_word_break_to_break_text", function () {
        $$WordBreaker.break_text = function () {
            return "breaked text";
        }
        observer.display_error_message_if_necessary(inactive_json("project1"))
        assertTrue(jQuery('#trans_content').text().indexOf("breaked text") > -1);
    });
});
