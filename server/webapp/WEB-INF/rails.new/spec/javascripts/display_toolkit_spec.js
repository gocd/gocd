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

describe("display_toolkit", function () {
    beforeEach(function () {
        setFixtures("<div id=\"toolkit_project1\" style=\"display:none;\">\n" +
            "\t\t<div class=\"toolkit_panel\">\n" +
            "\t   \t\t<div class=\"toolkit_entry\">\n" +
            "\t\t\t\t    <a href=\"rss.xml?projectName=project1\"><img src=\"images/rssicon_small.png\" alt=\"Rss Feed\" title=\"Rss Feed\"/>RSS</a>\n" +
            "\t\t\t\t    <a href=\"cctray.xml\"><img src=\"images/cctrayicon_small.gif\" alt=\"CCTray\" title=\"CC tray\"/>for CCTray</a>\n" +
            "\t\t\t</div>\n" +
            "\t\t</div>\n" +
            "\t</div>\n" +
            "\t<div id=\"toolkit_project2\" style=\"display:none;\">\n" +
            "\t\t<div class=\"toolkit_panel\">\n" +
            "\t   \t\t<div class=\"toolkit_entry\">\n" +
            "\t\t\t\t    <a href=\"rss.xml?projectName=project2\"><img src=\"images/rssicon_small.png\" alt=\"Rss Feed\" title=\"Rss Feed\"/>RSS</a>\n" +
            "\t\t\t\t    <a href=\"cctray.xml\"><img src=\"images/cctrayicon_small.gif\" alt=\"CCTray\" title=\"CC tray\"/>for CCTray</a>\n" +
            "\t\t\t</div>\n" +
            "\t\t</div>\n" +
            "\t</div>");

        $('toolkit_project1').hide();
        $('toolkit_project2').hide();
    });

    afterEach(function () {
    });

    it("test_hide_all_tooltip_and_invoke_show_on_itself", function () {
        new Toolkit().show('toolkit_project1')
        assertTrue($('toolkit_project1').visible())
        assertFalse($('toolkit_project2').visible())
    });

    it("test_hide_click_one_same_entry_twice_should_not_hide_it", function () {
        new Toolkit().show('toolkit_project1');
        new Toolkit().show('toolkit_project1');
        assertTrue($('toolkit_project1').visible());
        assertFalse($('toolkit_project2').visible());
    });

    function disable_bubble(event) {
        invoked_diable_bubble = true;
    }
});
